/**
 * @file SdcEvaluator.kt
 * Contains declarations for SdcEvaluator.kt.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Questionnaire

/**
 * Basic evaluator for SDC expressions and constraints.
 */
object SdcEvaluator {
    private const val MAX_ITERATIONS = 5

    /**
     * Evaluates SDC calculatedExpression extensions across the Questionnaire and updates the answer map.
     * Iterates repeatedly to allow cascading calculations (e.g. A = 1, B = A + 1) to settle.
     *
     * @param questionnaire The FHIR Questionnaire containing items with calculatedExpressions.
     * @param currentAnswers The `context` map of linkId to answer value, acting as the variable state environment.
     * @return A new map with the evaluated answers updated.
     */
    fun evaluateCalculatedExpressions(
        questionnaire: Questionnaire,
        currentAnswers: Map<String, Any>,
    ): Map<String, Any> {
        val updatedAnswers = currentAnswers.toMutableMap()
        var changed: Boolean

        var iterations = 0
        do {
            changed = false
            questionnaire.item.forEach { item ->
                changed = changed or evaluateItem(item, updatedAnswers)
            }
            iterations++
        } while (changed && iterations < MAX_ITERATIONS)

        return updatedAnswers
    }

    /**
     * Evaluates a single Questionnaire Item (and its nested items) for a calculatedExpression.
     *
     * @param item The Questionnaire Item to evaluate.
     * @param answers The mutable map of linkId to current answer values.
     * @return True if any answer was changed/calculated during evaluation, false otherwise.
     */
    private fun evaluateItem(
        item: Questionnaire.Item,
        answers: MutableMap<String, Any>,
    ): Boolean {
        var changed = false
        val linkId = item.linkId?.value ?: return false

        val calcExt =
            item.extension.find {
                it.url == "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression"
            }
        if (calcExt != null) {
            val exprExt = calcExt.extension.find { it.url == "expression" }
            val exprString =
                exprExt
                    ?.value
                    ?.asString()
                    ?.value
                    ?.value ?: calcExt.value
                    ?.asString()
                    ?.value
                    ?.value
            if (exprString != null) {
                val result = evaluateMathExpression(exprString, answers)
                if (result != null && answers[linkId] != result) {
                    answers[linkId] = result
                    changed = true
                }
            }
        }

        item.item.forEach { nested ->
            changed = changed or evaluateItem(nested, answers)
        }
        return changed
    }

    /**
     * Replaces variable tokens (e.g., %linkId) in a math expression string with their current values
     * and evaluates the resulting expression.
     *
     * @param expression The mathematical expression string.
     * @param answers The map containing the current values for variables.
     * @return The evaluated Float result, or null if evaluation fails.
     */
    private fun evaluateMathExpression(
        expression: String,
        answers: Map<String, Any>,
    ): Float? {
        var expr = expression
        answers.forEach { (key, value) ->
            val numVal = (value as? Float) ?: (value as? String)?.toFloatOrNull() ?: 0f
            expr = expr.replace("%$key", numVal.toString())
        }

        expr = expr.replace(Regex("%[a-zA-Z0-9_]+"), "0")

        return try {
            evalSimpleMath(expr)
        } catch (e: NumberFormatException) {
            println("Math evaluation error: ${e.message}")
            null
        } catch (e: IllegalArgumentException) {
            println("Math evaluation error: ${e.message}")
            null
        }
    }

    /**
     * Parses and evaluates a basic arithmetic math expression supporting parentheses,
     * multiplication, division, addition, and subtraction.
     *
     * @param str The fully substituted mathematical expression.
     * @return The evaluated Float result.
     */
    private fun evalSimpleMath(str: String): Float = parseSimpleMath(str)

    /**
     * Parses a string expression into a Float value.
     * @param str Parameter str
     * @return the parsed float
     */
    private fun parseSimpleMath(str: String): Float {
        var s = str.replace(" ", "")
        if (s.isEmpty()) return 0f

        while (s.contains("(")) {
            val endIdx = s.indexOf(')')
            val startIdx = s.substring(0, endIdx).lastIndexOf('(')
            val inner = s.substring(startIdx + 1, endIdx)
            val res = parseSimpleMath(inner)
            s = s.substring(0, startIdx) + res + s.substring(endIdx + 1)
        }

        s = processMultiplicationAndDivision(s)
        s = processAdditionAndSubtraction(s)
        return s.toFloat()
    }

    /**
     * Helper
     * @param str The str.
     * @return The result.
     */
    private fun processMultiplicationAndDivision(str: String): String {
        var s = str
        val mulDivRegex = Regex("(-?\\d+\\.?\\d*)[*/](-?\\d+\\.?\\d*)")
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
     * Helper
     * @param str The str.
     * @return The result.
     */
    private fun processAdditionAndSubtraction(str: String): String {
        var s = str
        val addSubRegex = Regex("(-?\\d+\\.?\\d*)[+-](-?\\d+\\.?\\d*)")
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
}
