/**
 * @file SdcEvaluator.kt
 * Contains declarations for SdcEvaluator.kt.
 */
package io.healthplatform.chartcam.sdc

import com.google.fhir.model.r4.Questionnaire
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Basic evaluator for SDC expressions and constraints.
 */
object SdcEvaluator {
    private const val MAX_ITERATIONS = 5

    /**
     * Evaluates SDC calculatedExpression extensions across the Questionnaire and updates the answer map.
     * Iterates repeatedly to allow cascading calculations (e.g. A = 1, B = A + 1) to settle.
     * Detects circular dependencies and gracefully aborts.
     *
     * @param questionnaire The FHIR Questionnaire containing items with calculatedExpressions.
     * @param currentAnswers The `context` map of linkId to answer value, acting as the variable state environment.
     * @return A new map with the evaluated answers updated.
     */
    fun evaluateCalculatedExpressions(
        questionnaire: Questionnaire,
        currentAnswers: Map<String, Any>,
    ): Map<String, Any> {
        if (detectCircularDependencies(questionnaire)) {
            println(
                "Warning: Circular variable dependency detected in Questionnaire " +
                    "calculatedExpressions. Aborting evaluation.",
            )
            return currentAnswers
        }

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

        if (changed && iterations >= MAX_ITERATIONS) {
            println("Warning: Maximum iterations reached while evaluating calculatedExpressions.")
        }

        return updatedAnswers
    }

    /**
     * Detects whether there are circular variable dependencies between items in the Questionnaire.
     *
     * @param questionnaire The FHIR Questionnaire to check.
     * @return True if a circular dependency is detected, false otherwise.
     */
    fun detectCircularDependencies(questionnaire: Questionnaire): Boolean {
        val dependencyGraph = mutableMapOf<String, MutableSet<String>>()
        collectDependencies(questionnaire.item, dependencyGraph)

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        for (node in dependencyGraph.keys) {
            if (checkCycle(node, dependencyGraph, visited, inStack)) return true
        }
        return false
    }

    /**
     * Helper to detect cycle from a specific node using DFS.
     *
     * @param node The current node linkId.
     * @param dependencyGraph The dependency graph.
     * @param visited The set of all visited nodes.
     * @param inStack The set of nodes currently in recursion stack.
     * @return True if a cycle is detected, false otherwise.
     */
    private fun checkCycle(
        node: String,
        dependencyGraph: Map<String, Set<String>>,
        visited: MutableSet<String>,
        inStack: MutableSet<String>,
    ): Boolean {
        if (inStack.contains(node)) return true
        var cycleDetected = false
        if (!visited.contains(node)) {
            visited.add(node)
            inStack.add(node)

            val neighbors = dependencyGraph[node] ?: emptySet()
            for (neighbor in neighbors) {
                if (checkCycle(neighbor, dependencyGraph, visited, inStack)) {
                    cycleDetected = true
                    break
                }
            }
            inStack.remove(node)
        }
        return cycleDetected
    }

    /**
     * Helper to collect dependencies for each item with a calculatedExpression.
     *
     * @param items The list of Questionnaire items to traverse.
     * @param graph The mutable adjacency map from linkId to referenced variable linkIds.
     */
    private fun collectDependencies(
        items: List<Questionnaire.Item>,
        graph: MutableMap<String, MutableSet<String>>,
    ) {
        items.forEach { item ->
            val linkId = item.linkId.value
            if (linkId != null) {
                val expr = extractExpressionString(item)
                if (expr != null) {
                    val referenced =
                        Regex("%([a-zA-Z0-9_]+)")
                            .findAll(expr)
                            .map { it.groupValues[1] }
                            .toMutableSet()
                    graph[linkId] = referenced
                }
            }
            if (item.item.isNotEmpty()) {
                collectDependencies(item.item, graph)
            }
        }
    }

    /**
     * Extracts calculatedExpression string from a Questionnaire Item if present.
     *
     * @param item The Questionnaire Item to inspect.
     * @return The expression string or null.
     */
    private fun extractExpressionString(item: Questionnaire.Item): String? {
        val calcExt =
            item.extension.find {
                it.url == "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression"
            } ?: return null

        val exprExt = calcExt.extension.find { it.url == "expression" }
        return exprExt
            ?.value
            ?.asString()
            ?.value
            ?.value ?: calcExt.value
            ?.asString()
            ?.value
            ?.value
    }

    /**
     * Evaluates a calculated value based on expression type.
     *
     * @param exprString The expression string.
     * @param answers The current answers map.
     * @return The evaluated result.
     */
    fun evaluateCalculatedValue(
        exprString: String,
        answers: Map<String, Any>,
    ): Any? {
        if (exprString.isBlank()) return 0f
        return when {
            exprString.contains("'") ||
                exprString.contains("\"") ||
                exprString.startsWith("concat(") ->
                evaluateStringExpression(exprString, answers)
            exprString.contains(">") ||
                exprString.contains("<") ||
                exprString.contains("==") ||
                exprString.contains("!=") ->
                evaluateLogicalExpression(exprString, answers)
            else -> evaluateExpression(exprString, answers)
        }
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
        val linkId = item.linkId.value ?: return false

        val exprString = extractExpressionString(item)
        if (exprString != null) {
            val result = evaluateCalculatedValue(exprString, answers)
            if (result != null && answers[linkId] != result) {
                answers[linkId] = result
                changed = true
            }
        }

        item.item.forEach { nested ->
            changed = changed or evaluateItem(nested, answers)
        }
        return changed
    }

    /**
     * Checks whether an expression contains unsupported operators.
     *
     * @param expression The expression to validate.
     * @param answers The current answers context map.
     * @return True if unsupported operators are present, false otherwise.
     */
    private fun hasUnsupportedOperators(
        expression: String,
        answers: Map<String, Any?>,
    ): Boolean {
        val unsupportedOperators = listOf("^", "%", "==", "!=", "<=", ">=", "<", ">", "&&", "||", "&", "|")
        var sanitized = expression
        for ((key, _) in answers) {
            sanitized = sanitized.replace("%$key", "")
        }
        sanitized = sanitized.replace(Regex("%[a-zA-Z0-9_]+"), "")

        for (op in unsupportedOperators) {
            if (sanitized.contains(op)) {
                println("Warning: Unsupported operator '$op' in expression: $expression")
                return true
            }
        }
        return false
    }

    /**
     * Converts an answer value into a numeric float for calculation.
     *
     * @param value The value to convert.
     * @return The resulting Float representation.
     */
    private fun toNumericFloat(value: Any?): Float =
        when (value) {
            null -> 0f
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is Number -> value.toFloat()
            is Boolean -> if (value) 1f else 0f
            is String -> if (value.isBlank()) 0f else (value.toFloatOrNull() ?: 0f)
            else -> 0f
        }

    /**
     * Evaluates a math or FHIRPath-like expression string with context variables.
     * Handles malformed expressions, boundary values, arithmetic overflow, and unsupported operators gracefully.
     *
     * @param expression The mathematical expression string.
     * @param answers The map containing current values for variables.
     * @return The evaluated Float result, or null if evaluation fails.
     */
    fun evaluateExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): Float? {
        if (expression.isBlank() || hasUnsupportedOperators(expression, answers)) {
            return null
        }

        var expr = expression
        answers.forEach { (key, value) ->
            expr = expr.replace("%$key", toNumericFloat(value).toString())
        }

        // Replace any remaining unpopulated %variable with 0
        expr = expr.replace(Regex("%[a-zA-Z0-9_]+"), "0")

        return try {
            val result = evalSimpleMath(expr)
            if (result.isNaN() || result.isInfinite()) {
                println("Warning: Arithmetic overflow or invalid math result: $result")
                null
            } else {
                result
            }
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
     *
     * @param str Parameter str
     * @return the parsed float
     */
    private fun parseSimpleMath(str: String): Float {
        var s = str.replace(" ", "")
        if (s.isEmpty()) return 0f

        // Validate parenthesis matching
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

        // Check for consecutive invalid operator sequences (e.g. ++, *+, /*, +*)
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

    private const val CONCAT_PREFIX_LEN = 7
    private const val ISO_DATE_LEN = 10

    /**
     * Evaluates string expressions (such as concatenation or interpolation).
     *
     * @param expression The expression string.
     * @param answers The current answers context map.
     * @return The concatenated string result.
     */
    fun evaluateStringExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): String {
        val expr = expression.trim()
        if (expr.startsWith("concat(") && expr.endsWith(")")) {
            val inner = expr.substring(CONCAT_PREFIX_LEN, expr.length - 1)
            val parts = inner.split(",")
            return parts.joinToString("") { part ->
                val trimmed = part.trim()
                if (trimmed.startsWith("%")) {
                    val varName = trimmed.removePrefix("%")
                    answers[varName]?.toString() ?: ""
                } else {
                    trimmed.trim('\'', '"')
                }
            }
        }
        val parts = expr.split("+")
        return parts.joinToString("") { part ->
            val trimmed = part.trim()
            if (trimmed.startsWith("%")) {
                val varName = trimmed.removePrefix("%")
                answers[varName]?.toString() ?: ""
            } else {
                trimmed.trim('\'', '"')
            }
        }
    }

    /**
     * Evaluates logical expressions containing comparisons (==, !=, <, <=, >, >=) or boolean operators (&&, ||).
     *
     * @param expression The logical expression string.
     * @param answers The current answers context map.
     * @return Boolean result or null if malformed.
     */
    fun evaluateLogicalExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): Boolean? {
        val expr = expression.trim()
        return when {
            expr.contains("||") -> expr.split("||").any { evaluateLogicalExpression(it.trim(), answers) == true }
            expr.contains("&&") -> expr.split("&&").all { evaluateLogicalExpression(it.trim(), answers) == true }
            else -> evaluateComparison(expr, answers)
        }
    }

    /**
     * Evaluates an atomic comparison expression.
     *
     * @param expr The comparison expression.
     * @param answers The context answers map.
     * @return Boolean result or null.
     */
    private fun evaluateComparison(
        expr: String,
        answers: Map<String, Any?>,
    ): Boolean? {
        val compOps = listOf(">=", "<=", "==", "!=", ">", "<")
        val foundOp = compOps.firstOrNull { expr.contains(it) }
        val parts = if (foundOp != null) expr.split(foundOp) else emptyList()
        if (parts.size != 2 || foundOp == null) {
            return null
        }

        val lhsNum = evaluateExpression(parts[0].trim(), answers)
        val rhsNum = evaluateExpression(parts[1].trim(), answers)

        return if (lhsNum != null && rhsNum != null) {
            compareNumeric(foundOp, lhsNum, rhsNum)
        } else {
            compareStrings(foundOp, parts[0].trim(), parts[1].trim(), answers)
        }
    }

    /**
     * Compares two numbers.
     *
     * @param op The comparison operator.
     * @param lhs Left hand number.
     * @param rhs Right hand number.
     * @return Comparison result.
     */
    private fun compareNumeric(
        op: String,
        lhs: Float,
        rhs: Float,
    ): Boolean? =
        when (op) {
            ">=" -> lhs >= rhs
            "<=" -> lhs <= rhs
            "==" -> lhs == rhs
            "!=" -> lhs != rhs
            ">" -> lhs > rhs
            "<" -> lhs < rhs
            else -> null
        }

    /**
     * Compares two string values.
     *
     * @param op The comparison operator.
     * @param lhsStr Left hand string or variable.
     * @param rhsStr Right hand string or variable.
     * @param answers Answers context.
     * @return Comparison result.
     */
    private fun compareStrings(
        op: String,
        lhsStr: String,
        rhsStr: String,
        answers: Map<String, Any?>,
    ): Boolean? {
        val lhsVal = answers[lhsStr.removePrefix("%")]?.toString() ?: lhsStr.trim('\'', '"')
        val rhsVal = answers[rhsStr.removePrefix("%")]?.toString() ?: rhsStr.trim('\'', '"')
        return when (op) {
            "==" -> lhsVal == rhsVal
            "!=" -> lhsVal != rhsVal
            else -> null
        }
    }

    /**
     * Evaluates initial expressions across Questionnaire items using patient demographic
     * or previous encounter response context.
     *
     * @param questionnaire The FHIR Questionnaire containing items with initialExpressions.
     * @param context Map representing context variables (e.g. "patient.gender", "patient.age", "encounter.id").
     * @return Map of linkId to initial evaluated answers.
     */
    fun evaluateInitialExpressions(
        questionnaire: Questionnaire,
        context: Map<String, Any?> = emptyMap(),
    ): Map<String, Any> {
        val initialAnswers = mutableMapOf<String, Any>()
        collectInitialValues(questionnaire.item, context, initialAnswers)
        return initialAnswers
    }

    /**
     * Helper to extract initial expression from item.
     *
     * @param item Questionnaire item.
     * @return Initial expression string or null.
     */
    private fun extractInitialExpr(item: Questionnaire.Item): String? {
        val initExt =
            item.extension.find {
                it.url == "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-initialExpression"
            } ?: return null
        return initExt.extension
            .find { it.url == "expression" }
            ?.value
            ?.asString()
            ?.value
            ?.value ?: initExt.value
            ?.asString()
            ?.value
            ?.value
    }

    /**
     * Helper to collect initial values from questionnaire items recursively.
     *
     * @param items The list of Questionnaire items.
     * @param context The context map.
     * @param target The target map for initial answers.
     */
    private fun collectInitialValues(
        items: List<Questionnaire.Item>,
        context: Map<String, Any?>,
        target: MutableMap<String, Any>,
    ) {
        items.forEach { item ->
            val linkId = item.linkId.value
            val exprString = extractInitialExpr(item)
            if (linkId != null && exprString != null) {
                val key = exprString.removePrefix("%")
                val resolved = context[key] ?: context[exprString]
                if (resolved != null) {
                    target[linkId] = resolved
                }
            }
            if (item.item.isNotEmpty()) {
                collectInitialValues(item.item, context, target)
            }
        }
    }

    /**
     * Evaluates whether an item is enabled considering both its own enableWhen conditions
     * and the enableWhen state of all ancestor items in its hierarchy.
     *
     * @param item The target Questionnaire item.
     * @param ancestors The list of ancestor Questionnaire items from root down to parent.
     * @param answers The current answers context map.
     * @return True if the item and all its ancestors are enabled, false otherwise.
     */
    fun isItemHierarchyEnabled(
        item: Questionnaire.Item,
        ancestors: List<Questionnaire.Item> = emptyList(),
        answers: Map<String, Any>,
    ): Boolean {
        for (ancestor in ancestors) {
            if (!isItemEnabled(ancestor, answers)) return false
        }
        return isItemEnabled(item, answers)
    }

    /**
     * Evaluates date offset arithmetic.
     *
     * @param dateStr An ISO-8601 date string (e.g., "2026-01-01").
     * @param daysToAdd The number of days to add.
     * @return The resulting date string.
     */
    fun evaluateDateOffset(
        dateStr: String,
        daysToAdd: Int,
    ): String {
        val parsed = LocalDate.parse(dateStr.take(ISO_DATE_LEN))
        val period = DatePeriod(days = daysToAdd)
        return parsed.plus(period).toString()
    }
}
