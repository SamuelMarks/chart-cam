package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Questionnaire

/**
 * Basic evaluator for SDC expressions and constraints.
 */
object SdcEvaluator {
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
        } while (changed && iterations < 5)

        return updatedAnswers
    }

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
        } catch (e: Exception) {
            null
        }
    }

    private fun evalSimpleMath(str: String): Float {
        fun parse(str: String): Float {
            var s = str.replace(" ", "")
            if (s.isEmpty()) return 0f

            while (s.contains("(")) {
                val end = s.indexOf(')')
                val start = s.substring(0, end).lastIndexOf('(')
                val inner = s.substring(start + 1, end)
                val res = parse(inner)
                s = s.substring(0, start) + res + s.substring(end + 1)
            }

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

            val addSubRegex = Regex("(-?\\d+\\.?\\d*)[+-](-?\\d+\\.?\\d*)")
            while (s.contains("+") || s.drop(1).contains("-")) {
                var opMatch: MatchResult? = null
                var startIndex = 0
                while (startIndex < s.length) {
                    val m = addSubRegex.find(s, startIndex) ?: break
                    if (m.range.first > 0 || (s.length > m.range.last + 1 && s[m.range.last + 1] in listOf('+', '-'))) {
                        opMatch = m
                        break
                    }
                    startIndex = m.range.last
                }

                val match = opMatch ?: addSubRegex.find(s) ?: break
                val op = match.value
                val opIdx = op.drop(1).indexOfFirst { it == '+' || it == '-' } + 1
                val a = op.substring(0, opIdx).toFloat()
                val b = op.substring(opIdx + 1).toFloat()
                val isAdd = op[opIdx] == '+'
                val res = if (isAdd) a + b else a - b
                s = s.replaceFirst(op, res.toString())
            }
            return s.toFloat()
        }
        return parse(str)
    }
}
