/**
 * @file SdcMathEvaluator.kt
 * Evaluates mathematical and string expressions within SDC questionnaires.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.FhirDecimal

private const val CONCAT_PREFIX_LEN = 7

/**
 * Utility object for parsing and evaluating arithmetic expressions and string operations in SDC.
 */
internal object SdcMathEvaluator {
    /**
     * Evaluates a mathematical expression string using arbitrary-precision [FhirDecimal].
     *
     * @param str The substituted arithmetic expression string.
     * @return A [Result] enclosing the resulting [FhirDecimal] value.
     */
    fun evalSimpleMath(str: String): Result<FhirDecimal> =
        runCatching {
            parseSimpleMathDecimal(str)
        }

    /**
     * Evaluates a mathematical expression string and returns a Float approximation wrapped in a [Result].
     *
     * @param str The substituted arithmetic expression string.
     * @return A [Result] enclosing the resulting Float value.
     */
    fun evalSimpleMathFloat(str: String): Result<Float> =
        evalSimpleMath(str).map { it.asBigDecimal().doubleValue(false).toFloat() }

    /**
     * Parses a sanitized arithmetic expression string into a [FhirDecimal].
     *
     * @param str The expression string.
     * @return The evaluated [FhirDecimal] outcome.
     */
    private fun parseSimpleMathDecimal(str: String): FhirDecimal {
        var s = str.replace(" ", "")
        if (s.isEmpty()) return FhirDecimal.fromInt(0)

        var parenDepth = 0
        for (ch in s) {
            if (ch == '(') parenDepth++
            if (ch == ')') parenDepth--
            require(parenDepth >= 0) { "Mismatched parentheses in expression: $str" }
        }
        require(parenDepth == 0) { "Mismatched parentheses in expression: $str" }

        while (s.contains("(")) {
            val endIdx = s.indexOf(')')
            val startIdx = s.substring(0, endIdx).lastIndexOf('(')
            val inner = s.substring(startIdx + 1, endIdx)
            val res = parseSimpleMathDecimal(inner)
            s = s.substring(0, startIdx) + res.toString() + s.substring(endIdx + 1)
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
        return FhirDecimal.fromString(s)
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
            val isMul = op.contains("*")
            val parts = op.split("*", "/")
            val a = FhirDecimal.fromString(parts[0])
            val b = FhirDecimal.fromString(parts[1])
            require(isMul || !b.isZero()) { "Division by zero in expression: $str" }
            val res =
                if (isMul) {
                    a * b
                } else {
                    runCatching { a / b }.getOrElse {
                        val bigA = a.asBigDecimal()
                        val bigB = b.asBigDecimal()
                        val mode =
                            com.ionspin.kotlin.bignum.decimal.DecimalMode(
                                decimalPrecision = 8L,
                                roundingMode = com.ionspin.kotlin.bignum.decimal.RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
                            )
                        val divResult = bigA.divide(bigB, mode)
                        FhirDecimal.fromBigDecimal(divResult)
                    }
                }
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
        while (true) {
            val match = addSubRegex.find(s) ?: break
            val op = match.value
            val opIdx = op.drop(1).indexOfFirst { it == '+' || it == '-' } + 1
            val a = FhirDecimal.fromString(op.substring(0, opIdx))
            val b = FhirDecimal.fromString(op.substring(opIdx + 1))
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
            val isConcat = expr.startsWith("concat(") && expr.endsWith(")")
            val parts =
                if (isConcat) {
                    val inner = expr.substring(CONCAT_PREFIX_LEN, expr.length - 1)
                    splitArgumentsRespectingQuotes(inner)
                } else {
                    splitByPlusRespectingQuotes(expr)
                }
            parts.joinToString("") { part ->
                val trimmed = part.trim()
                if (trimmed.startsWith("%")) {
                    val varName = trimmed.removePrefix("%")
                    val ans = answers[varName]
                    if (ans != null) ans.toString() else ""
                } else {
                    trimmed.trim('\'', '"')
                }
            }
        }
}
