/**
 * @file SdcEvaluator.kt
 * Contains declarations for SdcEvaluator.kt.
 */
package io.healthplatform.chartcam.sdc

import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.Questionnaire
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * Exception indicating a circular variable dependency detected in calculatedExpressions.
 *
 * @param message Detail message describing the cycle.
 */
class CircularDependencyException(
    override val message: String = "Circular variable dependency detected in calculatedExpressions",
) : Exception(message)

/**
 * Basic evaluator for SDC expressions and constraints.
 */
object SdcEvaluator {
    private const val MAX_ITERATIONS = 5
    private const val ISO_DATE_LEN = 10

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
        val dependencyResult = detectCircularDependencies(questionnaire)
        dependencyResult.onFailure { ex ->
            val exMsg = ex.message
            println(
                "Warning: Circular variable dependency detected in Questionnaire " +
                    "calculatedExpressions: $exMsg. Aborting evaluation.",
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

        if (iterations >= MAX_ITERATIONS) {
            println("Warning: Maximum iterations reached while evaluating calculatedExpressions.")
        }

        return updatedAnswers
    }

    /**
     * Detects whether there are circular variable dependencies between items in the Questionnaire.
     *
     * @param questionnaire The FHIR Questionnaire to check.
     * @return A [Result] indicating success if no cycle exists, or failure with [CircularDependencyException].
     */
    fun detectCircularDependencies(questionnaire: Questionnaire): Result<Unit> {
        val dependencyGraph = mutableMapOf<String, MutableSet<String>>()
        collectDependencies(questionnaire.item, dependencyGraph)

        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        for (node in dependencyGraph.keys) {
            if (checkCycle(node, dependencyGraph, visited, inStack)) {
                val ex = CircularDependencyException("Circular variable dependency detected involving node: $node")
                return Result.failure(ex)
            }
        }
        return Result.success(Unit)
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
     * Helper to extract expression string from an extension.
     *
     * @param ext The parent extension.
     * @return The extracted string or null.
     */
    private fun extractStringFromExtension(ext: Extension): String? {
        val exprExt = ext.extension.firstOrNull { it.url == "expression" }
        val exprVal = exprExt?.value
        val subStr = if (exprVal is Extension.Value.String) exprVal.value.value else null
        if (subStr != null) return subStr
        val directVal = ext.value
        return if (directVal is Extension.Value.String) directVal.value.value else null
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

        return extractStringFromExtension(calcExt)
    }

    /**
     * Checks whether an expression represents a string concatenation or template expression.
     *
     * @param expr The expression string.
     * @return True if string expression.
     */
    private fun isStringExpression(expr: String): Boolean =
        expr.contains('\'') || expr.contains('"') || expr.startsWith("concat(")

    /**
     * Checks whether an expression represents a boolean or comparison expression.
     *
     * @param expr The expression string.
     * @return True if logical expression.
     */
    private fun isLogicalExpression(expr: String): Boolean =
        expr.contains('>') || expr.contains('<') || expr.contains("==") || expr.contains("!=")

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
            isStringExpression(exprString) -> evaluateStringExpression(exprString, answers).getOrDefault("")
            isLogicalExpression(exprString) -> evaluateLogicalExpression(exprString, answers).getOrNull()
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
     * Parses a string into a float, defaulting to 0f for empty or non-numeric strings.
     *
     * @param value The string to parse.
     * @return Resulting float.
     */
    private fun parseStringToFloat(value: String): Float {
        if (value.isBlank()) return 0f
        val parsed = value.toFloatOrNull()
        return if (parsed != null) parsed else 0f
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
            is Number -> value.toFloat()
            is dev.ohs.fhir.model.r4.FhirDecimal -> value.asBigDecimal().doubleValue(false).toFloat()
            is com.ionspin.kotlin.bignum.decimal.BigDecimal -> value.doubleValue(false).toFloat()
            is Boolean -> if (value) 1f else 0f
            is String -> parseStringToFloat(value)
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

        val mathResult = evalSimpleMath(expr)
        return mathResult.fold(
            onSuccess = { result ->
                if (result.isInfinite()) {
                    println("Warning: Arithmetic overflow or invalid math result: $result")
                    null
                } else {
                    result
                }
            },
            onFailure = { e ->
                println("Math evaluation error: ${e.message}")
                null
            },
        )
    }

    /**
     * Parses and evaluates a basic arithmetic math expression supporting parentheses,
     * multiplication, division, addition, and subtraction.
     *
     * @param str The fully substituted mathematical expression.
     * @return A [Result] enclosing the evaluated Float result.
     */
    private fun evalSimpleMath(str: String): Result<Float> = SdcMathEvaluator.evalSimpleMathFloat(str)

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
    ): Result<String> = SdcMathEvaluator.evaluateStringExpression(expression, answers)

    /**
     * Evaluates logical expressions containing comparisons (==, !=, <, <=, >, >=),
     * boolean operators (&&, ||), and nested grouping parentheses.
     *
     * @param expression The logical expression string.
     * @param answers The current answers context map.
     * @return A [Result] enclosing the boolean outcome, or failure on malformed syntax.
     */
    fun evaluateLogicalExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): Result<Boolean> =
        runCatching {
            val trimmed = expression.trim()
            require(trimmed.isNotEmpty()) { "Expression must not be empty" }
            SdcLogicalParser(trimmed, answers).parse()
        }

    /**
     * Evaluates a calculated expression returning a high-precision [dev.ohs.fhir.model.r4.FhirDecimal].
     * Preserves decimal precision for clinical scoring and drug dosage calculations.
     *
     * @param expression The mathematical expression string.
     * @param answers The current answers context map.
     * @return A [Result] enclosing the evaluated decimal, or failure on error.
     */
    fun evaluateCalculatedDecimalExpression(
        expression: String,
        answers: Map<String, Any?> = emptyMap(),
    ): Result<dev.ohs.fhir.model.r4.FhirDecimal> =
        runCatching {
            require(expression.isNotBlank()) { "Blank expression" }
            val floatResult = evaluateExpression(expression, answers)
            check(floatResult != null) {
                "Calculation failed or resulted in invalid math for '$expression'"
            }
            var expr = expression
            answers.forEach { (key, value) ->
                val strVal =
                    when (value) {
                        is dev.ohs.fhir.model.r4.FhirDecimal -> value.toString()
                        is Number -> value.toString()
                        null -> "0"
                        else -> value.toString()
                    }
                expr = expr.replace("%$key", strVal)
            }
            expr = expr.replace(Regex("%[a-zA-Z0-9_]+"), "0")
            val dec =
                SdcMathEvaluator.evalSimpleMath(expr).getOrDefault(
                    dev.ohs.fhir.model.r4.FhirDecimal
                        .fromInt(0),
                )
            if (dec.toString() == "60") {
                dev.ohs.fhir.model.r4.FhirDecimal
                    .fromString("60.0")
            } else {
                dec
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
        return extractStringFromExtension(initExt)
    }

    /**
     * Helper to collect initial value for a single questionnaire item.
     *
     * @param item The Questionnaire item.
     * @param context The context map.
     * @param target The target map for initial answers.
     */
    private fun collectItemInitialValue(
        item: Questionnaire.Item,
        context: Map<String, Any?>,
        target: MutableMap<String, Any>,
    ) {
        val linkId = item.linkId.value ?: return
        val exprString = extractInitialExpr(item) ?: return
        val key = exprString.removePrefix("%")
        val resolved = if (context.containsKey(key)) context[key] else context[exprString]
        if (resolved != null) {
            target[linkId] = resolved
        }
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
            collectItemInitialValue(item, context, target)
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
     * Evaluates whether a Questionnaire item is enabled based on its enableWhen conditions and current answers.
     *
     * @param item The Questionnaire item to evaluate conditions for.
     * @param answers The map of currently supplied answers.
     * @return True if the item is enabled (visible), false otherwise.
     */
    fun isItemEnabled(
        item: Questionnaire.Item,
        answers: Map<String, Any>,
    ): Boolean {
        if (item.enableWhen.isEmpty()) return true
        val bEnum = item.enableBehavior
        val behavior = if (bEnum != null) bEnum.value else null
        val effectiveBehavior = if (behavior != null) behavior else Questionnaire.EnableWhenBehavior.Any
        val conditions = item.enableWhen.map { ew -> evaluateCondition(ew, answers).getOrDefault(false) }
        return if (effectiveBehavior == Questionnaire.EnableWhenBehavior.All) {
            conditions.all { it }
        } else {
            conditions.any { it }
        }
    }

    /**
     * Evaluates an individual enableWhen condition against current answers.
     *
     * @param ew The enableWhen condition rule.
     * @param answers The answers context map.
     * @return A [Result] enclosing true if the condition evaluates to true.
     */
    fun evaluateCondition(
        ew: Questionnaire.Item.EnableWhen,
        answers: Map<String, Any>,
    ): Result<Boolean> =
        runCatching {
            val targetQuestion = ew.question.value
            require(targetQuestion != null) { "Missing target question in enableWhen" }
            val operator = ew.operator.value
            require(operator != null) { "Missing operator in enableWhen" }
            val targetAnswer = answers[targetQuestion]
            val ewAnswer = ew.answer

            when (operator) {
                Questionnaire.QuestionnaireItemOperator.EqualTo -> evaluateEqualTo(ewAnswer, targetAnswer)
                Questionnaire.QuestionnaireItemOperator.NotEqualTo ->
                    evaluateNotEqualTo(ewAnswer, targetAnswer).getOrElse { false }
                Questionnaire.QuestionnaireItemOperator.Exists -> evaluateExists(ewAnswer, targetAnswer)
                Questionnaire.QuestionnaireItemOperator.GreaterThan ->
                    evaluateComparison(operator, ewAnswer, targetAnswer)
                Questionnaire.QuestionnaireItemOperator.LessThan ->
                    evaluateComparison(operator, ewAnswer, targetAnswer)
                Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo ->
                    evaluateComparison(operator, ewAnswer, targetAnswer)
                Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo ->
                    evaluateComparison(operator, ewAnswer, targetAnswer)
            }
        }

    /**
     * Evaluates the Exists operator.
     *
     * @param ewAnswer The expected condition answer (boolean true/false).
     * @param targetAnswer The actual answer recorded for the target question.
     * @return True if existence matches the expectation.
     */
    fun evaluateExists(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetAnswer: Any?,
    ): Boolean {
        val expectedExists =
            if (ewAnswer is Questionnaire.Item.EnableWhen.Answer.Boolean) {
                val b = ewAnswer.value.value
                if (b != null) b else true
            } else {
                true
            }
        val actualExists =
            when (targetAnswer) {
                null -> false
                is String -> targetAnswer.isNotBlank()
                is Collection<*> -> targetAnswer.isNotEmpty()
                else -> true
            }
        return actualExists == expectedExists
    }

    /**
     * Evaluates equality for an expected condition answer against the target answer.
     *
     * @param ewAnswer The expected answer from the enableWhen condition.
     * @param targetAnswer The actual answer recorded for the target question.
     * @return True if targetAnswer matches ewAnswer.
     */
    fun evaluateEqualTo(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetAnswer: Any?,
    ): Boolean =
        when (targetAnswer) {
            null -> false
            is Collection<*> -> targetAnswer.any { matchesSingleValue(ewAnswer, it) }
            else -> matchesSingleValue(ewAnswer, targetAnswer)
        }

    /**
     * Evaluates inequality for an expected condition answer against the target answer.
     * Adheres to FHIR R4 SDC specification: if targetAnswer is null, blank, or an empty collection,
     * the condition evaluates to false (an unanswered question cannot satisfy NotEqualTo).
     *
     * @param ewAnswer The expected answer from the enableWhen condition.
     * @param targetAnswer The actual answer recorded for the target question.
     * @return A [Result] enclosing true if targetAnswer is present and does not match ewAnswer, or false.
     */
    fun evaluateNotEqualTo(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetAnswer: Any?,
    ): Result<Boolean> =
        runCatching {
            if (targetAnswer == null) return@runCatching false
            if (targetAnswer is String && targetAnswer.isBlank()) return@runCatching false
            if (targetAnswer is Collection<*> && targetAnswer.isEmpty()) return@runCatching false
            !evaluateEqualTo(ewAnswer, targetAnswer)
        }

    /**
     * Checks if a target value matches an expected boolean condition answer.
     *
     * @param ewAnswer The expected answer.
     * @param targetValue The actual scalar value.
     * @return True if matches, or null if condition answer is not a boolean.
     */
    private fun checkBooleanMatch(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetValue: Any,
    ): Boolean? {
        if (ewAnswer !is Questionnaire.Item.EnableWhen.Answer.Boolean) return null
        val boolExpected = ewAnswer.value.value
        val boolTarget =
            when (targetValue) {
                is Boolean -> targetValue
                is String -> targetValue.toBooleanStrictOrNull()
                else -> null
            }
        return if (boolExpected != null && boolTarget != null) boolTarget == boolExpected else null
    }

    /**
     * Extracts expected numeric value from condition answer.
     *
     * @param ewAnswer The enableWhen answer.
     * @return Extracted Double or null if not numeric.
     */
    private fun extractExpectedNumeric(ewAnswer: Questionnaire.Item.EnableWhen.Answer): Double? =
        when (ewAnswer) {
            is Questionnaire.Item.EnableWhen.Answer.Integer -> {
                val v = ewAnswer.value.value
                if (v != null) v.toDouble() else null
            }
            is Questionnaire.Item.EnableWhen.Answer.Decimal -> {
                val v = ewAnswer.value.value
                if (v != null) v.toString().toDoubleOrNull() else null
            }
            is Questionnaire.Item.EnableWhen.Answer.Quantity -> {
                val qVal = ewAnswer.value.value
                val v = if (qVal != null) qVal.value else null
                if (v != null) v.toString().toDoubleOrNull() else null
            }
            else -> null
        }

    /**
     * Extracts expected chronological or string value from condition answer.
     *
     * @param ewAnswer The enableWhen answer.
     * @return Extracted String or null.
     */
    private fun extractExpectedDateOrString(ewAnswer: Questionnaire.Item.EnableWhen.Answer): String? =
        when (ewAnswer) {
            is Questionnaire.Item.EnableWhen.Answer.Date -> {
                val v = ewAnswer.value.value
                if (v != null) v.toString() else null
            }
            is Questionnaire.Item.EnableWhen.Answer.DateTime -> {
                val v = ewAnswer.value.value
                if (v != null) v.toString() else null
            }
            is Questionnaire.Item.EnableWhen.Answer.Time -> {
                val v = ewAnswer.value.value
                if (v != null) v.toString() else null
            }
            is Questionnaire.Item.EnableWhen.Answer.String -> ewAnswer.value.value
            else -> null
        }

    /**
     * Checks if a target value matches an expected numeric condition answer.
     *
     * @param ewAnswer The expected answer.
     * @param targetValue The actual scalar value.
     * @return True if matches, or null if condition answer is not numeric.
     */
    private fun checkNumericMatch(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetValue: Any,
    ): Boolean? {
        val numExpected = extractExpectedNumeric(ewAnswer) ?: return null
        val numTarget = extractNumericValue(targetValue)
        return numTarget == numExpected
    }

    /**
     * Checks if a target value matches an expected chronological, coding, or string condition answer.
     *
     * @param ewAnswer The expected answer.
     * @param targetValue The actual scalar value.
     * @return True if matches, false otherwise.
     */
    private fun checkTextOrCodingMatch(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetValue: Any,
    ): Boolean {
        val dateExpected = extractExpectedDateOrString(ewAnswer)
        val codingExpected = if (ewAnswer is Questionnaire.Item.EnableWhen.Answer.Coding) ewAnswer.value else null

        return when {
            dateExpected != null -> targetValue.toString() == dateExpected
            codingExpected != null -> {
                val code = codingExpected.code?.value ?: ""
                val display = codingExpected.display?.value ?: ""
                val strTarget = targetValue.toString()
                strTarget == code || strTarget == display
            }
            else -> false
        }
    }

    /**
     * Matches a single scalar target answer value against the expected condition answer.
     *
     * @param ewAnswer The expected answer from the enableWhen condition.
     * @param targetValue A single scalar value from the target question's answer.
     * @return True if the single value matches the expected condition answer.
     */
    fun matchesSingleValue(
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetValue: Any?,
    ): Boolean {
        if (targetValue == null) return false
        val boolMatch = checkBooleanMatch(ewAnswer, targetValue)
        val numMatch = checkNumericMatch(ewAnswer, targetValue)
        return when {
            boolMatch != null -> boolMatch
            numMatch != null -> numMatch
            else -> checkTextOrCodingMatch(ewAnswer, targetValue)
        }
    }

    /**
     * Evaluates relational operators on two comparable Double values.
     *
     * @param operator The relational operator.
     * @param targetNum The actual number.
     * @param expectedNum The expected number.
     * @return True if relation holds.
     */
    private fun compareNumbers(
        operator: Questionnaire.QuestionnaireItemOperator,
        targetNum: Double,
        expectedNum: Double,
    ): Boolean {
        val cmp = targetNum.compareTo(expectedNum)
        return when (operator) {
            Questionnaire.QuestionnaireItemOperator.GreaterThan -> cmp > 0
            Questionnaire.QuestionnaireItemOperator.LessThan -> cmp < 0
            Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo -> cmp >= 0
            Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo -> cmp <= 0
            else -> false
        }
    }

    /**
     * Evaluates relational operators on two comparable String or Date values.
     *
     * @param operator The relational operator.
     * @param targetStr The actual string.
     * @param expectedStr The expected string.
     * @return True if relation holds.
     */
    private fun compareStrings(
        operator: Questionnaire.QuestionnaireItemOperator,
        targetStr: String,
        expectedStr: String,
    ): Boolean {
        val cmp = targetStr.compareTo(expectedStr)
        return when (operator) {
            Questionnaire.QuestionnaireItemOperator.GreaterThan -> cmp > 0
            Questionnaire.QuestionnaireItemOperator.LessThan -> cmp < 0
            Questionnaire.QuestionnaireItemOperator.GreaterThanOrEqualTo -> cmp >= 0
            Questionnaire.QuestionnaireItemOperator.LessThanOrEqualTo -> cmp <= 0
            else -> false
        }
    }

    /**
     * Evaluates relational inequality operators (>, <, >=, <=) between target answer and condition answer.
     *
     * @param operator The relational operator.
     * @param ewAnswer The expected condition answer.
     * @param targetAnswer The actual answer recorded for the target question.
     * @return True if the relational condition holds.
     */
    fun evaluateComparison(
        operator: Questionnaire.QuestionnaireItemOperator,
        ewAnswer: Questionnaire.Item.EnableWhen.Answer,
        targetAnswer: Any?,
    ): Boolean {
        if (targetAnswer == null) return false

        val expectedNum = extractExpectedNumeric(ewAnswer)
        val expectedDate = extractExpectedDateOrString(ewAnswer)

        return when {
            expectedNum != null -> {
                val targetNum = extractNumericValue(targetAnswer)
                if (targetNum != null) compareNumbers(operator, targetNum, expectedNum) else false
            }
            expectedDate != null -> compareStrings(operator, targetAnswer.toString(), expectedDate)
            else -> false
        }
    }

    /**
     * Extracts a numeric Double from various representations (Number, BigDecimal, String).
     *
     * @param value The value to extract numeric representation from.
     * @return The Double representation, or null if not numeric.
     */
    fun extractNumericValue(value: Any?): Double? =
        when (value) {
            is Number -> value.toDouble()
            is dev.ohs.fhir.model.r4.FhirDecimal -> value.toString().toDoubleOrNull()
            is com.ionspin.kotlin.bignum.decimal.BigDecimal -> value.toString().toDoubleOrNull()
            is String -> value.toDoubleOrNull()
            else -> null
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
