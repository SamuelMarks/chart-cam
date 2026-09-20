/**
 * @file SdcExtensionsTest.kt
 * Contains declarations for SdcExtensionsTest.kt.
 */
package io.healthplatform.chartcam.fhir

import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.Extension
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.Integer
import dev.ohs.fhir.model.r4.Questionnaire
import dev.ohs.fhir.model.r4.terminologies.PublicationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Tests for SDC extension functions on FHIR resources.
 */
class SdcExtensionsTest {
    /**
     * Helper for creating FHIR strings.
     * @param s The string value.
     * @return The FHIR String builder.
     */
    private fun str(s: String) = FhirString.Builder().apply { value = s }

    /**
     * Test the `isHidden` extension.
     */
    @Test
    fun testIsHidden() {
        val hiddenExt =
            Extension.Builder(url = SdcExtensions.HIDDEN).apply {
                value = Extension.Value.Boolean(FhirBoolean.Builder().apply { value = true }.build())
            }
        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(hiddenExt)
                }.build()
        assertTrue(item.isHidden())

        val visibleItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertFalse(visibleItem.isHidden())
    }

    /**
     * Test getting the item control extension.
     */
    @Test
    fun testGetItemControl() {
        val coding = Coding.Builder().apply { code = Code.Builder().apply { value = "drop-down" } }
        val codeableConcept = CodeableConcept.Builder().apply { this.coding.add(coding) }.build()

        val controlExt =
            Extension.Builder(url = SdcExtensions.ITEM_CONTROL).apply {
                value = Extension.Value.CodeableConcept(codeableConcept)
            }

        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(controlExt)
                }.build()

        assertEquals("drop-down", item.getItemControl())

        val itemNoControl =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertNull(itemNoControl.getItemControl())
    }

    /**
     * Test getting min/max value extensions.
     */
    @Test
    fun testGetMinValueAndMaxValue() {
        val minExt =
            Extension.Builder(url = SdcExtensions.MIN_VALUE).apply {
                value = Extension.Value.Integer(Integer.Builder().apply { value = 10 }.build())
            }
        val maxExt =
            Extension.Builder(url = SdcExtensions.MAX_VALUE).apply {
                value = Extension.Value.Integer(Integer.Builder().apply { value = 50 }.build())
            }

        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    extension.add(minExt)
                    extension.add(maxExt)
                }.build()

        assertEquals(10f, item.getMinValue())
        assertEquals(50f, item.getMaxValue())
        assertNotNull(item.safeMinValue.getOrNull())
        assertNotNull(item.safeMaxValue.getOrNull())

        val emptyItem =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).build()
        assertNull(emptyItem.getMinValue())
        assertNull(emptyItem.getMaxValue())
    }

    /**
     * Test getting the localized text via translation extensions.
     */
    @Test
    fun testGetLocalizedText() {
        val langExt =
            Extension.Builder(url = "lang").apply {
                value = Extension.Value.String(str("es").build())
            }
        val contentExt =
            Extension.Builder(url = "content").apply {
                value = Extension.Value.String(str("Notas Clínicas").build())
            }

        val transExt =
            Extension.Builder(url = SdcExtensions.TRANSLATION).apply {
                extension.add(langExt)
                extension.add(contentExt)
            }

        val item =
            Questionnaire.Item
                .Builder(
                    linkId = str("1"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = str("Clinical Notes")
                    extension.add(transExt)
                }.build()

        assertEquals("Notas Clínicas", item.getLocalizedText("es"))
        assertEquals("Clinical Notes", item.getLocalizedText("en"))

        val itemNoExt =
            Questionnaire.Item
                .Builder(
                    linkId = str("2"),
                    type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                ).apply {
                    text = str("Default Text")
                }.build()

        assertEquals("Default Text", itemNoExt.getLocalizedText("ja"))
    }

    /**
     * Test Questionnaire.getLocalizedTitle extension with translation extensions and fallbacks.
     */
    @Test
    fun testQuestionnaireGetLocalizedTitle() {
        val langExtEs =
            Extension.Builder(url = "lang").apply {
                value = Extension.Value.String(str("es").build())
            }
        val contentExtEs =
            Extension.Builder(url = "content").apply {
                value = Extension.Value.String(str("Formulario de Dermatología").build())
            }
        val transExt =
            Extension.Builder(url = SdcExtensions.TRANSLATION).apply {
                extension.add(langExtEs)
                extension.add(contentExtEs)
            }

        val qWithTrans =
            Questionnaire
                .Builder(
                    status = Enumeration(value = PublicationStatus.Active),
                ).apply {
                    title = str("Dermatology Form")
                    extension.add(transExt)
                }.build()

        assertEquals("Formulario de Dermatología", qWithTrans.getLocalizedTitle("es"))
        assertEquals("Dermatology Form", qWithTrans.getLocalizedTitle("en"))

        val qWithoutTrans =
            Questionnaire
                .Builder(
                    status = Enumeration(value = PublicationStatus.Active),
                ).apply {
                    title = str("Triage Form")
                }.build()

        assertEquals("Triage Form", qWithoutTrans.getLocalizedTitle("es"))
        assertEquals("Triage Form", qWithoutTrans.getLocalizedTitle("ja"))
    }

    /**
     * Test safe accessors for unit, itemWeight, and choiceOrientation.
     */
    @Test
    fun testSafeUnitWeightAndOrientation() {
        val unitCoding =
            Coding(
                code = Code(value = "kg"),
                system =
                    dev.ohs.fhir.model.r4
                        .Uri(value = "http://unitsofmeasure.org"),
            )
        val unitExt = Extension(url = SdcExtensions.QUESTIONNAIRE_UNIT, value = Extension.Value.Coding(unitCoding))
        val weightExt =
            Extension(
                url = SdcExtensions.ITEM_WEIGHT,
                value =
                    Extension.Value.Decimal(
                        dev.ohs.fhir.model.r4
                            .Decimal(
                                value =
                                    dev.ohs.fhir.model.r4.FhirDecimal
                                        .fromString("2.5"),
                            ),
                    ),
            )
        val orientExt = Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.String(FhirString(value = "horizontal")))

        val item =
            Questionnaire.Item(
                linkId = FhirString(value = "q-weight"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension = listOf(unitExt, weightExt, orientExt),
            )

        val unitRes = item.safeUnit
        assertTrue(unitRes.isSuccess)
        assertEquals("kg", unitRes.getOrNull()?.code?.value)

        val weightRes = item.safeItemWeight
        assertTrue(weightRes.isSuccess)
        assertEquals("2.5", weightRes.getOrNull()?.toString())

        val orientRes = item.safeChoiceOrientation
        assertTrue(orientRes.isSuccess)
        assertEquals("horizontal", orientRes.getOrNull())
    }

    /**
     * Validates visual form control classification helpers across all supported values.
     */
    @Test
    fun testVisualControlsClassification() {
        fun makeItemWithControl(control: String?, loincCode: String? = null): Questionnaire.Item {
            val exts = mutableListOf<Extension>()
            if (control != null) {
                exts.add(Extension(url = SdcExtensions.ITEM_CONTROL, value = Extension.Value.String(FhirString(value = control))))
            }
            val codings = mutableListOf<Coding>()
            if (loincCode != null) {
                codings.add(Coding(code = Code(value = loincCode)))
            }
            return Questionnaire.Item(
                linkId = FhirString(value = "ctrl_q"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension = exts,
                code = codings,
            )
        }

        // Pain controls
        assertTrue(makeItemWithControl(SdcExtensions.ITEM_CONTROL_PAIN_VAS).isVisualPainControl())
        assertTrue(makeItemWithControl("wong-baker").isVisualPainControl())
        assertTrue(makeItemWithControl("slider", SdcExtensions.LOINC_PAIN_SEVERITY).isVisualPainControl())
        assertTrue(makeItemWithControl(null, SdcExtensions.LOINC_PAIN_SCORE).isVisualPainControl())
        assertFalse(makeItemWithControl("other", SdcExtensions.LOINC_PAIN_SEVERITY).isVisualPainControl())
        assertFalse(makeItemWithControl(null, "unrelated-loinc").isVisualPainControl())
        assertFalse(makeItemWithControl("slider", null).isVisualPainControl())

        // Fitzpatrick controls
        assertTrue(makeItemWithControl(SdcExtensions.ITEM_CONTROL_PALETTE).isFitzpatrickPalette())
        assertTrue(makeItemWithControl("color-palette").isFitzpatrickPalette())
        assertTrue(makeItemWithControl("fitzpatrick").isFitzpatrickPalette())
        assertFalse(makeItemWithControl("other").isFitzpatrickPalette())

        // Body map controls
        assertTrue(makeItemWithControl(SdcExtensions.ITEM_CONTROL_BODY_MAP).isBodyMap())
        assertFalse(makeItemWithControl("other").isBodyMap())

        // Segmented controls
        assertTrue(makeItemWithControl(SdcExtensions.ITEM_CONTROL_SEGMENTED_CONTROL).isSegmentedControl())
        assertTrue(makeItemWithControl("choice-cards").isSegmentedControl())
        assertFalse(makeItemWithControl("other").isSegmentedControl())
    }

    /**
     * Validates safe itemControl, initialExpression, min/max values, isHidden, and translations.
     */
    @Test
    fun testExhaustiveSdcExtensionsBranches() {
        // safeItemControl with Code, String, and unsupported type
        val codeItem =
            Questionnaire.Item(
                linkId = FhirString(value = "code_item"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.ITEM_CONTROL, value = Extension.Value.Code(Code(value = "radio")))),
            )
        assertEquals("radio", codeItem.safeItemControl.getOrNull())

        val unsupportedControlItem =
            Questionnaire.Item(
                linkId = FhirString(value = "unsupported_ctrl"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.ITEM_CONTROL, value = Extension.Value.Integer(Integer(value = 1)))),
            )
        assertNull(unsupportedControlItem.safeItemControl.getOrNull())

        // safeMinValue and safeMaxValue with Decimal, Integer, and unsupported type
        val decExt =
            Extension(
                url = SdcExtensions.MIN_VALUE,
                value =
                    Extension.Value.Decimal(
                        dev.ohs.fhir.model.r4
                            .Decimal(
                                value =
                                    dev.ohs.fhir.model.r4.FhirDecimal
                                        .fromString("1.5"),
                            ),
                    ),
            )
        val maxDecExt =
            Extension(
                url = SdcExtensions.MAX_VALUE,
                value =
                    Extension.Value.Decimal(
                        dev.ohs.fhir.model.r4
                            .Decimal(
                                value =
                                    dev.ohs.fhir.model.r4.FhirDecimal
                                        .fromString("9.5"),
                            ),
                    ),
            )
        val numItem =
            Questionnaire.Item(
                linkId = FhirString(value = "num_item"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension = listOf(decExt, maxDecExt),
            )
        assertEquals("1.5", numItem.getDecimalMinValue().getOrNull()?.toString())
        assertEquals("9.5", numItem.getDecimalMaxValue().getOrNull()?.toString())

        val unsuppMinItem =
            Questionnaire.Item(
                linkId = FhirString(value = "unsupp_min"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension = listOf(Extension(url = SdcExtensions.MIN_VALUE, value = Extension.Value.String(FhirString(value = "not-dec")))),
            )
        assertNull(unsuppMinItem.safeMinValue.getOrNull())
        assertNull(unsuppMinItem.safeMaxValue.getOrNull())

        // safeInitialExpression
        val exprChildExt =
            Extension(
                url = SdcExtensions.INITIAL_EXPRESSION,
                extension = listOf(Extension(url = "expression", value = Extension.Value.String(FhirString(value = "today()")))),
            )
        val exprChildItem =
            Questionnaire.Item(
                linkId = FhirString(value = "expr_child"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Date),
                extension = listOf(exprChildExt),
            )
        assertEquals("today()", exprChildItem.getInitialExpression())

        val exprDirectExt =
            Extension(
                url = SdcExtensions.INITIAL_EXPRESSION,
                value = Extension.Value.String(FhirString(value = "now()")),
            )
        val exprDirectItem =
            Questionnaire.Item(
                linkId = FhirString(value = "expr_direct"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.DateTime),
                extension = listOf(exprDirectExt),
            )
        assertEquals("now()", exprDirectItem.getInitialExpression())

        val exprUnsuppExt =
            Extension(
                url = SdcExtensions.INITIAL_EXPRESSION,
                value = Extension.Value.Integer(Integer(value = 42)),
            )
        val exprUnsuppItem =
            Questionnaire.Item(
                linkId = FhirString(value = "expr_unsupp"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                extension = listOf(exprUnsuppExt),
            )
        assertNull(exprUnsuppItem.getInitialExpression())

        val noInitItem =
            Questionnaire.Item(
                linkId = FhirString(value = "no_init"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        assertNull(noInitItem.safeInitialExpression.getOrNull())

        // isHidden with false and unsupported type
        val hiddenFalseItem =
            Questionnaire.Item(
                linkId = FhirString(value = "hf"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.HIDDEN, value = Extension.Value.Boolean(FhirBoolean(value = false)))),
            )
        assertFalse(hiddenFalseItem.isHidden())

        val hiddenUnsuppItem =
            Questionnaire.Item(
                linkId = FhirString(value = "hu"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.HIDDEN, value = Extension.Value.String(FhirString(value = "false")))),
            )
        assertFalse(hiddenUnsuppItem.isHidden())

        // safeUnit, safeItemWeight, and safeChoiceOrientation unsupported variants
        val unsuppGenericItem =
            Questionnaire.Item(
                linkId = FhirString(value = "ug"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.QUESTIONNAIRE_UNIT, value = Extension.Value.Integer(Integer(value = 1))),
                        Extension(url = SdcExtensions.ITEM_WEIGHT, value = Extension.Value.String(FhirString(value = "heavy"))),
                        Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.Integer(Integer(value = 2))),
                    ),
            )
        assertNull(unsuppGenericItem.safeUnit.getOrNull())
        assertNull(unsuppGenericItem.safeItemWeight.getOrNull())
        assertNull(unsuppGenericItem.safeChoiceOrientation.getOrNull())

        val codeOrientItem =
            Questionnaire.Item(
                linkId = FhirString(value = "co"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.Code(Code(value = "vertical"))),
                    ),
            )
        assertEquals("vertical", codeOrientItem.safeChoiceOrientation.getOrNull())

        // getLocalizedText with Code lang, non-matching lang, blank content, and null text
        val langCodeExt =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.Code(Code(value = "ja"))),
                        Extension(url = "content", value = Extension.Value.String(FhirString(value = "臨床ノート"))),
                    ),
            )
        val blankContentExt =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.String(FhirString(value = "es"))),
                        Extension(url = "content", value = Extension.Value.String(FhirString(value = "   "))),
                    ),
            )
        val jaItem =
            Questionnaire.Item(
                linkId = FhirString(value = "ja_q"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Fallback Text"),
                extension = listOf(langCodeExt, blankContentExt),
            )
        assertEquals("臨床ノート", jaItem.getLocalizedText("ja-JP"))
        assertEquals("Fallback Text", jaItem.getLocalizedText("es"))
        assertEquals("Fallback Text", jaItem.getLocalizedText("de"))
        assertEquals("Fallback Text", jaItem.getLocalizedText())

        val nullTextItem =
            Questionnaire.Item(
                linkId = FhirString(value = "nt_q"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        assertEquals("", nullTextItem.getLocalizedText("fr"))

        // getLocalizedTitle with direct extension using Code lang, title extension using String, blank content, and null title
        val qTitleTrans =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.Code(Code(value = "ja"))),
                        Extension(url = "content", value = Extension.Value.String(FhirString(value = "問診票"))),
                    ),
            )
        val qDirectBlankTrans =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.String(FhirString(value = "es"))),
                        Extension(url = "content", value = Extension.Value.String(FhirString(value = " "))),
                    ),
            )
        val qExhaustive =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = "Default Title", extension = listOf(qTitleTrans)),
                extension = listOf(qDirectBlankTrans),
            )
        assertEquals("問診票", qExhaustive.getLocalizedTitle("ja"))
        assertEquals("Default Title", qExhaustive.getLocalizedTitle("es"))
        assertEquals("Default Title", qExhaustive.getLocalizedTitle("en"))
        assertEquals("Default Title", qExhaustive.getLocalizedTitle())

        val qNullTitle =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
            )
        assertEquals("", qNullTitle.getLocalizedTitle("en"))
    }

    /**
     * Exercises remaining boundary branches across SdcExtensions.
     */
    @Test
    fun testRemainingSdcExtensionsBranches() {
        // Visual pain control with null coding.code and LOINC_PAIN_SCORE vs severity
        val itemWithNullCodeCoding =
            Questionnaire.Item(
                linkId = FhirString(value = "null_code_coding"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                code = listOf(Coding(code = null), Coding(code = Code(value = SdcExtensions.LOINC_PAIN_SCORE))),
            )
        assertTrue(itemWithNullCodeCoding.isVisualPainControl())

        // safeItemControl with empty coding, null code, null value
        val emptyCodingItem =
            Questionnaire.Item(
                linkId = FhirString(value = "empty_coding"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.ITEM_CONTROL,
                            value = Extension.Value.CodeableConcept(CodeableConcept(coding = emptyList())),
                        ),
                    ),
            )
        assertNull(emptyCodingItem.safeItemControl.getOrNull())

        val nullCodeItem =
            Questionnaire.Item(
                linkId = FhirString(value = "null_code"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.ITEM_CONTROL,
                            value = Extension.Value.CodeableConcept(CodeableConcept(coding = listOf(Coding(code = null)))),
                        ),
                    ),
            )
        assertNull(nullCodeItem.safeItemControl.getOrNull())

        val nullValueItem =
            Questionnaire.Item(
                linkId = FhirString(value = "null_val"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.ITEM_CONTROL,
                            value = Extension.Value.CodeableConcept(CodeableConcept(coding = listOf(Coding(code = Code(value = null))))),
                        ),
                    ),
            )
        assertNull(nullValueItem.safeItemControl.getOrNull())

        // safeMinValue and safeMaxValue with Integer(value = null)
        val nullIntMinItem =
            Questionnaire.Item(
                linkId = FhirString(value = "nim"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Integer),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.MIN_VALUE, value = Extension.Value.Integer(Integer(value = null))),
                        Extension(url = SdcExtensions.MAX_VALUE, value = Extension.Value.Integer(Integer(value = null))),
                    ),
            )
        assertNull(nullIntMinItem.safeMinValue.getOrNull())
        assertNull(nullIntMinItem.safeMaxValue.getOrNull())
        assertNull(nullIntMinItem.getMinValue())
        assertNull(nullIntMinItem.getMaxValue())

        // safeInitialExpression where child expression extension value is not String, but parent is String
        val initParentStringChildInt =
            Questionnaire.Item(
                linkId = FhirString(value = "ipsc"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.INITIAL_EXPRESSION,
                            value = Extension.Value.String(FhirString(value = "parent_expr")),
                            extension = listOf(Extension(url = "expression", value = Extension.Value.Integer(Integer(value = 1)))),
                        ),
                    ),
            )
        assertEquals("parent_expr", initParentStringChildInt.getInitialExpression())

        // isHidden with Boolean(value = null)
        val hiddenNullBoolItem =
            Questionnaire.Item(
                linkId = FhirString(value = "hnb"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.HIDDEN, value = Extension.Value.Boolean(FhirBoolean(value = null)))),
            )
        assertFalse(hiddenNullBoolItem.isHidden())

        // translations with non-string lang and non-string content
        val nonStringTrans =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.Integer(Integer(value = 99))),
                        Extension(url = "content", value = Extension.Value.Integer(Integer(value = 100))),
                    ),
            )
        val nonStringTransMatchLang =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.String(FhirString(value = "es"))),
                        Extension(url = "content", value = Extension.Value.Integer(Integer(value = 100))),
                    ),
            )
        val qItemWithNonString =
            Questionnaire.Item(
                linkId = FhirString(value = "ns_q"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "English Default"),
                extension = listOf(nonStringTrans, nonStringTransMatchLang),
            )
        assertEquals("English Default", qItemWithNonString.getLocalizedText("es"))

        val qWithNonString =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = "Default Title"),
                extension = listOf(nonStringTrans, nonStringTransMatchLang),
            )
        assertEquals("Default Title", qWithNonString.getLocalizedTitle("es"))

        // Null text and null title values
        val itemWithNullTextVal =
            Questionnaire.Item(
                linkId = FhirString(value = "ntv"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = null),
            )
        assertEquals("", itemWithNullTextVal.getLocalizedText("en"))

        val qWithNullTitleVal =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = null),
            )
        assertEquals("", qWithNullTitleVal.getLocalizedTitle("en"))

        // safeUnit, safeItemWeight, safeChoiceOrientation with other types
        val itemWithOtherUnit =
            Questionnaire.Item(
                linkId = FhirString(value = "iou"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension = listOf(Extension(url = SdcExtensions.QUESTIONNAIRE_UNIT, value = Extension.Value.Integer(Integer(value = 1)))),
            )
        assertNull(itemWithOtherUnit.safeUnit.getOrNull())

        val itemWithOtherWeight =
            Questionnaire.Item(
                linkId = FhirString(value = "iow"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension = listOf(Extension(url = SdcExtensions.ITEM_WEIGHT, value = Extension.Value.Integer(Integer(value = 1)))),
            )
        assertNull(itemWithOtherWeight.safeItemWeight.getOrNull())

        val itemWithOtherOrientation =
            Questionnaire.Item(
                linkId = FhirString(value = "ioo"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension = listOf(Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.Integer(Integer(value = 1)))),
            )
        assertNull(itemWithOtherOrientation.safeChoiceOrientation.getOrNull())

        val itemWithCodeOrientation =
            Questionnaire.Item(
                linkId = FhirString(value = "ico"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.Code(Code(value = "horizontal"))),
                    ),
            )
        assertEquals("horizontal", itemWithCodeOrientation.safeChoiceOrientation.getOrNull())

        val itemWithStringOrientation =
            Questionnaire.Item(
                linkId = FhirString(value = "iso"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.CHOICE_ORIENTATION, value = Extension.Value.String(FhirString(value = "vertical"))),
                    ),
            )
        assertEquals("vertical", itemWithStringOrientation.safeChoiceOrientation.getOrNull())

        val itemWithOtherInitExpr =
            Questionnaire.Item(
                linkId = FhirString(value = "ioie"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                extension = listOf(Extension(url = SdcExtensions.INITIAL_EXPRESSION, value = Extension.Value.Integer(Integer(value = 99)))),
            )
        assertNull(itemWithOtherInitExpr.getInitialExpression())

        // getMinValue and getMaxValue when safeMinValue / safeMaxValue are null
        val itemNoMinMax =
            Questionnaire.Item(
                linkId = FhirString(value = "nomm"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
            )
        assertNull(itemNoMinMax.getMinValue())
        assertNull(itemNoMinMax.getMaxValue())

        // Empty item accessors for safeUnit, safeItemWeight, safeChoiceOrientation, getItemControl, getInitialExpression
        val emptyItem =
            Questionnaire.Item(
                linkId = FhirString(value = "completely_empty"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            )
        assertNull(emptyItem.safeUnit.getOrNull())
        assertNull(emptyItem.safeItemWeight.getOrNull())
        assertNull(emptyItem.safeChoiceOrientation.getOrNull())
        assertNull(emptyItem.getItemControl())
        assertNull(emptyItem.getInitialExpression())
        assertNull(emptyItem.safeMinValue.getOrNull())
        assertNull(emptyItem.safeMaxValue.getOrNull())

        // Decimal min and max value extensions
        val itemWithDecimalMinMax =
            Questionnaire.Item(
                linkId = FhirString(value = "dmm"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension =
                    listOf(
                        Extension(
                            url = SdcExtensions.MIN_VALUE,
                            value = Extension.Value.Decimal(Decimal(value = FhirDecimal.fromString("1.5"))),
                        ),
                        Extension(
                            url = SdcExtensions.MAX_VALUE,
                            value = Extension.Value.Decimal(Decimal(value = FhirDecimal.fromString("99.5"))),
                        ),
                    ),
            )
        assertEquals(1.5f, itemWithDecimalMinMax.getMinValue())
        assertEquals(99.5f, itemWithDecimalMinMax.getMaxValue())
        assertNotNull(itemWithDecimalMinMax.getDecimalMinValue().getOrNull())
        assertNotNull(itemWithDecimalMinMax.getDecimalMaxValue().getOrNull())

        // Non-numeric (String) min and max extensions
        val itemWithStringMinMax =
            Questionnaire.Item(
                linkId = FhirString(value = "smm"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.Decimal),
                extension =
                    listOf(
                        Extension(url = SdcExtensions.MIN_VALUE, value = Extension.Value.String(FhirString(value = "nan"))),
                        Extension(url = SdcExtensions.MAX_VALUE, value = Extension.Value.String(FhirString(value = "nan"))),
                    ),
            )
        assertNull(itemWithStringMinMax.getMinValue())
        assertNull(itemWithStringMinMax.getMaxValue())
        assertNull(itemWithStringMinMax.safeMinValue.getOrNull())
        assertNull(itemWithStringMinMax.safeMaxValue.getOrNull())

        // Translation extension missing 'lang' child extension
        val transNoLang =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension = listOf(Extension(url = "content", value = Extension.Value.String(FhirString(value = "Cont")))),
            )
        // Translation extension with matching 'lang' but missing 'content' child extension
        val transNoContent =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension = listOf(Extension(url = "lang", value = Extension.Value.String(FhirString(value = "es")))),
            )
        val itemTransEdge =
            Questionnaire.Item(
                linkId = FhirString(value = "ite"),
                type = Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                text = FhirString(value = "Fallback Text"),
                extension = listOf(transNoLang, transNoContent),
            )
        assertEquals("Fallback Text", itemTransEdge.getLocalizedText("es"))

        // Questionnaire with translations on title itself (transNoLang and transNoContent placed first)
        val esTitleExt =
            Extension(
                url = SdcExtensions.TRANSLATION,
                extension =
                    listOf(
                        Extension(url = "lang", value = Extension.Value.String(FhirString(value = "es"))),
                        Extension(url = "content", value = Extension.Value.String(FhirString(value = "Título en Español"))),
                    ),
            )
        val qWithTitleTrans =
            Questionnaire(
                status = Enumeration(value = PublicationStatus.Active),
                title = FhirString(value = "English Title", extension = listOf(transNoLang, transNoContent, esTitleExt)),
            )
        assertEquals("Título en Español", qWithTitleTrans.getLocalizedTitle("es"))
    }
}
