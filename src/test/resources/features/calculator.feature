Feature: Calculator
  As a user of the blueprint
  I want basic arithmetic operations
  So that I can add, subtract, and divide whole numbers safely

  Scenario: Adding two numbers
    Given a calculator
    When I add 2 and 3
    Then the result is 5

  Scenario: Subtracting two numbers
    Given a calculator
    When I subtract 3 from 5
    Then the result is 2

  Scenario Outline: Dividing two numbers
    Given a calculator
    When I divide <dividend> by <divisor>
    Then the result is <quotient>

    Examples:
      | dividend | divisor | quotient |
      | 10       | 2       | 5        |
      | 9        | 3       | 3        |
      | 7        | 7       | 1        |

  Scenario: Dividing by zero is rejected
    Given a calculator
    When I divide 1 by 0
    Then the operation is rejected with message "Cannot divide by zero"
