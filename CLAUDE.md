# DOCoMETRe

Eclipse RCP application (Java 21, SWT/JFace, GEF), built with Maven/Tycho (`Docometre.Releng`).
Main plugins : `Docometre` (application) and `Process` (diagrams model and code generation),
tests in the `Docometre.Tests` fragment (JUnit 5).

## Checking changes

Tycho may not be usable (Maven Central unreachable), but `tools/headless-check/check.sh` compiles
`Process` and `Docometre` against the real Eclipse bundles and runs all the tests of `Docometre.Tests`
(see `tools/headless-check/README.md`).

- Run `tools/headless-check/check.sh` before every commit that changes Java code, properties files
  or tests, and do not commit if it fails. Report its result (compilation, number of tests run and failed).
- Never filter or ignore compiler errors to get a "clean" result : every error reported by javac is real.
- This check does not run the application : say so when a change also needs to be tried in the user interface.
