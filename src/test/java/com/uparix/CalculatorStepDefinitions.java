package com.uparix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Glue code mapping the Gherkin steps in {@code features/calculator.feature}
 * onto the {@link Calculator} production type.
 */
public class CalculatorStepDefinitions {

    private Calculator calculator;
    private int result;
    private Throwable thrown;

    @Given("a calculator")
    public void aCalculator() {
        calculator = new Calculator();
        thrown = null;
    }

    @When("I add {int} and {int}")
    public void iAdd(int a, int b) {
        result = calculator.add(a, b);
    }

    @When("I subtract {int} from {int}")
    public void iSubtract(int subtrahend, int minuend) {
        result = calculator.subtract(minuend, subtrahend);
    }

    @When("I divide {int} by {int}")
    public void iDivide(int dividend, int divisor) {
        thrown = catchThrowable(() -> result = calculator.divide(dividend, divisor));
    }

    @Then("the result is {int}")
    public void theResultIs(int expected) {
        assertThat(thrown).isNull();
        assertThat(result).isEqualTo(expected);
    }

    @Then("the operation is rejected with message {string}")
    public void theOperationIsRejectedWith(String message) {
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }
}
