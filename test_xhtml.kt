import com.google.fhir.model.r4.Narrative
import com.google.fhir.model.r4.Xhtml
import com.google.fhir.model.r4.Enumeration

fun main() {
    val n = Narrative.Builder().apply {
        status = Enumeration(value = Narrative.NarrativeStatus.Generated)
        div = Xhtml.Builder().apply { value = "Hello World" }.build()
    }.build()
    println(n.div.value)
}
