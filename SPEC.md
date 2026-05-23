# Java Quality-Gate Maven Setup — Specification

A reproducible spec for a TDD-ready Java project whose `mvn verify` enforces a
full stack of correctness and quality gates: tests, coverage, complexity,
mutation testing, static analysis, duplication detection, architecture rules,
compile-time null/bug checking, a banned-API check, security bug-pattern
analysis, and dependency CVE scanning — plus on-demand automated refactoring.

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
├── config/
│   └── pmd/
│       └── ruleset.xml            # curated PMD ruleset
└── src/
    ├── main/java/...              # production code (Error Prone + NullAway apply here)
    └── test/java/...              # tests (incl. ArchUnit rules as JUnit tests)
```

---

## 4. Toolchain summary

| Concern                       | Tool                         | Version                    | Phase it runs        | Fails build on             |
|-------------------------------|------------------------------|----------------------------|----------------------|----------------------------|
| Compile / target              | maven-compiler-plugin        | 3.15.0                     | compile              | newer-than-Java-25 API use |
| Null safety                   | NullAway (+ JSpecify)        | 0.13.4 / 1.0.0             | compile (prod only)  | nullable deref, etc.       |
| Bug patterns (source-hooked)  | Error Prone                  | 2.49.0                     | compile (prod only)  | known bug patterns         |
| Unit tests / BDD              | Surefire + JUnit5 + Cucumber | 3.5.2                      | test                 | failing test               |
| Architecture rules            | ArchUnit                     | 1.4.2                      | test                 | structural violation       |
| Coverage                      | JaCoCo                       | 0.8.14                     | test + verify        | < line/branch threshold    |
| Complexity × coverage         | CRAP (crap-java)             | 0.5.0                      | verify               | high CRAP score            |
| Static analysis + duplication | PMD + CPD (engine 7.24.0)    | plugin 3.28.0              | verify               | rule violation / duplicate |
| Bug patterns (bytecode)       | SpotBugs                     | 4.9.8.3                    | verify               | bug instance               |
| Security bug patterns         | Find Security Bugs           | 1.14.0                     | verify (in SpotBugs) | security finding           |
| Banned APIs                   | forbidden-apis               | 3.10                       | verify               | forbidden API call         |
| Dependency CVEs               | OWASP Dependency-Check       | 12.1.3                     | verify               | CVE with CVSS ≥ 7.0        |
| Mutation testing              | PItest                       | 1.24.1 (+junit5 1.2.3)     | on demand            | < mutation threshold       |
| Automated refactoring         | OpenRewrite                  | 6.12.0 (recipes 3.13/2.11) | on demand            | — (rewrites sources)       |

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
7. **Find Security Bugs rides on the existing SpotBugs execution** (registered
   as a detector `<plugin>` in SpotBugs' config) rather than as a separate
   plugin — one bytecode scan covers both general and security bug patterns.
8. **OpenRewrite is not bound to any phase.** It mutates source files, so it
   must never run unattended during `verify`; it is invoked manually via
   `rewrite:dryRun` / `rewrite:run`.

---

## 6. Files

## 7. Build commands

```bash
mvn test            # fast inner loop: unit tests + ArchUnit rules
mvn verify          # full gate: tests, coverage, CRAP, PMD/CPD, SpotBugs+FindSecBugs,
                    #            forbidden-apis, OWASP Dependency-Check
mvn org.pitest:pitest-maven:mutationCoverage   # mutation testing (on demand)
mvn rewrite:dryRun  # OpenRewrite: log proposed refactorings (review first)
mvn rewrite:run     # OpenRewrite: apply the active recipes in place
```

`verify` fails on: a failing/missing test, an architecture violation, coverage
below threshold, a high CRAP score, a PMD/CPD finding, a SpotBugs or Find
Security Bugs finding, a forbidden API call, or a dependency CVE scoring ≥ 7.0
(CVSS High; tune `dependencycheck.failBuildOnCVSS`). Compile fails on an Error
Prone bug pattern, a NullAway nullness error, or use of a newer-than-Java-25 API.

OpenRewrite is intentionally unbound from the lifecycle — it never rewrites
sources during `verify`; run `rewrite:dryRun`/`rewrite:run` explicitly.

> **Dependency-Check + NVD:** the first run downloads the full NVD CVE database
> (slow, and rate-limited without a key). Provide an [NVD API key](https://nvd.nist.gov/developers/request-an-api-key)
> via `-Dnvd.api.key=…` or the `NVD_API_KEY` env var for fast, reliable updates
> in CI. The database is cached under `~/.m2/repository/org/owasp/...` between runs.

---

## 8. Version-bump checklist (when raising the JDK or `release`)

Bump these together — all read bytecode and need an ASM matching the new target:

- [ ] `maven.compiler.release`
- [ ] `archunit.version` (ASM bundled)
- [ ] `jacoco.version`
- [ ] `pmd.version` (engine override; check its ASM)
- [ ] `spotbugs.plugin.version` (and its core)
- [ ] `findsecbugs.version` (loaded into SpotBugs; reads bytecode via SpotBugs' ASM)
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
