import com.google.fhir.model.r4.Extension

fun test(v: Extension.Value) {
    v.asString()
    v.asBoolean()
    v.asInteger()
}
