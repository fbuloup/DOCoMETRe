#!/usr/bin/env bash
# Headless check of DOCoMETRe, without Eclipse nor Maven/Tycho :
#   1. compiles all sources of Process and Docometre against the real Eclipse bundles (see bundles.txt),
#      other projects of the repository being compiled from their sources when needed,
#   2. compiles Docometre.Tests and runs its JUnit 5 tests (unless --no-tests).
# Exit code is 0 only if compilation has no error and all tests pass.
# Usage : tools/headless-check/check.sh [--no-tests]
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LIBS_DIR="${DOCOMETRE_LIBS:-$HOME/.cache/docometre-headless-libs}"
BUILD_DIR="${DOCOMETRE_CHECK_BUILD:-$HOME/.cache/docometre-headless-check}"
RUN_TESTS=1
[ "${1:-}" = "--no-tests" ] && RUN_TESTS=0

# Eclipse bundles (downloaded once)
"$SCRIPT_DIR/fetch-libs.sh" > /dev/null || { echo "CHECK FAILED : Eclipse bundles not available (see fetch-libs.sh)"; exit 2; }

cd "$REPOSITORY_DIR" || exit 2
CLASSPATH_LIBS="$(ls "$LIBS_DIR"/*.jar | tr '\n' ':')$(find "$REPOSITORY_DIR" -name "*.jar" -not -path "*/.git/*" -not -path "*/target/*" -not -path "*/bin/*" | tr '\n' ':')"
SOURCEPATH="$(find "$REPOSITORY_DIR" -mindepth 2 -maxdepth 2 -type d -name src -not -path "*/.git/*" -not -path "*/Docometre.Tests/*" | tr '\n' ':')"
rm -rf "$BUILD_DIR" && mkdir -p "$BUILD_DIR/classes" "$BUILD_DIR/tests"

# Copies resources (non Java files) of a source folder to a classes folder
copy_resources() {
	(cd "$1" && find . -type f ! -name "*.java" -exec cp --parents {} "$2" \;)
}

echo "=== Compilation of Process and Docometre"
find Process/src Docometre/src -name "*.java" > "$BUILD_DIR/sources.txt"
javac --release 21 -nowarn -encoding UTF-8 -proc:none -implicit:class -d "$BUILD_DIR/classes" \
	-cp "$CLASSPATH_LIBS" -sourcepath "$SOURCEPATH" @"$BUILD_DIR/sources.txt" > "$BUILD_DIR/javac.log" 2>&1
COMPILATION_STATUS=$?
# Every error is reported : never filter them
grep -v "^Picked up JAVA_TOOL_OPTIONS" "$BUILD_DIR/javac.log" | grep -v "^Note: "
if [ $COMPILATION_STATUS -ne 0 ]; then
	echo "CHECK FAILED : compilation errors ($(grep -c 'error:' "$BUILD_DIR/javac.log") error(s), see above)"
	exit 1
fi
echo "Compilation OK ($(wc -l < "$BUILD_DIR/sources.txt") source files)"
copy_resources Process/src "$BUILD_DIR/classes"
copy_resources Docometre/src "$BUILD_DIR/classes"

if [ $RUN_TESTS -eq 0 ]; then
	echo "CHECK OK (tests not run)"
	exit 0
fi

echo "=== Compilation of Docometre.Tests"
javac --release 21 -nowarn -encoding UTF-8 -proc:none -d "$BUILD_DIR/tests" -cp "$BUILD_DIR/classes:$CLASSPATH_LIBS" \
	$(find Docometre.Tests/src -name "*.java") "$SCRIPT_DIR/RunAllTests.java" > "$BUILD_DIR/javac-tests.log" 2>&1
COMPILATION_STATUS=$?
grep -v "^Picked up JAVA_TOOL_OPTIONS" "$BUILD_DIR/javac-tests.log" | grep -v "^Note: "
if [ $COMPILATION_STATUS -ne 0 ]; then
	echo "CHECK FAILED : tests compilation errors"
	exit 1
fi
copy_resources Docometre.Tests/src "$BUILD_DIR/tests"

echo "=== Tests"
# UTF-8 file names (e.g. "Essai n°1") ; tests search Libraries from the working directory (repository root)
LC_ALL=C.UTF-8 java -Dsun.jnu.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp "$BUILD_DIR/tests:$BUILD_DIR/classes:$CLASSPATH_LIBS" RunAllTests 2>&1 \
	| grep -v "^Picked up JAVA_TOOL_OPTIONS"
TESTS_STATUS=${PIPESTATUS[0]}
if [ $TESTS_STATUS -ne 0 ]; then
	echo "CHECK FAILED : tests failed (see above)"
	exit 1
fi
echo "CHECK OK"
