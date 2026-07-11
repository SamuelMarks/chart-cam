package test
import com.google.fhir.model.r4.Date
import com.google.fhir.model.r4.FhirDate

/**
 * A simple test function that creates a FHIR Date.
 * @return A Date object representing 2020-01-01.
 */
fun foo() =
    Date.Builder().apply {
        value =
            FhirDate.fromString("2020-01-01")
    }
