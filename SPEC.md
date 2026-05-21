# Java Quality-Gate Maven Setup — Specification

A reproducible spec for a TDD-ready Java project whose `mvn verify` enforces a
full stack of correctness and quality gates: tests, coverage, complexity,
mutation testing, static analysis, duplication detection, architecture rules,
compile-time null/bug checking, and a banned-API check.

This document contains everything needed to recreate the setup from scratch.
Replace the placeholders below; nothing else is project-specific.

| Placeholder    | Meaning                   | Example           |
|----------------|---------------------------|-------------------|
| `GROUP_ID`     | Maven groupId             | `com.example`     |
| `ARTIFACT_ID`  | Maven artifactId          | `my-service`      |
| `BASE_PACKAGE` | root package of your code | `com.example.app` |

---

## 1. Prerequisites

- **JDK 25 or newer** (this spec builds on JDK 26, targeting Java 25 bytecode).
- **Maven 3.9+** (Maven 4.x recommended; see the `jvm.config` note in §6).

The project compiles with `--release 25`, so the produced artifact is a genuine
Java 25 jar even when built on a newer JDK. `--release` also restricts the API
surface, so accidental use of a newer API fails the build.

---

## 2. The single most important principle

**Every tool that reads `.class` files needs an ASM version new enough for your
target bytecode.** Java 25 produces class-file major version 69; Java 26 → 70.
A tool bundling an older ASM throws `Unsupported class file major version NN`.

This affects ArchUnit, JaCoCo, PMD, PItest, SpotBugs, forbidden-apis — and the
javac-hooking Error Prone. When you bump the JDK or `release`, expect to bump
this whole set together. The versions pinned here are chosen specifically
because they read Java 25/26 bytecode. See the bump checklist in §8.

---

## 3. Directory layout

```
.
├── pom.xml
├── .mvn/
│   └── jvm.config                 # Maven-JVM flags (silences its own Unsafe warning)
├── config/
│   └── pmd/
│       └── ruleset.xml            # curated PMD ruleset
└── src/
    ├── main/java/...              # production code (Error Prone + NullAway apply here)
    └── test/java/...              # tests (incl. ArchUnit rules as JUnit tests)
```

---

## 4. Toolchain summary

| Concern                       | Tool                         | Version                | Phase it runs       | Fails build on             |
|-------------------------------|------------------------------|------------------------|---------------------|----------------------------|
| Compile / target              | maven-compiler-plugin        | 3.15.0                 | compile             | newer-than-Java-25 API use |
| Null safety                   | NullAway (+ JSpecify)        | 0.13.4 / 1.0.0         | compile (prod only) | nullable deref, etc.       |
| Bug patterns (source-hooked)  | Error Prone                  | 2.49.0                 | compile (prod only) | known bug patterns         |
| Unit tests / BDD              | Surefire + JUnit5 + Cucumber | 3.5.2                  | test                | failing test               |
| Architecture rules            | ArchUnit                     | 1.4.2                  | test                | structural violation       |
| Coverage                      | JaCoCo                       | 0.8.14                 | test + verify       | < line/branch threshold    |
| Complexity × coverage         | CRAP (crap-java)             | 0.5.0                  | verify              | high CRAP score            |
| Static analysis + duplication | PMD + CPD (engine 7.24.0)    | plugin 3.28.0          | verify              | rule violation / duplicate |
| Bug patterns (bytecode)       | SpotBugs                     | 4.9.8.3                | verify              | bug instance               |
| Banned APIs                   | forbidden-apis               | 3.10                   | verify              | forbidden API call         |
| Mutation testing              | PItest                       | 1.24.1 (+junit5 1.2.3) | on demand           | < mutation threshold       |

---

## 5. Key design decisions (the non-obvious bits)

1. **Error Prone + NullAway run on production code only** (the `default-compile`
   execution). Test code is excluded because framework-initialised fields
   (Mockito `@Mock`, Cucumber glue) are NullAway false positives.
2. **Error Prone requires a forked javac with `--add-exports`/`--add-opens`**
   for `jdk.compiler` internals on JDK 16+. Hence `<fork>true</fork>` plus the
   `-J--add-exports=...` args.
3. **PMD's engine is overridden** to 7.24.0 via the plugin's `<dependencies>`,
   because the plugin bundles an older PMD whose ASM can't read Java 25/26.
4. **Most analysis runs in `verify`**, not `test`, to keep the TDD inner loop
   (`mvn test`) fast. ArchUnit is the deliberate exception — it's authored as
   JUnit tests, so it runs every `mvn test`.
5. **`.mvn/jvm.config`** silences a `sun.misc.Unsafe` warning emitted by
   *Maven's own* Guice, unrelated to your code. A no-op SLF4J binding
   (`slf4j-nop`, test scope) silences the "No SLF4J providers" notice.
6. **JaCoCo / PItest / NullAway are scoped to `BASE_PACKAGE`** so they ignore
   generated and third-party classes.

---

## 6. Files

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>GROUP_ID</groupId>
    <artifactId>ARTIFACT_ID</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

        <!-- Root package; scopes NullAway, JaCoCo and PItest. -->
        <base.package>BASE_PACKAGE</base.package>

        <!-- Compile against the Java 25 API and emit Java 25 bytecode (major
             version 69) even when building on a newer JDK. release enforces
             both the bytecode level and the API surface. -->
        <maven.compiler.release>25</maven.compiler.release>
        <maven.compiler.plugin.version>3.15.0</maven.compiler.plugin.version>

        <junit.version>5.12.2</junit.version>
        <junit.platform.version>1.12.2</junit.platform.version>
        <assertj.version>3.27.7</assertj.version>
        <mockito.version>5.14.2</mockito.version>
        <cucumber.version>7.22.1</cucumber.version>
        <archunit.version>1.4.2</archunit.version>
        <slf4j.version>2.0.17</slf4j.version>

        <surefire.version>3.5.2</surefire.version>
        <pmd.plugin.version>3.28.0</pmd.plugin.version>
        <!-- PMD engine override: bundles an ASM that reads Java 25/26 bytecode. -->
        <pmd.version>7.24.0</pmd.version>
        <cpd.min.tokens>100</cpd.min.tokens>
        <jacoco.version>0.8.14</jacoco.version>
        <pitest.version>1.24.1</pitest.version>
        <pitest.junit5.version>1.2.3</pitest.junit5.version>

        <!-- Compile-time correctness checks -->
        <errorprone.version>2.49.0</errorprone.version>
        <nullaway.version>0.13.4</nullaway.version>
        <jspecify.version>1.0.0</jspecify.version>
        <forbiddenapis.version>3.10</forbiddenapis.version>
        <spotbugs.plugin.version>4.9.8.3</spotbugs.plugin.version>

        <!-- Quality gates -->
        <coverage.line.min>0.80</coverage.line.min>
        <coverage.branch.min>0.70</coverage.branch.min>
        <mutation.threshold>70</mutation.threshold>
    </properties>

    <dependencies>
        <!-- JSpecify @Nullable etc. so production code can express nullness
             that NullAway then verifies. -->
        <dependency>
            <groupId>org.jspecify</groupId>
            <artifactId>jspecify</artifactId>
            <version>${jspecify.version}</version>
        </dependency>

        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Gherkin / BDD: Cucumber on the JUnit Platform -->
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-java</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.cucumber</groupId>
            <artifactId>cucumber-junit-platform-engine</artifactId>
            <version>${cucumber.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.platform</groupId>
            <artifactId>junit-platform-suite</artifactId>
            <version>${junit.platform.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- Architecture tests as JUnit tests -->
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>${archunit.version}</version>
            <scope>test</scope>
        </dependency>

        <!-- No-op SLF4J binding: silences "No SLF4J providers were found". -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-nop</artifactId>
            <version>${slf4j.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Compiler + Error Prone + NullAway (production sources only). -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven.compiler.plugin.version}</version>
                <configuration>
                    <release>${maven.compiler.release}</release>
                </configuration>
                <executions>
                    <execution>
                        <id>default-compile</id>
                        <configuration>
                            <!-- Fork javac so the JVM can open its internals to Error Prone. -->
                            <fork>true</fork>
                            <annotationProcessorPaths>
                                <path>
                                    <groupId>com.google.errorprone</groupId>
                                    <artifactId>error_prone_core</artifactId>
                                    <version>${errorprone.version}</version>
                                </path>
                                <path>
                                    <groupId>com.uber.nullaway</groupId>
                                    <artifactId>nullaway</artifactId>
                                    <version>${nullaway.version}</version>
                                </path>
                            </annotationProcessorPaths>
                            <compilerArgs>
                                <arg>-XDcompilePolicy=simple</arg>
                                <arg>--should-stop=ifError=FLOW</arg>
                                <arg>-Xplugin:ErrorProne -XepOpt:NullAway:AnnotatedPackages=${base.package} -Xep:NullAway:ERROR</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED</arg>
                                <arg>-J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED</arg>
                                <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED</arg>
                                <arg>-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED</arg>
                            </compilerArgs>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Unit tests (TDD inner loop) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>${surefire.version}</version>
            </plugin>

            <!-- Coverage + CRAP inputs -->
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>${jacoco.version}</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals><goal>prepare-agent</goal></goals>
                        <configuration>
                            <includes>
                                <include>${base.package}.*</include>
                            </includes>
                        </configuration>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>test</phase>
                        <goals><goal>report</goal></goals>
                    </execution>
                    <execution>
                        <id>check-coverage</id>
                        <phase>verify</phase>
                        <goals><goal>check</goal></goals>
                        <configuration>
                            <rules>
                                <rule>
                                    <element>BUNDLE</element>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>${coverage.line.min}</minimum>
                                        </limit>
                                        <limit>
                                            <counter>BRANCH</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>${coverage.branch.min}</minimum>
                                        </limit>
                                    </limits>
                                </rule>
                            </rules>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Complexity × coverage (CRAP) -->
            <plugin>
                <groupId>media.barney</groupId>
                <artifactId>crap-java-maven-plugin</artifactId>
                <version>0.5.0</version>
                <executions>
                    <execution>
                        <goals><goal>check</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <!-- PMD + CPD (engine pinned via plugin dependencies) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-pmd-plugin</artifactId>
                <version>${pmd.plugin.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>net.sourceforge.pmd</groupId>
                        <artifactId>pmd-core</artifactId>
                        <version>${pmd.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>net.sourceforge.pmd</groupId>
                        <artifactId>pmd-java</artifactId>
                        <version>${pmd.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>net.sourceforge.pmd</groupId>
                        <artifactId>pmd-javascript</artifactId>
                        <version>${pmd.version}</version>
                    </dependency>
                    <dependency>
                        <groupId>net.sourceforge.pmd</groupId>
                        <artifactId>pmd-jsp</artifactId>
                        <version>${pmd.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <rulesets>
                        <ruleset>${project.basedir}/config/pmd/ruleset.xml</ruleset>
                    </rulesets>
                    <failOnViolation>true</failOnViolation>
                    <printFailingErrors>true</printFailingErrors>
                    <minimumTokens>${cpd.min.tokens}</minimumTokens>
                    <includeTests>false</includeTests>
                </configuration>
                <executions>
                    <execution>
                        <id>pmd-and-cpd-check</id>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                            <goal>cpd-check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>

            <!-- Mutation testing (run on demand) -->
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>${pitest.version}</version>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                        <version>${pitest.junit5.version}</version>
                    </dependency>
                </dependencies>
                <configuration>
                    <targetClasses>
                        <param>${base.package}.*</param>
                    </targetClasses>
                    <targetTests>
                        <param>${base.package}.*</param>
                    </targetTests>
                    <mutationThreshold>${mutation.threshold}</mutationThreshold>
                    <outputFormats>
                        <param>HTML</param>
                        <param>XML</param>
                    </outputFormats>
                    <timestampedReports>false</timestampedReports>
                </configuration>
            </plugin>

            <!-- Banned / non-portable JDK APIs (production classes) -->
            <plugin>
                <groupId>de.thetaphi</groupId>
                <artifactId>forbiddenapis</artifactId>
                <version>${forbiddenapis.version}</version>
                <configuration>
                    <failOnUnsupportedJava>false</failOnUnsupportedJava>
                    <bundledSignatures>
                        <bundledSignature>jdk-unsafe</bundledSignature>
                        <bundledSignature>jdk-deprecated</bundledSignature>
                        <bundledSignature>jdk-non-portable</bundledSignature>
                        <bundledSignature>jdk-internal</bundledSignature>
                    </bundledSignatures>
                </configuration>
                <executions>
                    <execution>
                        <id>forbidden-apis-check</id>
                        <phase>verify</phase>
                        <goals><goal>check</goal></goals>
                    </execution>
                </executions>
            </plugin>

            <!-- SpotBugs: bytecode bug-pattern analysis -->
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <version>${spotbugs.plugin.version}</version>
                <configuration>
                    <effort>Max</effort>
                    <threshold>Medium</threshold>
                    <includeTests>false</includeTests>
                    <failOnError>true</failOnError>
                </configuration>
                <executions>
                    <execution>
                        <id>spotbugs-check</id>
                        <phase>verify</phase>
                        <goals><goal>check</goal></goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### `config/pmd/ruleset.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset name="quality-gate"
         xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0
                             https://pmd.sourceforge.io/ruleset_2_0_0.xsd">

    <description>
        Curated PMD 7 ruleset favouring real-defect rules over stylistic ones
        (style is left to Checkstyle/Spotless). Tune the excludes as needed.
    </description>

    <rule ref="category/java/errorprone.xml">
        <exclude name="AvoidLiteralsInIfCondition"/>
        <exclude name="MissingSerialVersionUID"/>
    </rule>
    <rule ref="category/java/bestpractices.xml">
        <exclude name="GuardLogStatement"/>
    </rule>
    <rule ref="category/java/design.xml">
        <exclude name="LawOfDemeter"/>
        <exclude name="LoosePackageCoupling"/>
    </rule>
    <rule ref="category/java/performance.xml"/>
    <rule ref="category/java/multithreading.xml"/>
    <rule ref="category/java/security.xml"/>
</ruleset>
```

### `.mvn/jvm.config`

```
--sun-misc-unsafe-memory-access=allow
```

Silences a `sun.misc.Unsafe` warning from Maven's own internals (older bundled
Guice). Requires JDK 23+. Alternatively, run the build with **Maven 4.x**, whose
newer Guice doesn't trigger the warning, and omit this file.

---

## 7. Build commands

```bash
mvn test            # fast inner loop: unit tests + ArchUnit rules
mvn verify          # full gate: tests, coverage, CRAP, PMD/CPD, SpotBugs, forbidden-apis
mvn org.pitest:pitest-maven:mutationCoverage   # mutation testing (on demand)
```

`verify` fails on: a failing/missing test, an architecture violation, coverage
below threshold, a high CRAP score, a PMD/CPD/SpotBugs finding, or a forbidden
API call. Compile fails on an Error Prone bug pattern, a NullAway nullness
error, or use of a newer-than-Java-25 API.

---

## 8. Version-bump checklist (when raising the JDK or `release`)

Bump these together — all read bytecode and need an ASM matching the new target:

- [ ] `maven.compiler.release`
- [ ] `archunit.version` (ASM bundled)
- [ ] `jacoco.version`
- [ ] `pmd.version` (engine override; check its ASM)
- [ ] `spotbugs.plugin.version` (and its core)
- [ ] `pitest.version`
- [ ] `forbiddenapis.version`
- [ ] `errorprone.version` — **most likely to lag a new JDK; check its release notes first**

After bumping, run `mvn clean verify`; an `Unsupported class file major version`
error means that tool's ASM is still too old.

---

## 9. Recommended source conventions

- Put production code under `BASE_PACKAGE`; annotate nullable references with
  `org.jspecify.annotations.@Nullable` so NullAway can verify them.
- Author architecture rules as a JUnit test class using ArchUnit's
  `@AnalyzeClasses(packages = "BASE_PACKAGE", importOptions = DoNotIncludeTests.class)`
  and `@ArchTest` fields. They then run on every `mvn test`.
- Keep the fast loop fast: unit tests + ArchUnit in `test`; everything heavier
  in `verify`.
```
