package com.uparix;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform suite that discovers every {@code .feature} file under the
 * {@code features} classpath resource and runs it with the Cucumber engine.
 * Surefire picks this up automatically during {@code mvn test}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.uparix")
@ConfigurationParameter(
        key = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-report.html")
class RunCucumberTest {
}
