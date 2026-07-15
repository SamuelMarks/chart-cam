/**
 * @file TestExpression.kt
 * Contains declarations for TestExpression.kt.
 */
import com.google.fhir.model.r4.Extension

/**
 * A simple test function to verify that extension values can be safely extracted as primitives.
 *
 * @param v The FHIR Extension Value to be tested.
 */
fun test(v: Extension.Value) {
    v.asString()
    v.asBoolean()
    v.asInteger()
}
