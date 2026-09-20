/**
 * @file FhirSearchIndexerEngineExhaustiveTest.kt
 * Exhaustive unit tests for FhirSearchIndexerEngine and ResourceIndexingContext targeting 100% coverage.
 */

package io.healthplatform.chartcam.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.ohs.fhir.model.r4.Address
import dev.ohs.fhir.model.r4.Canonical
import dev.ohs.fhir.model.r4.Code
import dev.ohs.fhir.model.r4.CodeableConcept
import dev.ohs.fhir.model.r4.Coding
import dev.ohs.fhir.model.r4.Date
import dev.ohs.fhir.model.r4.DateTime
import dev.ohs.fhir.model.r4.Decimal
import dev.ohs.fhir.model.r4.Encounter
import dev.ohs.fhir.model.r4.Enumeration
import dev.ohs.fhir.model.r4.ExtensibleEnumeration
import dev.ohs.fhir.model.r4.FhirDate
import dev.ohs.fhir.model.r4.FhirDateTime
import dev.ohs.fhir.model.r4.FhirDecimal
import dev.ohs.fhir.model.r4.HumanName
import dev.ohs.fhir.model.r4.Identifier
import dev.ohs.fhir.model.r4.Meta
import dev.ohs.fhir.model.r4.Observation
import dev.ohs.fhir.model.r4.Patient
import dev.ohs.fhir.model.r4.Period
import dev.ohs.fhir.model.r4.Practitioner
import dev.ohs.fhir.model.r4.Quantity
import dev.ohs.fhir.model.r4.Reference
import dev.ohs.fhir.model.r4.Uri
import dev.ohs.fhir.model.r4.terminologies.AdministrativeGender
import dev.ohs.fhir.model.r4.terminologies.SearchParamType
import io.healthplatform.chartcam.database.ChartCamDatabase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.ohs.fhir.model.r4.Boolean as FhirBoolean
import dev.ohs.fhir.model.r4.Integer as FhirInteger
import dev.ohs.fhir.model.r4.String as FhirString

/**
 * Test enum with identical code and name.
 *
 * @property code Terminology code.
 * @property system Terminology system URI.
 * @property display Optional display name.
 */
enum class TestSameNameEnum(
    override val code: String,
    override val system: String,
    override val display: String? = null,
) : dev.ohs.fhir.model.r4.FhirEnum {
    SAME_CODE("SAME_CODE", "http://test"),
}

/**
 * Test class implementing FhirEnum but not Enum.
 *
 * @property code Terminology code.
 * @property system Terminology system URI.
 * @property display Optional display name.
 */
class TestNonEnumFhir(
    override val code: String,
    override val system: String,
    override val display: String? = null,
) : dev.ohs.fhir.model.r4.FhirEnum

/**
 * Test class verifying all internal branches of [FhirSearchIndexerEngine].
 */
class FhirSearchIndexerEngineExhaustiveTest {
    private lateinit var db: ChartCamDatabase
    private lateinit var ctx: ResourceIndexingContext

    /**
     * Set up in-memory database and indexing context.
     */
    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        ChartCamDatabase.Schema.synchronous().create(driver)
        db = ChartCamDatabase(driver)
        ctx = ResourceIndexingContext(db.chartCamQueries, "Patient", "test-patient-1")
    }

    private suspend fun seedResources() {
        db.chartCamQueries.insertResource("test-patient-1", "Patient", "{}", "2026-01-01")
        db.chartCamQueries.insertResource("p-meta", "Patient", "{}", "2026-01-01")
        db.chartCamQueries.insertResource("obs-1", "Observation", "{}", "2026-01-01")
    }

    /**
     * Tests indexing fallback types (Uri, Composite, Special).
     */
    @Test
    fun testIndexFallbackTypes() =
        runTest {
            seedResources()
            ctx.indexValue("uri-param", SearchParamType.Uri, "http://example.com/fhir")
            ctx.indexValue("comp-param", SearchParamType.Composite, "code$123")
            ctx.indexValue("spec-param", SearchParamType.Special, "special-value")

            val tokens = db.chartCamQueries.searchResourcesByToken("Patient", "uri-param", null, "http://example.com/fhir").awaitAsList()
            assertEquals(1, tokens.size)

            val compTokens = db.chartCamQueries.searchResourcesByToken("Patient", "comp-param", null, "code$123").awaitAsList()
            assertEquals(1, compTokens.size)

            val specTokens = db.chartCamQueries.searchResourcesByToken("Patient", "spec-param", null, "special-value").awaitAsList()
            assertEquals(1, specTokens.size)
        }

    /**
     * Tests indexing numeric values with all variants (FhirInteger, FhirDecimal, Number, String).
     */
    @Test
    fun testIndexNumberVariants() =
        runTest {
            seedResources()
            ctx.indexValue("int-param", SearchParamType.Number, FhirInteger(value = 42))
            ctx.indexValue("int-null", SearchParamType.Number, FhirInteger(value = null))
            ctx.indexValue("dec-param", SearchParamType.Number, FhirDecimal.fromString("10.5"))
            ctx.indexValue("num-param", SearchParamType.Number, 100L)
            ctx.indexValue("str-param", SearchParamType.Number, "250")

            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "int-param", null, "42")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "dec-param", null, "10.5")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "num-param", null, "100")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "str-param", null, "250")
                    .awaitAsList()
                    .size,
            )
        }

    /**
     * Tests indexing address components and empty addresses.
     */
    @Test
    fun testIndexAddressBranches() =
        runTest {
            seedResources()
            val addr =
                Address(
                    line = listOf(FhirString(value = "742 Evergreen Terrace"), FhirString(value = null)),
                    city = FhirString(value = "Springfield"),
                )
            ctx.indexValue("address", SearchParamType.String, addr)

            val matches =
                db.chartCamQueries
                    .searchResourcesByString(
                        "Patient",
                        "address",
                        "742 Evergreen Terrace Springfield",
                    ).awaitAsList()
            assertEquals(1, matches.size)

            val emptyAddr = Address(line = emptyList(), city = null)
            ctx.indexValue("address-empty", SearchParamType.String, emptyAddr)
            assertEquals(
                0,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "address-empty", "")
                    .awaitAsList()
                    .size,
            )

            val cityOnly = Address(line = emptyList(), city = FhirString(value = "Boston"))
            ctx.indexValue("address-city-only", SearchParamType.String, cityOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "address-city-only", "Boston")
                    .awaitAsList()
                    .size,
            )

            val lineOnly = Address(line = listOf(FhirString(value = "Main Street")), city = null)
            ctx.indexValue("address-line-only", SearchParamType.String, lineOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "address-line-only", "Main Street")
                    .awaitAsList()
                    .size,
            )

            val cityNullVal = Address(line = emptyList(), city = FhirString(value = null))
            ctx.indexValue("address-city-null-val", SearchParamType.String, cityNullVal)
        }

    /**
     * Tests string indexing variants including HumanName and FhirString.
     */
    @Test
    fun testIndexStringVariants() =
        runTest {
            seedResources()
            ctx.indexValue("raw-str", SearchParamType.String, FhirString(value = "SimpleText"))
            ctx.indexValue("raw-str", SearchParamType.String, FhirString(value = "SimpleText"))
            ctx.indexValue("null-str", SearchParamType.String, FhirString(value = null))
            ctx.indexValue("scalar-str", SearchParamType.String, "Scalar")

            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "raw-str", "SimpleText")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "scalar-str", "Scalar")
                    .awaitAsList()
                    .size,
            )

            // HumanName variants
            val nameFamilyOnly = HumanName(family = FhirString(value = "Jones"))
            ctx.indexValue("name-family-only", SearchParamType.String, nameFamilyOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "family", "Jones")
                    .awaitAsList()
                    .size,
            )

            val nameGivenOnly = HumanName(given = listOf(FhirString(value = "Bob")))
            ctx.indexValue("name-given-only", SearchParamType.String, nameGivenOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "given", "Bob")
                    .awaitAsList()
                    .size,
            )

            val emptyName = HumanName()
            ctx.indexValue("name-empty", SearchParamType.String, emptyName)
        }

    /**
     * Tests indexing quantity with units, missing value, and fallback.
     */
    @Test
    fun testIndexQuantityBranches() =
        runTest {
            seedResources()
            val quantWithSysAndUnit =
                Quantity(
                    value = Decimal(value = FhirDecimal.fromString("120")),
                    system = Uri(value = "http://unitsofmeasure.org"),
                    code = Code(value = "mm[Hg]"),
                )
            ctx.indexValue("systolic", SearchParamType.Quantity, quantWithSysAndUnit)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "systolic", "http://unitsofmeasure.org", "120")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken(
                        "Patient",
                        "systolic",
                        "http://unitsofmeasure.org",
                        "120|mm[Hg]",
                    ).awaitAsList()
                    .size,
            )

            val quantUnitOnly =
                Quantity(
                    value = Decimal(value = FhirDecimal.fromString("80")),
                    unit = FhirString(value = "kg"),
                )
            ctx.indexValue("weight", SearchParamType.Quantity, quantUnitOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "weight", null, "80")
                    .awaitAsList()
                    .size,
            )

            val quantNoValue = Quantity(value = null)
            ctx.indexValue("empty-quant", SearchParamType.Quantity, quantNoValue)

            val quantWithNullDecimal = Quantity(value = Decimal(value = null))
            ctx.indexValue("quant-null-dec", SearchParamType.Quantity, quantWithNullDecimal)

            val quantUnitFallback =
                Quantity(
                    value = Decimal(value = FhirDecimal.fromString("10")),
                    code = Code(value = null),
                    unit = FhirString(value = "g"),
                )
            ctx.indexValue("quant-unit-fallback", SearchParamType.Quantity, quantUnitFallback)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "quant-unit-fallback", null, "10|g")
                    .awaitAsList()
                    .size,
            )

            val quantNoUnitOrCode =
                Quantity(
                    value = Decimal(value = FhirDecimal.fromString("50")),
                )
            ctx.indexValue("simple-quant", SearchParamType.Quantity, quantNoUnitOrCode)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "simple-quant", null, "50")
                    .awaitAsList()
                    .size,
            )

            val quantWithNullUnitAndCode =
                Quantity(
                    value = Decimal(value = FhirDecimal.fromString("100")),
                    code = Code(value = null),
                    unit = FhirString(value = null),
                )
            ctx.indexValue("quant-no-unit", SearchParamType.Quantity, quantWithNullUnitAndCode)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "quant-no-unit", null, "100")
                    .awaitAsList()
                    .size,
            )

            // Quantity fallback
            ctx.indexValue("quant-fallback", SearchParamType.Quantity, "raw-quantity")
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "quant-fallback", null, "raw-quantity")
                    .awaitAsList()
                    .size,
            )
        }

    /**
     * Tests cross-referencing, target type routing, and bare reference branches.
     */
    @Test
    fun testIndexReferenceAndTargetBranches() =
        runTest {
            seedResources()
            val anonResource =
                object : dev.ohs.fhir.model.r4.Resource() {
                    override val id: String = "anon-id"
                    override val meta: dev.ohs.fhir.model.r4.Meta? = null
                    override val implicitRules: dev.ohs.fhir.model.r4.Uri? = null
                    override val language: dev.ohs.fhir.model.r4.Code? = null

                    override fun toBuilder(): dev.ohs.fhir.model.r4.Resource.Builder = Patient.Builder()
                }

            // Target classes routing
            ctx.indexValue(
                "subject",
                SearchParamType.Reference,
                Reference(reference = FhirString(value = "Patient/p-100")),
                listOf(Patient::class, Encounter::class, Practitioner::class, Observation::class, anonResource::class),
            )

            // Target cross references for Encounter, Patient, and Observation
            ctx.indexValue("target", SearchParamType.Reference, "Encounter/enc-99")
            ctx.indexValue("target", SearchParamType.Reference, "Patient/pat-88")
            ctx.indexValue("target", SearchParamType.Reference, "Observation/obs-1")
            ctx.indexValue("practitioner", SearchParamType.Reference, "pr-77")
            ctx.indexValue("practitioner", SearchParamType.Reference, "Practitioner/pr-88")
            ctx.indexValue("encounter", SearchParamType.Reference, "enc-77")
            ctx.indexValue("encounter", SearchParamType.Reference, "Encounter/enc-88")
            ctx.indexValue("patient", SearchParamType.Reference, "pat-77")
            ctx.indexValue("patient", SearchParamType.Reference, "Patient/pat-77")

            // Bare reference with no slash
            ctx.indexValue("bare-ref", SearchParamType.Reference, "raw-bare-id")
            // Duplicate reference to hit deduplication false branch
            ctx.indexValue("bare-ref", SearchParamType.Reference, "raw-bare-id")

            // Canonical reference
            ctx.indexValue("canonical-ref", SearchParamType.Reference, Canonical(value = "http://example.com/q1"))

            // Null reference, reference with null value, and Blank reference
            ctx.indexValue("null-ref", SearchParamType.Reference, Reference(reference = null))
            ctx.indexValue("null-ref-val", SearchParamType.Reference, Reference(reference = FhirString(value = null)))
            ctx.indexValue("blank-ref", SearchParamType.Reference, Reference(reference = FhirString(value = "   ")))

            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "target", "Encounter/enc-99")
                    .awaitAsList()
                    .isNotEmpty(),
            )
            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "encounter", "Encounter/enc-99")
                    .awaitAsList()
                    .isNotEmpty(),
            )
            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "patient", "Patient/pat-88")
                    .awaitAsList()
                    .isNotEmpty(),
            )
            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "target", "Observation/obs-1")
                    .awaitAsList()
                    .isNotEmpty(),
            )
            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference(
                        "Patient",
                        "canonical-ref",
                        "http://example.com/q1",
                    ).awaitAsList()
                    .isNotEmpty(),
            )
            assertTrue(
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "bare-ref", "raw-bare-id")
                    .awaitAsList()
                    .isNotEmpty(),
            )
        }

    /**
     * Tests period with end date, timing, and date fallback.
     */
    @Test
    fun testIndexDateVariants() =
        runTest {
            seedResources()
            val period =
                Period(
                    start = DateTime(value = FhirDateTime.fromString("2026-01-01T00:00:00Z")),
                    end = DateTime(value = FhirDateTime.fromString("2026-01-02T00:00:00Z")),
                )
            ctx.indexValue("period-param", SearchParamType.Date, period)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "period-param", "2026-01-01T00:00:00Z")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "period-param", "2026-01-02T00:00:00Z")
                    .awaitAsList()
                    .size,
            )

            val periodStartOnly =
                Period(
                    start = DateTime(value = FhirDateTime.fromString("2026-01-05T00:00:00Z")),
                    end = null,
                )
            ctx.indexValue("period-start-only", SearchParamType.Date, periodStartOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "period-start-only", "2026-01-05T00:00:00Z")
                    .awaitAsList()
                    .size,
            )

            val periodEndOnly =
                Period(
                    start = null,
                    end = DateTime(value = FhirDateTime.fromString("2026-01-06T00:00:00Z")),
                )
            ctx.indexValue("period-end-only", SearchParamType.Date, periodEndOnly)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "period-end-only", "2026-01-06T00:00:00Z")
                    .awaitAsList()
                    .size,
            )

            val periodEmpty = Period(start = null, end = null)
            ctx.indexValue("period-empty", SearchParamType.Date, periodEmpty)

            val periodNullValues =
                Period(
                    start = DateTime(value = null),
                    end = DateTime(value = null),
                )
            ctx.indexValue("period-nulls", SearchParamType.Date, periodNullValues)

            ctx.indexValue("date-obj", SearchParamType.Date, Date(value = FhirDate.fromString("2026-02-01")))
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "date-obj", "2026-02-01")
                    .awaitAsList()
                    .size,
            )
            ctx.indexValue("date-null", SearchParamType.Date, Date(value = null))

            ctx.indexValue("datetime-obj", SearchParamType.Date, DateTime(value = FhirDateTime.fromString("2026-03-01T12:00:00Z")))
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "datetime-obj", "2026-03-01T12:00:00Z")
                    .awaitAsList()
                    .size,
            )
            ctx.indexValue("datetime-null", SearchParamType.Date, DateTime(value = null))

            ctx.indexValue(
                "instant-obj",
                SearchParamType.Date,
                dev.ohs.fhir.model.r4
                    .Instant(value = FhirDateTime.fromString("2026-04-01T12:00:00Z")),
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "instant-obj", "2026-04-01T12:00:00Z")
                    .awaitAsList()
                    .size,
            )
            ctx.indexValue(
                "instant-null",
                SearchParamType.Date,
                dev.ohs.fhir.model.r4
                    .Instant(value = null),
            )

            ctx.indexValue("fhirdate-obj", SearchParamType.Date, FhirDate.fromString("2026-05-01"))
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "fhirdate-obj", "2026-05-01")
                    .awaitAsList()
                    .size,
            )

            ctx.indexValue("fhirdatetime-obj", SearchParamType.Date, FhirDateTime.fromString("2026-06-01T12:00:00Z"))
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "fhirdatetime-obj", "2026-06-01T12:00:00Z")
                    .awaitAsList()
                    .size,
            )

            ctx.indexValue("raw-date", SearchParamType.Date, "2026-07-10")
            ctx.indexValue("raw-date", SearchParamType.Date, "2026-07-10")
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByDate("Patient", "raw-date", "2026-07-10")
                    .awaitAsList()
                    .size,
            )
            ctx.indexValue("blank-date", SearchParamType.Date, "   ")
        }

    /**
     * Tests extensible enumeration, boolean, and other tokens.
     */
    @Test
    fun testIndexTokenVariants() =
        runTest {
            seedResources()
            val customExtEnum = ExtensibleEnumeration.Custom("other-gender")
            ctx.indexValue("ext-enum", SearchParamType.Token, customExtEnum)

            val emptyExtEnum = ExtensibleEnumeration.Custom("")
            ctx.indexValue("ext-empty", SearchParamType.Token, emptyExtEnum)

            val predefinedExtEnum = ExtensibleEnumeration.Predefined(AdministrativeGender.Male)
            ctx.indexValue("ext-predefined", SearchParamType.Token, predefinedExtEnum)

            ctx.indexValue("bool-token", SearchParamType.Token, FhirBoolean(value = true))
            ctx.indexValue("code-token", SearchParamType.Token, Code(value = "code-123"))
            ctx.indexValue("code-token", SearchParamType.Token, Code(value = "code-123"))
            ctx.indexValue("code-token-null", SearchParamType.Token, Code(value = null))
            ctx.indexValue("bool-token-null", SearchParamType.Token, FhirBoolean(value = null))
            ctx.indexValue("other-token", SearchParamType.Token, "custom-scalar")

            val enumWithVal = Enumeration(value = AdministrativeGender.Female)
            ctx.indexValue("enum-token", SearchParamType.Token, enumWithVal)

            val sameNameEnum = Enumeration(value = TestSameNameEnum.SAME_CODE)
            ctx.indexValue("enum-same-name", SearchParamType.Token, sameNameEnum)

            val nonEnumFhir = Enumeration(value = TestNonEnumFhir("non-enum", "http://test"))
            ctx.indexValue("enum-non-enum", SearchParamType.Token, nonEnumFhir)

            val enumNull = Enumeration<AdministrativeGender>(value = null)
            ctx.indexValue("enum-null", SearchParamType.Token, enumNull)

            val codeable =
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(code = Code(value = "C1"), system = Uri(value = "http://sys1")),
                            Coding(code = Code(value = "C2"), system = null),
                            Coding(code = Code(value = null), system = null),
                            Coding(code = Code(value = "   "), system = null),
                        ),
                )
            ctx.indexValue("codeable", SearchParamType.Token, codeable)

            // Identifiers
            ctx.indexValue("id-null-val", SearchParamType.Token, Identifier(value = null))
            ctx.indexValue("id-blank-val", SearchParamType.Token, Identifier(value = FhirString(value = "  ")))
            ctx.indexValue(
                "id-valid",
                SearchParamType.Token,
                Identifier(value = FhirString(value = "MRN-1"), system = Uri(value = "http://sys")),
            )
            ctx.indexValue("id-patient-nosys", SearchParamType.Token, Identifier(value = FhirString(value = "MRN-NOSYS"), system = null))

            val obsCtx = ResourceIndexingContext(db.chartCamQueries, "Observation", "obs-1")
            obsCtx.indexValue("id-obs", SearchParamType.Token, Identifier(value = FhirString(value = "OBS-ID-1"), system = null))

            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "ext-enum", null, "other-gender")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "bool-token", null, "true")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "code-token", null, "code-123")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "other-token", null, "custom-scalar")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "codeable", "http://sys1", "C1")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "codeable", null, "C2")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "id-valid", "http://sys", "MRN-1")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Observation", "id-obs", null, "OBS-ID-1")
                    .awaitAsList()
                    .size,
            )
        }

    /**
     * Tests base resource metadata indexing including security, tag, profile, and id.
     */
    @Test
    fun testIndexUniversalBaseParameters() =
        runTest {
            seedResources()
            val patientWithMeta =
                Patient(
                    id = "p-meta",
                    meta =
                        Meta(
                            lastUpdated =
                                dev.ohs.fhir.model.r4
                                    .Instant(value = FhirDateTime.fromString("2026-09-18T10:00:00Z")),
                            security = listOf(Coding(code = Code(value = "RESTRICTED")), Coding(code = null)),
                            tag = listOf(Coding(code = Code(value = "CLINICAL_TRIAL")), Coding(code = null)),
                            profile = listOf(Canonical(value = "http://example.com/Profile"), Canonical(value = null)),
                        ),
                )

            val res = FhirSearchIndexerEngine.indexResourceGeneric(db.chartCamQueries, patientWithMeta, "Patient", "p-meta", emptyList())
            assertTrue(res.isSuccess)

            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "_security", null, "RESTRICTED")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "_tag", null, "CLINICAL_TRIAL")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByReference("Patient", "_profile", "http://example.com/Profile")
                    .awaitAsList()
                    .size,
            )
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByToken("Patient", "_id", null, "p-meta")
                    .awaitAsList()
                    .size,
            )

            // Resource with null ID and null meta
            val patientNoMeta = Patient(id = null, meta = null)
            val res2 = FhirSearchIndexerEngine.indexResourceGeneric(db.chartCamQueries, patientNoMeta, "Patient", "p-nometa", emptyList())
            assertTrue(res2.isSuccess)

            // Resource with empty meta
            val patientEmptyMeta =
                Patient(
                    id = "p-empty-meta",
                    meta = Meta(lastUpdated = null, security = emptyList(), tag = emptyList(), profile = emptyList()),
                )
            val res3 =
                FhirSearchIndexerEngine.indexResourceGeneric(
                    db.chartCamQueries,
                    patientEmptyMeta,
                    "Patient",
                    "p-empty-meta",
                    emptyList(),
                )
            assertTrue(res3.isSuccess)

            // Custom SearchParam with throwing extractor and null-returning extractor
            val throwingParam =
                dev.ohs.fhir.model.r4.search.SearchParam<Patient, Any>("failing", SearchParamType.String, "failing", emptyList()) {
                    throw IllegalStateException("extraction failure") // allow-exception
                }
            val nullElemParam =
                dev.ohs.fhir.model.r4.search.SearchParam<Patient, Any?>("null-elem", SearchParamType.String, "null-elem", emptyList()) {
                    listOf(null, "valid-val")
                }
            val resCustom =
                FhirSearchIndexerEngine.indexResourceGeneric(
                    db.chartCamQueries,
                    patientWithMeta,
                    "Patient",
                    "p-meta",
                    listOf(throwingParam, nullElemParam),
                )
            assertTrue(resCustom.isSuccess)
            assertEquals(
                1,
                db.chartCamQueries
                    .searchResourcesByString("Patient", "null-elem", "valid-val")
                    .awaitAsList()
                    .size,
            )
        }
}
