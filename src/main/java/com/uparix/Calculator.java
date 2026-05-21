package com.uparix;

/**
 * Minimal sample type so the blueprint builds, tests, and reports out of the box.
 * Replace with your own production code as you red-green-refactor.
 */
public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * Branching logic exists purely to give JaCoCo a branch to cover and
     * PIT a conditional to mutate.
     */
    public int divide(int dividend, int divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("Cannot divide by zero");
        }
        return dividend / divisor;
    }
}