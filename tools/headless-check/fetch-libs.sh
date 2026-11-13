#!/usr/bin/env bash
# Downloads the Eclipse bundles listed in bundles.txt (only those not already downloaded and valid).
# Libraries folder : $DOCOMETRE_LIBS, ~/.cache/docometre-headless-libs by default.
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBS_DIR="${DOCOMETRE_LIBS:-$HOME/.cache/docometre-headless-libs}"
RELEASE_URL="https://download.eclipse.org/releases/2025-03/202503121000"
NEBULA_URL="https://download.eclipse.org/nebula/updates/release/3.4.0"

mkdir -p "$LIBS_DIR"
failures=0
while read -r repository bundle; do
	case "$repository" in
		release) url="$RELEASE_URL" ;;
		nebula) url="$NEBULA_URL" ;;
		*) continue ;;
	esac
	jar="$LIBS_DIR/$bundle.jar"
	# Already downloaded and valid
	if [ -f "$jar" ] && unzip -tq "$jar" > /dev/null 2>&1; then continue; fi
	downloaded=0
	for attempt in 1 2 3; do
		if curl -sSf --max-time 300 -o "$jar.part" "$url/plugins/$bundle.jar" && unzip -tq "$jar.part" > /dev/null 2>&1; then
			mv "$jar.part" "$jar"
			downloaded=1
			break
		fi
		sleep $((attempt * 2))
	done
	if [ "$downloaded" -eq 0 ]; then
		rm -f "$jar.part"
		echo "ERROR : unable to download $bundle from $url" >&2
		failures=$((failures + 1))
	fi
done < "$SCRIPT_DIR/bundles.txt"

if [ "$failures" -gt 0 ]; then
	echo "$failures bundle(s) not downloaded in $LIBS_DIR" >&2
	exit 1
fi
echo "Eclipse bundles available in $LIBS_DIR"
