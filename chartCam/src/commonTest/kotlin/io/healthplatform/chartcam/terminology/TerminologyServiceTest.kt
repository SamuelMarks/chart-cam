package io.healthplatform.chartcam.terminology

import kotlin.test.Test
import kotlin.test.assertEquals

class TerminologyServiceTest {
    @Test
    fun testTerminologyService() {
        val loincSystem = TerminologyService.getLoincCodeSystem()
        assertEquals("http://loinc.org", loincSystem.url?.value)
        assertEquals("LOINC", loincSystem.name?.value)

        val snomedSystem = TerminologyService.getSnomedCodeSystem()
        assertEquals("http://snomed.info/sct", snomedSystem.url?.value)
        assertEquals("SNOMED CT", snomedSystem.name?.value)

        val loincCoding = TerminologyService.getLoincCoding("123-4", "Test Code")
        assertEquals("http://loinc.org", loincCoding.system?.value)
        assertEquals("123-4", loincCoding.code?.value)
        assertEquals("Test Code", loincCoding.display?.value)

        val loincCodingNoDisplay = TerminologyService.getLoincCoding("123-5")
        assertEquals("123-5", loincCodingNoDisplay.code?.value)
    }
}
