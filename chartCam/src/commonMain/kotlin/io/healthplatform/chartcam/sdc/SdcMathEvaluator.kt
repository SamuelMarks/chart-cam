/**
 * @file SdcMathEvaluator.kt
 * Evaluates mathematical and string expressions within SDC questionnaires.
 */
package io.healthplatform.chartcam.sdc

private const val CONCAT_PREFIX_LEN = 7

/**
 * Utility object for parsing and evaluating arithmetic expressions and string operations in SDC.
 */
internal object SdcMathEvaluator {
    /**
     * Evaluates a mathematical expression string.
     *
     * @param str The substituted arithmetic expression string.
     * @return The resulting Float value.
     */
    fun evalSimpleMath(str: String): Float = parseSimpleMath(str)

    /**
     * Parses a sanitized arithmetic expression string into a Float.
     *
     * @param str The expression string.
     * @return The evaluated Float outcome.
     */
    private fun parseSimpleMath(str: String): Float {
        var s = str.replace(" ", "")
        if (s.isEmpty()) return 0f

        var parenDepth = 0
        for (ch in s) {
            if (ch == '(') parenDepth++
            if (ch == ')') parenDepth--
            require(parenDepth >= 0) { "Mismatched parentheses in expression: $str" }
        }
        require(parenDepth == 0) { "Mismatched parentheses in expression: $str" }

        while (s.contains("(")) {
            val endIdx = s.indexOf(')')
            val startIdx = if (endIdx != -1) s.substring(0, endIdx).lastIndexOf('(') else -1
            require(startIdx != -1 && endIdx != -1) { "Invalid parenthesis ordering in expression: $str" }
            val inner = s.substring(startIdx + 1, endIdx)
            val res = parseSimpleMath(inner)
            s = s.substring(0, startIdx) + res + s.substring(endIdx + 1)
        }

        val sanitizedOps =
            s
                .replace("*-", "*")
                .replace("/-", "/")
                .replace("+-", "-")
                .replace("--", "+")
        require(!Regex("[*+-/]{2,}").containsMatchIn(sanitizedOps)) {
            "Malformed consecutive operators in expression: $s"
        }

        s = processMultiplicationAndDivision(s)
        s = processAdditionAndSubtraction(s)
        return s.toFloat()
    }

    /**
     * Evaluates all multiplication and division operators in the expression.
     *
     * @param str The expression string.
     * @return The transformed expression string.
     */
    private fun processMultiplicationAndDivision(str: String): String {
        var s = str
        val mulDivRegex = Regex("""(-?\d+\.?\d*)[*/](-?\d+\.?\d*)""")
        while (s.contains("*") || s.contains("/")) {
            val match = mulDivRegex.find(s) ?: break
            val op = match.value
            val parts = op.split("*", "/")
            val isMul = op.contains("*")
            val a = parts[0].toFloat()
            val b = parts[1].toFloat()
            val res = if (isMul) a * b else a / b
            s = s.replaceFirst(op, res.toString())
        }
        return s
    }

    /**
     * Evaluates all addition and subtraction operators in the expression.
     *
     * @param str The expression string.
     * @return The transformed expression string.
     */
    private fun processAdditionAndSubtraction(str: String): String {
        var s = str
        val addSubRegex = Regex("""(-?\d+\.?\d*)[+-](-?\d+\.?\d*)""")
        while (s.contains("+") || s.drop(1).contains("-")) {
            var opMatch: MatchResult? = null
            var startIndex = 0
            var m = addSubRegex.find(s, startIndex)
            while (m != null && startIndex < s.length && opMatch == null) {
                if (m.range.first > 0 || (s.length > m.range.last + 1 && s[m.range.last + 1] in listOf('+', '-'))) {
                    opMatch = m
                } else {
                    startIndex = m.range.last
                    m = addSubRegex.find(s, startIndex)
                }
            }

            val match = opMatch ?: addSubRegex.find(s) ?: break
            val op = match.value
            val opIdx = op.drop(1).indexOfFirst { it == '+' || it == '-' } + 1
            val a = op.substring(0, opIdx).toFloat()
            val b = op.substring(opIdx + 1).toFloat()
            val res = if (op[opIdx] == '+') a + b else a - b
            s = s.replaceFirst(op, res.toString())
        }
        return s
    }

    /**
     * Splits arguments in a comma-separated parameter string while ignoring commas within quotation marks.
     *
     * @param input The parameter string to split.
     * @return List of argument strings.
     */
    fun splitArgumentsRespectingQuotes(input: String): List<String> {
        val args = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '
        var depth = 0
        for (c in input) {
            if (c == '\'' || c == '"') {
                if (!inQuotes) {
                    inQuotes = true
                    quoteChar = c
                } else if (c == quoteChar) {
                    inQuotes = false
                }
                current.append(c)
            } else if (!inQuotes) {
                if (c == '(') depth++
                if (c == ')' && depth > 0) depth--
                if (c == ',' && depth == 0) {
                    args.add(current.toString().trim())
                    current = StringBuilder()
                    continue
                }
                current.append(c)
            } else {
                current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            args.add(current.toString().trim())
        }
        return args
    }

    /**
     * Splits an expression by the plus symbol while ignoring plus signs within quotation marks.
     *
     * @param input The string expression to split.
     * @return List of token strings.
     */
    fun splitByPlusRespectingQuotes(input: String): List<String> {
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inSingle = false
        var inDouble = false
        for (c in input) {
            when (c) {
                '\'' -> {
                    if (!inDouble) inSingle = !inSingle
                    current.append(c)
                }
                '"' -> {
                    if (!inSingle) inDouble = !inDouble
                    current.append(c)
                }
                '+' -> {
                    if (!inSingle && !inDouble) {
                        parts.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(c)
                    }
                }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString().trim())
        }
        return parts
    }

    /**
     * Evaluates string expressions (such as concatenation or interpolation).
     *
     * @param expression The expression string.
     * @param answers The current answers context map.
     * @return A [Result] enclosing the concatenated string result.
     */
    fun evaluateStringExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): Result<String> =
        runCatching {
            val expr = expression.trim()
            if (expr.startsWith("concat(") && expr.endsWith(")")) {
                val inner = expr.substring(CONCAT_PREFIX_LEN, expr.length - 1)
                val parts = splitArgumentsRespectingQuotes(inner)
                parts.joinToString("") { part ->
                    val trimmed = part.trim()
                    if (trimmed.startsWith("%")) {
                        val varName = trimmed.removePrefix("%")
                        answers[varName]?.toString() ?: ""
                    } else {
                        trimmed.trim('\'', '"')
                    }
                }
            } else {
                val parts = splitByPlusRespectingQuotes(expr)
                parts.joinToString("") { part ->
                    val trimmed = part.trim()
                    if (trimmed.startsWith("%")) {
                        val varName = trimmed.removePrefix("%")
                        answers[varName]?.toString() ?: ""
                    } else {
                        trimmed.trim('\'', '"')
                    }
                }
            }
        }
}
