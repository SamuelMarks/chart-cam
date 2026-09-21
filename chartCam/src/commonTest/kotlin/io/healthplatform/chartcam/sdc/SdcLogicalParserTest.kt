/**
 * @file SdcLogicalParserTest.kt
 * Comprehensive unit tests for SdcLogicalParser to ensure 100% line and branch coverage.
 */

package io.healthplatform.chartcam.sdc

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests covering all branches, operators, syntax edge cases, and literals in [SdcLogicalParser].
 */
class SdcLogicalParserTest {
    /**
     * Tests basic boolean literals and unexpected trailing characters.
     */
    @Test
    fun testBooleanLiteralsAndUnexpectedCharacters() {
        assertTrue(SdcLogicalParser("true", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("false", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("  true  ", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("  false  ", emptyMap()).parse().getOrThrow())

        // Trailing character after a complete logical factor
        val res = SdcLogicalParser("(true) &", emptyMap()).parse()
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is IllegalArgumentException)
        assertTrue(res.exceptionOrNull()?.message?.contains("Unexpected character at position") == true)
    }

    /**
     * Tests negation and parenthesized expressions including missing closing parenthesis.
     */
    @Test
    fun testNegationAndParentheses() {
        assertFalse(SdcLogicalParser("!true", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("!false", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("!(false)", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("!(!true)", emptyMap()).parse().getOrThrow())

        val res = SdcLogicalParser("(true", emptyMap()).parse()
        assertTrue(res.isFailure)
        assertTrue(res.exceptionOrNull() is IllegalArgumentException)
        assertTrue(res.exceptionOrNull()?.message?.contains("Missing closing parenthesis") == true)
    }

    /**
     * Tests boolean logic short-circuiting and operators: &&, ||.
     */
    @Test
    fun testAndOrLogic() {
        assertTrue(SdcLogicalParser("true && true", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("true && false", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("false && true", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("false && false", emptyMap()).parse().getOrThrow())

        assertTrue(SdcLogicalParser("true || false", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("false || true", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("true || true", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("false || false", emptyMap()).parse().getOrThrow())

        // Multiple chained
        assertTrue(SdcLogicalParser("false || false || true", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("true && true && false", emptyMap()).parse().getOrThrow())
    }

    /**
     * Tests numeric comparisons: >=, <=, ==, !=, >, < with both literal numbers and context variables.
     */
    @Test
    fun testNumericComparisons() {
        val answers =
            mapOf<String, Any?>(
                "num1" to 10f,
                "num2" to 20.0,
                "num3" to 10,
                "strNum" to "15.5",
            )

        // Greater than or equal
        assertTrue(SdcLogicalParser("%num1 >= 10", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%num2 >= 10", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 >= 11", answers).parse().getOrThrow())

        // Less than or equal
        assertTrue(SdcLogicalParser("%num1 <= 10", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%num1 <= 20", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num2 <= 10", answers).parse().getOrThrow())

        // Equal
        assertTrue(SdcLogicalParser("%num1 == 10", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%num1 == %num3", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 == 20", answers).parse().getOrThrow())

        // Not equal
        assertTrue(SdcLogicalParser("%num1 != 20", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 != 10", answers).parse().getOrThrow())

        // Greater than
        assertTrue(SdcLogicalParser("%num2 > 15", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 > 10", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 > 20", answers).parse().getOrThrow())

        // Less than
        assertTrue(SdcLogicalParser("%num1 < 15", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num1 < 10", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%num2 < 10", answers).parse().getOrThrow())

        // String converted to float
        assertTrue(SdcLogicalParser("%strNum > 15", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%strNum < 16", answers).parse().getOrThrow())
    }

    /**
     * Tests string comparisons with single and double quotes, and variables.
     */
    @Test
    fun testStringComparisons() {
        val answers =
            mapOf<String, Any?>(
                "name" to "Alice",
                "role" to "Admin",
                "empty" to "",
            )

        // String equality and inequality
        assertTrue(SdcLogicalParser("%name == \"Alice\"", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%name == 'Bob'", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%name != 'Bob'", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%name != 'Alice'", answers).parse().getOrThrow())

        // String relational comparisons
        assertTrue(SdcLogicalParser("'b' > 'a'", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("'a' > 'b'", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("'a' < 'b'", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("'b' < 'a'", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("'b' >= 'b'", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("'b' >= 'a'", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("'a' >= 'b'", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("'a' <= 'a'", emptyMap()).parse().getOrThrow())
        assertTrue(SdcLogicalParser("'a' <= 'b'", emptyMap()).parse().getOrThrow())
        assertFalse(SdcLogicalParser("'b' <= 'a'", emptyMap()).parse().getOrThrow())

        // Variable to variable string comparison
        assertTrue(SdcLogicalParser("%name != %role", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%name == %role", answers).parse().getOrThrow())

        // Missing variable defaults to empty string
        assertTrue(SdcLogicalParser("%missing == ''", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%missing == 'something'", answers).parse().getOrThrow())
    }

    /**
     * Tests context variables evaluated directly as boolean expressions.
     */
    @Test
    fun testContextVariableBooleanEvaluation() {
        val answers =
            mapOf<String, Any?>(
                "boolTrue" to true,
                "boolFalse" to false,
                "strTrue" to "true",
                "strFalse" to "false",
                "numNonZero" to 1,
                "numZero" to 0,
                "objNonNull" to Any(),
                "objNull" to null,
            )

        assertTrue(SdcLogicalParser("%boolTrue", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%boolFalse", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%strTrue", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%strFalse", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%numNonZero", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%numZero", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%objNonNull", answers).parse().getOrThrow())
        assertFalse(SdcLogicalParser("%objNull", answers).parse().getOrThrow())
    }

    /**
     * Tests error handling on invalid syntax, empty comparison tokens, and unresolved literals.
     */
    @Test
    fun testErrorHandling() {
        // Invalid boolean literal
        val invalidRes = SdcLogicalParser("invalid_literal", emptyMap()).parse()
        assertTrue(invalidRes.isFailure)
        assertTrue(invalidRes.exceptionOrNull() is IllegalStateException)

        // Empty comparison token inside parentheses
        val emptyTokenRes = SdcLogicalParser("()", emptyMap()).parse()
        assertTrue(emptyTokenRes.isFailure)
        assertTrue(emptyTokenRes.exceptionOrNull() is IllegalArgumentException)

        // Malformed comparison: no left hand side
        assertFalse(SdcLogicalParser("== 5", emptyMap()).parse().getOrElse { false })

        // Error propagation across logical operators
        assertTrue(SdcLogicalParser("true || invalid_token", emptyMap()).parse().isFailure)
        assertTrue(SdcLogicalParser("true && invalid_token", emptyMap()).parse().isFailure)
        assertTrue(SdcLogicalParser("!invalid_token", emptyMap()).parse().isFailure)
    }

    /**
     * Tests operator inside quotes to verify findOperatorOutsideQuotes doesn't falsely match quotes.
     */
    @Test
    fun testOperatorInsideQuotes() {
        val answers = mapOf("text" to "a >= b")
        assertTrue(SdcLogicalParser("%text == 'a >= b'", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%text == \"a >= b\"", answers).parse().getOrThrow())
    }

    /**
     * Tests mixed numeric/string operands, mixed quotes, and parenthesis boundaries.
     */
    @Test
    fun testRemainingLogicalParserBranches() {
        val answers =
            mapOf<String, Any?>(
                "nonNum" to "not-a-number",
                "mixedSingle" to "single 'quote' text",
                "mixedDouble" to "double \"quote\" text",
                "intNum" to 42,
                "nullVal" to null,
                "text" to "a >= b",
            )

        // Left numeric, right non-numeric
        assertFalse(SdcLogicalParser("10 == %nonNum", answers).parse().getOrThrow())
        // Left non-numeric, right numeric
        assertFalse(SdcLogicalParser("%nonNum == 10", answers).parse().getOrThrow())
        // Null variable numeric comparison
        assertFalse(SdcLogicalParser("%nullVal > 10", answers).parse().getOrThrow())

        // Mixed quote characters
        assertTrue(SdcLogicalParser("%mixedSingle == \"single 'quote' text\"", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%mixedDouble == 'double \"quote\" text'", answers).parse().getOrThrow())

        // Quoted strings on LHS with operator inside quotes
        assertTrue(SdcLogicalParser("'a >= b' == %text", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("\"a >= b\" == %text", answers).parse().getOrThrow())
        assertTrue(
            SdcLogicalParser(
                "\"a 'nested' >= b\" == %text",
                answers + ("text" to "a 'nested' >= b"),
            ).parse().getOrThrow(),
        )
        assertTrue(
            SdcLogicalParser(
                "'a \"nested\" >= b' == %text",
                answers + ("text" to "a \"nested\" >= b"),
            ).parse().getOrThrow(),
        )

        // Missing variable on RHS
        assertTrue(SdcLogicalParser("'' == %missing", answers).parse().getOrThrow())
        assertTrue(SdcLogicalParser("%missing == %missing", answers).parse().getOrThrow())

        // Nested parentheses around comparison
        assertTrue(SdcLogicalParser("((%intNum == 42))", answers).parse().getOrThrow())
    }
}
