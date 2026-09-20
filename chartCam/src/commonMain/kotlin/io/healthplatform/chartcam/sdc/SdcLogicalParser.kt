/**
 * @file SdcLogicalParser.kt
 * Recursive descent parser for evaluating SDC boolean and comparison expressions.
 */
package io.healthplatform.chartcam.sdc

/**
 * Parser for evaluating boolean logic, parentheses, and relational operators in SDC expressions.
 *
 * @param input The expression string to parse.
 * @param answers The context answers map.
 */
internal class SdcLogicalParser(
    private val input: String,
    private val answers: Map<String, Any?>,
) {
    private var pos: Int = 0

    /**
     * Parses the logical expression into a boolean result.
     *
     * @return The evaluated boolean result.
     */
    fun parse(): Boolean {
        val result = parseOr()
        skipWhitespace()
        require(pos >= input.length) {
            "Unexpected character at position $pos in expression: $input"
        }
        return result
    }

    /**
     * Skips whitespace characters at the current scanner position.
     */
    private fun skipWhitespace() {
        while (pos < input.length && input[pos].isWhitespace()) {
            pos++
        }
    }

    /**
     * Matches and consumes a specific token string at the current position.
     *
     * @param token The token string to match.
     * @return True if the token matched and was consumed, false otherwise.
     */
    private fun match(token: String): Boolean {
        skipWhitespace()
        return if (input.startsWith(token, pos)) {
            pos += token.length
            true
        } else {
            false
        }
    }

    /**
     * Parses logical OR expressions (||).
     *
     * @return The evaluated boolean result.
     */
    private fun parseOr(): Boolean {
        var left = parseAnd()
        while (match("||")) {
            val right = parseAnd()
            left = left || right
        }
        return left
    }

    /**
     * Parses logical AND expressions (&&).
     *
     * @return The evaluated boolean result.
     */
    private fun parseAnd(): Boolean {
        var left = parseFactor()
        while (match("&&")) {
            val right = parseFactor()
            left = left && right
        }
        return left
    }

    /**
     * Parses a factor (either a parenthesized expression or an atomic comparison).
     *
     * @return The evaluated boolean result.
     */
    private fun parseFactor(): Boolean {
        skipWhitespace()
        return when {
            match("!") -> !parseFactor()
            match("(") -> {
                val res = parseOr()
                require(match(")")) { "Missing closing parenthesis in expression: $input" }
                res
            }
            else -> parseComparison()
        }
    }

    /**
     * Checks if a logical operator exists at position p.
     *
     * @param str The string being evaluated.
     * @param p The character position.
     * @return True if && or || begins at position p.
     */
    private fun isLogicalOperatorAt(
        str: String,
        p: Int,
    ): Boolean = str.startsWith("&&", p) || str.startsWith("||", p)

    /**
     * Parses an atomic comparison expression or boolean literal.
     *
     * @return The evaluated boolean outcome.
     */
    private fun parseComparison(): Boolean {
        skipWhitespace()
        val start = pos
        var inQuotes = false
        var quoteChar = ' '
        while (pos < input.length) {
            val c = input[pos]
            if (c == '\'' || c == '"') {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
            } else if (!inQuotes && (c == ')' || isLogicalOperatorAt(input, pos))) {
                break
            }
            pos++
        }
        val token = input.substring(start, pos).trim()
        require(token.isNotEmpty()) { "Empty comparison token in expression: $input" }

        val found = findOperatorOutsideQuotes(token) ?: return evaluateBooleanToken(token)
        val foundOp = found.first
        val opIndex = found.second
        val lhs = token.substring(0, opIndex).trim()
        val rhs = token.substring(opIndex + foundOp.length).trim()

        val numRes = compareNumeric(lhs, foundOp, rhs)
        return numRes ?: compareStrings(lhs, foundOp, rhs)
    }

    /**
     * Resolves a numeric value from an operand string or context variable.
     *
     * @param s The operand string.
     * @return The parsed Float value, or null if non-numeric.
     */
    private fun resolveNumeric(s: String): Float? {
        if (s.startsWith("%")) {
            val v = answers[s.removePrefix("%")]
            val num = (v as? Number)?.toFloat()
            val sVal = v?.toString()
            return if (num != null) {
                num
            } else if (sVal != null) {
                sVal.toFloatOrNull()
            } else {
                null
            }
        }
        return s.toFloatOrNull()
    }

    /**
     * Resolves a string value from an operand string or context variable.
     *
     * @param s The operand string.
     * @return The resolved string value.
     */
    private fun resolveString(s: String): String {
        if (s.startsWith("%")) {
            val v = answers[s.removePrefix("%")]
            return if (v != null) v.toString() else ""
        }
        return s.trim('\'', '"')
    }

    /**
     * Compares two numeric operand strings.
     *
     * @param lhsStr Left-hand operand string.
     * @param op The comparison operator.
     * @param rhsStr Right-hand operand string.
     * @return True/false comparison result, or null if non-numeric.
     */
    private fun compareNumeric(
        lhsStr: String,
        op: String,
        rhsStr: String,
    ): Boolean? {
        val lhsNum = resolveNumeric(lhsStr)
        val rhsNum = resolveNumeric(rhsStr)
        if (lhsNum == null || rhsNum == null) return null
        return evaluateNumericOperator(lhsNum, op, rhsNum)
    }

    /**
     * Evaluates a numeric comparison operator.
     *
     * @param lhsNum Left-hand float number.
     * @param op Comparison operator.
     * @param rhsNum Right-hand float number.
     * @return True/false comparison result.
     */
    private fun evaluateNumericOperator(
        lhsNum: Float,
        op: String,
        rhsNum: Float,
    ): Boolean =
        when (op) {
            ">=" -> lhsNum >= rhsNum
            "<=" -> lhsNum <= rhsNum
            "==" -> lhsNum == rhsNum
            "!=" -> lhsNum != rhsNum
            ">" -> lhsNum > rhsNum
            else -> lhsNum < rhsNum
        }

    /**
     * Compares two string operand strings.
     *
     * @param lhsStr Left-hand operand string.
     * @param op Comparison operator.
     * @param rhsStr Right-hand operand string.
     * @return True/false comparison result.
     */
    private fun compareStrings(
        lhsStr: String,
        op: String,
        rhsStr: String,
    ): Boolean {
        val lhsVal = resolveString(lhsStr)
        val rhsVal = resolveString(rhsStr)
        return when (op) {
            "==" -> lhsVal == rhsVal
            "!=" -> lhsVal != rhsVal
            ">=" -> lhsVal >= rhsVal
            "<=" -> lhsVal <= rhsVal
            ">" -> lhsVal > rhsVal
            else -> lhsVal < rhsVal
        }
    }

    /**
     * Evaluates a single token as a boolean literal or variable.
     *
     * @param token The token string.
     * @return Evaluated boolean value.
     */
    private fun evaluateBooleanToken(token: String): Boolean =
        when (token.lowercase()) {
            "true" -> true
            "false" -> false
            else -> {
                if (token.startsWith("%")) {
                    val varName = token.removePrefix("%")
                    val v = answers[varName]
                    when (v) {
                        is Boolean -> v
                        is String -> v.lowercase() == "true"
                        is Number -> v.toDouble() != 0.0
                        else -> v != null
                    }
                } else {
                    error("Invalid boolean literal or unresolved variable: $token")
                }
            }
        }

    /**
     * Finds comparison operator outside quotes.
     *
     * @param token The comparison token string.
     * @return The operator and its index, or null if none found.
     */
    private fun findOperatorOutsideQuotes(token: String): Pair<String, Int>? {
        val compOps = listOf(">=", "<=", "==", "!=", ">", "<")
        var inQuotes = false
        var quoteChar = ' '
        for (i in token.indices) {
            val c = token[i]
            if (c == '\'' || c == '"') {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
            } else if (!inQuotes) {
                val matched = compOps.firstOrNull { token.startsWith(it, i) }
                if (matched != null) return matched to i
            }
        }
        return null
    }
}
