package com.uparix;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Architecture rules enforced as ordinary JUnit tests. ArchUnit scans the
 * compiled production classes (tests excluded) and fails the build when any
 * rule is violated, so {@code mvn test} keeps structure honest as the
 * blueprint grows.
 *
 * <p>Each {@code @ArchTest} field below is a self-contained, freezable rule.
 * The current set is intentionally general-purpose (it passes on the starter
 * code); the commented layered-architecture template at the bottom is the
 * pattern to enable once domain/application/adapter packages exist.
 */
@AnalyzeClasses(packages = "com.uparix", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** Stray {@code System.out}/{@code System.err} calls are almost always
     *  forgotten debug output — route through a logger instead. */
    @ArchTest
    static final ArchRule no_access_to_standard_streams =
            NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    /** Throwing raw {@code Exception}/{@code RuntimeException}/{@code Throwable}
     *  hides intent and forces broad catches; throw a specific type. */
    @ArchTest
    static final ArchRule no_generic_exceptions =
            NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    /** Standardise on one logging facade rather than {@code java.util.logging}. */
    @ArchTest
    static final ArchRule no_java_util_logging =
            NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    /** Prefer the {@code java.time} API over the legacy, mutable date/time types. */
    @ArchTest
    static final ArchRule prefer_java_time =
            noClasses()
                    .should().dependOnClassesThat()
                    .belongToAnyOf(java.util.Date.class, java.util.Calendar.class,
                            java.text.SimpleDateFormat.class)
                    .because("use java.time (Instant, LocalDate, Duration, ...) "
                            + "instead of the legacy java.util date/time API");

    /** No cyclic dependencies between top-level packages under com.uparix.
     *  allowEmptyShould tolerates the single-package starter state; the rule
     *  starts enforcing as soon as sub-packages appear. */
    @ArchTest
    static final ArchRule no_package_cycles =
            slices().matching("com.uparix.(*)..").should().beFreeOfCycles()
                    .allowEmptyShould(true);

    /*
     * --- Layered-architecture template (enable when the layers exist) ---
     *
     * Uncomment and adjust the package matchers once you split the code into
     * layers. With ImportOption.DoNotIncludeTests, only production classes are
     * checked. ensureAllClassesAreContainedInArchitecture() guarantees no
     * production class escapes a layer.
     *
     * @ArchTest
     * static final ArchRule layered_architecture =
     *         layeredArchitecture().consideringAllDependencies()
     *                 .layer("Domain").definedBy("com.uparix.domain..")
     *                 .layer("Application").definedBy("com.uparix.application..")
     *                 .layer("Adapters").definedBy("com.uparix.adapter..")
     *                 .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
     *                 .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters")
     *                 .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters")
     *                 .ensureAllClassesAreContainedInArchitecture();
     */
}
