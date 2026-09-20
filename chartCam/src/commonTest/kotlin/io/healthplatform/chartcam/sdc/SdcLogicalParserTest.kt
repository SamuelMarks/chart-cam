/**
 * @file SdcLogicalParserTest.kt
 * Comprehensive unit tests for SdcLogicalParser to ensure 100% line and branch coverage.
 */

package io.healthplatform.chartcam.sdc

import kotlin.test.Test
import kotlin.test.assertFailsWith
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
        assertTrue(SdcLogicalParser("true", emptyMap()).parse())
        assertFalse(SdcLogicalParser("false", emptyMap()).parse())
        assertTrue(SdcLogicalParser("  true  ", emptyMap()).parse())
        assertFalse(SdcLogicalParser("  false  ", emptyMap()).parse())

        // Trailing character after a complete logical factor
        val ex =
            assertFailsWith<IllegalArgumentException> {
                SdcLogicalParser("(true) &", emptyMap()).parse()
            }
        assertTrue(ex.message!!.contains("Unexpected character at position"))
    }

    /**
     * Tests negation and parenthesized expressions including missing closing parenthesis.
     */
    @Test
    fun testNegationAndParentheses() {
        assertFalse(SdcLogicalParser("!true", emptyMap()).parse())
        assertTrue(SdcLogicalParser("!false", emptyMap()).parse())
        assertTrue(SdcLogicalParser("!(false)", emptyMap()).parse())
        assertTrue(SdcLogicalParser("!(!true)", emptyMap()).parse())

        val ex =
            assertFailsWith<IllegalArgumentException> {
                SdcLogicalParser("(true", emptyMap()).parse()
            }
        assertTrue(ex.message!!.contains("Missing closing parenthesis"))
    }

    /**
     * Tests boolean logic short-circuiting and operators: &&, ||.
     */
    @Test
    fun testAndOrLogic() {
        assertTrue(SdcLogicalParser("true && true", emptyMap()).parse())
        assertFalse(SdcLogicalParser("true && false", emptyMap()).parse())
        assertFalse(SdcLogicalParser("false && true", emptyMap()).parse())
        assertFalse(SdcLogicalParser("false && false", emptyMap()).parse())

        assertTrue(SdcLogicalParser("true || false", emptyMap()).parse())
        assertTrue(SdcLogicalParser("false || true", emptyMap()).parse())
        assertTrue(SdcLogicalParser("true || true", emptyMap()).parse())
        assertFalse(SdcLogicalParser("false || false", emptyMap()).parse())

        // Multiple chained
        assertTrue(SdcLogicalParser("false || false || true", emptyMap()).parse())
        assertFalse(SdcLogicalParser("true && true && false", emptyMap()).parse())
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
        assertTrue(SdcLogicalParser("%num1 >= 10", answers).parse())
        assertTrue(SdcLogicalParser("%num2 >= 10", answers).parse())
        assertFalse(SdcLogicalParser("%num1 >= 11", answers).parse())

        // Less than or equal
        assertTrue(SdcLogicalParser("%num1 <= 10", answers).parse())
        assertTrue(SdcLogicalParser("%num1 <= 20", answers).parse())
        assertFalse(SdcLogicalParser("%num2 <= 10", answers).parse())

        // Equal
        assertTrue(SdcLogicalParser("%num1 == 10", answers).parse())
        assertTrue(SdcLogicalParser("%num1 == %num3", answers).parse())
        assertFalse(SdcLogicalParser("%num1 == 20", answers).parse())

        // Not equal
        assertTrue(SdcLogicalParser("%num1 != 20", answers).parse())
        assertFalse(SdcLogicalParser("%num1 != 10", answers).parse())

        // Greater than
        assertTrue(SdcLogicalParser("%num2 > 15", answers).parse())
        assertFalse(SdcLogicalParser("%num1 > 10", answers).parse())
        assertFalse(SdcLogicalParser("%num1 > 20", answers).parse())

        // Less than
        assertTrue(SdcLogicalParser("%num1 < 15", answers).parse())
        assertFalse(SdcLogicalParser("%num1 < 10", answers).parse())
        assertFalse(SdcLogicalParser("%num2 < 10", answers).parse())

        // String converted to float
        assertTrue(SdcLogicalParser("%strNum > 15", answers).parse())
        assertTrue(SdcLogicalParser("%strNum < 16", answers).parse())
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
        assertTrue(SdcLogicalParser("%name == \"Alice\"", answers).parse())
        assertFalse(SdcLogicalParser("%name == 'Bob'", answers).parse())
        assertTrue(SdcLogicalParser("%name != 'Bob'", answers).parse())
        assertFalse(SdcLogicalParser("%name != 'Alice'", answers).parse())

        // String relational comparisons
        assertTrue(SdcLogicalParser("'b' > 'a'", emptyMap()).parse())
        assertFalse(SdcLogicalParser("'a' > 'b'", emptyMap()).parse())
        assertTrue(SdcLogicalParser("'a' < 'b'", emptyMap()).parse())
        assertFalse(SdcLogicalParser("'b' < 'a'", emptyMap()).parse())
        assertTrue(SdcLogicalParser("'b' >= 'b'", emptyMap()).parse())
        assertTrue(SdcLogicalParser("'b' >= 'a'", emptyMap()).parse())
        assertFalse(SdcLogicalParser("'a' >= 'b'", emptyMap()).parse())
        assertTrue(SdcLogicalParser("'a' <= 'a'", emptyMap()).parse())
        assertTrue(SdcLogicalParser("'a' <= 'b'", emptyMap()).parse())
        assertFalse(SdcLogicalParser("'b' <= 'a'", emptyMap()).parse())

        // Variable to variable string comparison
        assertTrue(SdcLogicalParser("%name != %role", answers).parse())
        assertFalse(SdcLogicalParser("%name == %role", answers).parse())

        // Missing variable defaults to empty string
        assertTrue(SdcLogicalParser("%missing == ''", answers).parse())
        assertFalse(SdcLogicalParser("%missing == 'something'", answers).parse())
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

        assertTrue(SdcLogicalParser("%boolTrue", answers).parse())
        assertFalse(SdcLogicalParser("%boolFalse", answers).parse())
        assertTrue(SdcLogicalParser("%strTrue", answers).parse())
        assertFalse(SdcLogicalParser("%strFalse", answers).parse())
        assertTrue(SdcLogicalParser("%numNonZero", answers).parse())
        assertFalse(SdcLogicalParser("%numZero", answers).parse())
        assertTrue(SdcLogicalParser("%objNonNull", answers).parse())
        assertFalse(SdcLogicalParser("%objNull", answers).parse())
    }

    /**
     * Tests error handling on invalid syntax, empty comparison tokens, and unresolved literals.
     */
    @Test
    fun testErrorHandling() {
        // Invalid boolean literal
        assertFailsWith<IllegalStateException> {
            SdcLogicalParser("invalid_literal", emptyMap()).parse()
        }

        // Empty comparison token inside parentheses
        assertFailsWith<IllegalArgumentException> {
            SdcLogicalParser("()", emptyMap()).parse()
        }

        // Malformed comparison: no left hand side
        assertFalse(SdcLogicalParser("== 5", emptyMap()).parse())
    }

    /**
     * Tests operator inside quotes to verify findOperatorOutsideQuotes doesn't falsely match quotes.
     */
    @Test
    fun testOperatorInsideQuotes() {
        val answers = mapOf("text" to "a >= b")
        assertTrue(SdcLogicalParser("%text == 'a >= b'", answers).parse())
        assertTrue(SdcLogicalParser("%text == \"a >= b\"", answers).parse())
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
        assertFalse(SdcLogicalParser("10 == %nonNum", answers).parse())
        // Left non-numeric, right numeric
        assertFalse(SdcLogicalParser("%nonNum == 10", answers).parse())
        // Null variable numeric comparison
        assertFalse(SdcLogicalParser("%nullVal > 10", answers).parse())

        // Mixed quote characters
        assertTrue(SdcLogicalParser("%mixedSingle == \"single 'quote' text\"", answers).parse())
        assertTrue(SdcLogicalParser("%mixedDouble == 'double \"quote\" text'", answers).parse())

        // Quoted strings on LHS with operator inside quotes
        assertTrue(SdcLogicalParser("'a >= b' == %text", answers).parse())
        assertTrue(SdcLogicalParser("\"a >= b\" == %text", answers).parse())
        assertTrue(SdcLogicalParser("\"a 'nested' >= b\" == %text", answers + ("text" to "a 'nested' >= b")).parse())
        assertTrue(SdcLogicalParser("'a \"nested\" >= b' == %text", answers + ("text" to "a \"nested\" >= b")).parse())

        // Missing variable on RHS
        assertTrue(SdcLogicalParser("'' == %missing", answers).parse())
        assertTrue(SdcLogicalParser("%missing == %missing", answers).parse())

        // Nested parentheses around comparison
        assertTrue(SdcLogicalParser("((%intNum == 42))", answers).parse())
    }
}
