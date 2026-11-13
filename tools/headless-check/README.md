# Headless check

Compiles DOCoMETRe and runs its tests without Eclipse nor Maven/Tycho, e.g. in a cloud session
or a continuous integration job.

```bash
tools/headless-check/check.sh             # compilation + tests
tools/headless-check/check.sh --no-tests  # compilation only
```

1. `fetch-libs.sh` downloads once the Eclipse bundles listed in `bundles.txt` from download.eclipse.org
   (target platform of `Docometre.Releng/pom.xml` : release 2025-03, Nebula 3.4.0), in
   `~/.cache/docometre-headless-libs` (or `$DOCOMETRE_LIBS`).
2. All sources of `Process` and `Docometre` are compiled with `javac --release 21` against these bundles,
   the other projects of the repository being compiled from their sources when needed.
3. `Docometre.Tests` is compiled and its JUnit 5 tests are run with `RunAllTests.java`
   (tests using `g++` are skipped if it is not installed).

The exit code is 0 only if compilation has no error and all tests pass. Build outputs go to
`~/.cache/docometre-headless-check` (or `$DOCOMETRE_CHECK_BUILD`).

Requirements : JDK 21, curl, unzip ; g++ for the tests of the Arduino code.

When the target platform changes (pom.xml repositories, new Require-Bundle in a MANIFEST.MF),
update `bundles.txt` : the bundle name and version are the name of the jar in the `plugins` folder
of the repository.
