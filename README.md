# ai-java-blueprint

A minimal Maven + JUnit 5 project laid out for test-driven development, with
code coverage, the CRAP metric, and mutation testing wired in.

## Layout

```
pom.xml
src/main/java/com/uparix/Calculator.java               sample production code
src/test/java/com/uparix/CalculatorTest.java           JUnit unit tests
src/test/java/com/uparix/RunCucumberTest.java          JUnit Platform suite (runs the features)
src/test/java/com/uparix/CalculatorStepDefinitions.java  Gherkin glue code
src/test/resources/features/calculator.feature         Gherkin feature describing the app
src/test/resources/junit-platform.properties           Cucumber/JUnit Platform config
```

## TDD inner loop

```bash
mvn test          # compile + run unit tests + generate JaCoCo coverage
```

Coverage HTML report: `target/site/jacoco/index.html`

## Quality gates

```bash
mvn verify        # tests + JaCoCo coverage thresholds (line 80% / branch 70%)
```

Thresholds live in `pom.xml` under `<properties>` (`coverage.line.min`,
`coverage.branch.min`, `mutation.threshold`) — tighten them as the project matures.

## CRAP metric

CRAP is computed by the `crap-java-maven-plugin`, which consumes the JaCoCo
report (per-method cyclomatic complexity and coverage). Its `check` goal is
bound to the `verify` phase and runs after JaCoCo:

```bash
mvn verify        # runs crap-java:check after the JaCoCo report
mvn crap-java:check   # run it on its own (needs an existing JaCoCo report)
```

Report: `target/crap-java/TEST-crap-java.xml` (JUnit format — one test case per
method). The build fails if any method's CRAP score exceeds the threshold
(default 8.0); set `<configuration><threshold>…</threshold></configuration>` on
the plugin to change it.

CRAP = `complexity^2 * (1 - coverage)^3 + complexity`. A method scores badly when
it is both complex and poorly tested; you lower it by adding tests or by
simplifying the method.

## BDD / Gherkin (Cucumber)

Behaviour is described in plain-language `.feature` files under
`src/test/resources/features/`. There is no standalone "gherkin Maven plugin";
the de-facto approach is Cucumber-JVM running on the JUnit Platform, so the
features execute through Surefire as part of `mvn test` — no extra goal needed.

- `calculator.feature` — scenarios for add, subtract, divide, and divide-by-zero.
- `CalculatorStepDefinitions.java` — glue mapping each step onto `Calculator`.
- `RunCucumberTest.java` — JUnit Platform suite that discovers the features.

```bash
mvn test          # runs both the JUnit tests and the Gherkin scenarios
```

Cucumber HTML report: `target/cucumber-report.html`. Add a `.feature` file and the
matching step methods to describe new behaviour.

## Mutation testing (PIT)

```bash
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```

Report: `target/pit-reports/index.html`. The build fails if the mutation score
drops below `mutation.threshold` (70%).