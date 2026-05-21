package com.uparix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Calculator")
class CalculatorTest {

    private final Calculator calculator = new Calculator();

    @Test
    @DisplayName("adds two numbers")
    void adds() {
        assertThat(calculator.add(2, 3)).isEqualTo(5);
    }

    @Test
    @DisplayName("subtracts two numbers")
    void subtracts() {
        assertThat(calculator.subtract(5, 3)).isEqualTo(2);
    }

    @Test
    @DisplayName("divides two numbers")
    void divides() {
        assertThat(calculator.divide(10, 2)).isEqualTo(5);
    }

    @Test
    @DisplayName("rejects division by zero")
    void rejectsDivisionByZero() {
        assertThatThrownBy(() -> calculator.divide(1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero");
    }
}