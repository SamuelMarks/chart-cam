/**
 * @file QuestionnaireBuilderViewModelJvmTest.kt
 * Contains declarations for QuestionnaireBuilderViewModelJvmTest.kt.
 */
package io.healthplatform.chartcam.viewmodel

import dev.ohs.fhir.model.r4.Questionnaire
import io.healthplatform.chartcam.repository.QuestionnaireRepository
import io.healthplatform.chartcam.repository.QuestionnaireSharingService
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class QuestionnaireBuilderViewModelJvmTest.
 */
class QuestionnaireBuilderViewModelJvmTest {
    /**
     * Test testJsonSerializationToUiState.
     */
    @Test
    fun testJsonSerializationToUiState() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Serialization Test")
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        val linkId1 =
            viewModel.state.value.items
                .first()
                .linkId
        viewModel.updateItem(linkId1, "Text Field", emptyList())

        viewModel.addItem(WidgetType.MULTI_SELECT)
        val linkId2 =
            viewModel.state.value.items
                .last()
                .linkId
        viewModel.updateItem(linkId2, "Choice Field", listOf("A", "B"))

        val builtQuestionnaire = viewModel.buildQuestionnaire()

        // 1. Serialize to JSON
        val sharingService = QuestionnaireSharingService()
        val jsonStr = sharingService.serializeQuestionnaire(builtQuestionnaire).getOrThrow()

        // 2. Deserialize from JSON
        val deserializedQuestionnaire = sharingService.deserializeQuestionnaire(jsonStr).getOrThrow()

        // 3. Save to repo manually to load it into a new ViewModel
        repo.saveQuestionnaire(deserializedQuestionnaire)

        // 4. Load into a new ViewModel (simulating accurate UI state reconstruction)
        val newViewModel = QuestionnaireBuilderViewModel(repo, duplicateFromId = deserializedQuestionnaire.id)
        val state = newViewModel.state.value

        assertEquals("Serialization Test (Copy)", state.title)
        assertEquals(2, state.items.size)

        val item1 = state.items[0]
        assertEquals("Text Field", item1.label)
        assertEquals(WidgetType.SINGLE_LINE_TEXT, item1.widgetType)

        val item2 = state.items[1]
        assertEquals("Choice Field", item2.label)
        assertEquals(WidgetType.MULTI_SELECT, item2.widgetType)
        assertEquals(listOf("A", "B"), item2.options)
    }

    /**
     * Test testInitialState.
     */
    @Test
    fun testInitialState() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        val state = viewModel.state.value
        assertEquals("", state.title)
        assertTrue(state.items.isEmpty())
        assertFalse(state.isPreviewMode)
    }

    /**
     * Test testUpdateTitle.
     */
    @Test
    fun testUpdateTitle() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("New Form")
        assertEquals("New Form", viewModel.state.value.title)
    }

    /**
     * Test testAddAndRemoveItem.
     */
    @Test
    fun testAddAndRemoveItem() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        assertEquals(1, viewModel.state.value.items.size)

        val item =
            viewModel.state.value.items
                .first()
        assertEquals("New SINGLE_LINE_TEXT Item", item.label)
        assertEquals(WidgetType.SINGLE_LINE_TEXT, item.widgetType)

        viewModel.removeItem(item.linkId)
        assertTrue(
            viewModel.state.value.items
                .isEmpty(),
        )
    }

    /**
     * Test updateItemLabelAndOptions modifies target item and preserves other items.
     */
    @Test
    fun testUpdateItemLabelAndOptions() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        viewModel.addItem(WidgetType.SINGLE_SELECT)
        val item1 = viewModel.state.value.items[0]
        val item2 = viewModel.state.value.items[1]

        viewModel.updateItem(item2.linkId, "Updated Label", listOf("Opt1", "Opt2"))
        val updated2 =
            viewModel.state.value.items
                .first { it.linkId == item2.linkId }
        val preserved1 =
            viewModel.state.value.items
                .first { it.linkId == item1.linkId }

        assertEquals("Updated Label", updated2.label)
        assertEquals(listOf("Opt1", "Opt2"), updated2.options)
        assertEquals("New SINGLE_LINE_TEXT Item", preserved1.label)

        viewModel.addItem(WidgetType.SEGMENTED_TILES)
        val itemTiles =
            viewModel.state.value.items
                .last()
        viewModel.updateItem(itemTiles.linkId, "Tiles", emptyList())
        val updatedTiles =
            viewModel.state.value.items
                .first { it.linkId == itemTiles.linkId }
        assertTrue(updatedTiles.isError)
    }

    /**
     * Test that removeItem purges dangling enableWhen conditions referencing the removed item.
     */
    @Test
    fun testRemoveItemPurgesDanglingEnableWhen() {
        val repo = QuestionnaireRepository()
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SWITCH)
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        val item1 = viewModel.state.value.items[0]
        val item2 = viewModel.state.value.items[1]

        // Add enableWhen condition to item2 referencing item1
        val condition =
            BuilderEnableWhen(
                question = item1.linkId,
                operator = Questionnaire.QuestionnaireItemOperator.Exists,
                answerBoolean = true,
            )
        viewModel.updateItemEnableWhen(item2.linkId, listOf(condition))

        assertEquals(
            1,
            viewModel.state.value.items[1]
                .enableWhen.size,
        )

        // Remove item1
        val removeResult = viewModel.removeItem(item1.linkId)
        assertTrue(removeResult.isSuccess)

        assertEquals(1, viewModel.state.value.items.size)
        assertEquals(
            item2.linkId,
            viewModel.state.value.items[0]
                .linkId,
        )
        assertEquals(
            0,
            viewModel.state.value.items[0]
                .enableWhen.size,
            "Dangling enableWhen pointing to removed item must be purged",
        )
    }

    /**
     * Test testUpdateItem.
     */
    @Test
    fun testUpdateItem() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.MULTI_SELECT)
        val linkId =
            viewModel.state.value.items
                .first()
                .linkId

        viewModel.updateItem(linkId, "Updated Label", listOf("OptA", "OptB"))

        val updatedItem =
            viewModel.state.value.items
                .first()
        assertEquals("Updated Label", updatedItem.label)
        assertEquals(listOf("OptA", "OptB"), updatedItem.options)
    }

    /**
     * Test testMoveItemUpAndDown.
     */
    @Test
    fun testMoveItemUpAndDown() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        viewModel.addItem(WidgetType.MULTI_LINE_TEXT)
        viewModel.addItem(WidgetType.DATE)

        val itemsBefore = viewModel.state.value.items
        assertEquals(3, itemsBefore.size)
        val id1 = itemsBefore[0].linkId
        val id2 = itemsBefore[1].linkId
        val id3 = itemsBefore[2].linkId

        // Move id2 up
        viewModel.moveItemUp(id2)
        var itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id1, itemsAfter[1].linkId)
        assertEquals(id3, itemsAfter[2].linkId)

        // Move id2 up again (should be no-op as it's already at top)
        viewModel.moveItemUp(id2)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id1, itemsAfter[1].linkId)
        assertEquals(id3, itemsAfter[2].linkId)

        // Move id1 down
        viewModel.moveItemDown(id1)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id3, itemsAfter[1].linkId)
        assertEquals(id1, itemsAfter[2].linkId)

        // Move id1 down again (should be no-op as it's already at bottom)
        viewModel.moveItemDown(id1)
        itemsAfter = viewModel.state.value.items
        assertEquals(id2, itemsAfter[0].linkId)
        assertEquals(id3, itemsAfter[1].linkId)
        assertEquals(id1, itemsAfter[2].linkId)
    }

    /**
     * Test testTogglePreviewMode.
     */
    @Test
    fun testTogglePreviewMode() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        assertFalse(viewModel.state.value.isPreviewMode)
        viewModel.togglePreviewMode()
        assertTrue(viewModel.state.value.isPreviewMode)
    }

    /**
     * Test testSaveQuestionnaire.
     */
    @Test
    fun testSaveQuestionnaire() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Test Builder Form")
        viewModel.addItem(WidgetType.PHOTO_CAMERA)
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)

        viewModel.saveQuestionnaire()

        val savedForms = repo.getAvailableQuestionnaires()
        // ID should be "custom-test-builder-form"
        val form = savedForms.find { it.id == "custom-test-builder-form" }
        assertNotNull(form)
        assertEquals("Test Builder Form", form.title?.value)
        assertEquals(2, form.item.size)
    }

    /**
     * Test testSingleSelectExtensionMapping.
     */
    @Test
    fun testSingleSelectExtensionMapping() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Single Select Test")
        viewModel.addItem(WidgetType.SINGLE_SELECT)

        val linkId =
            viewModel.state.value.items
                .first()
                .linkId
        viewModel.updateItem(linkId, "Select One", listOf("Option 1", "Option 2"))

        viewModel.saveQuestionnaire()

        val form = repo.getAvailableQuestionnaires().find { it.id == "custom-single-select-test" }
        assertNotNull(form)

        val item = form.item.firstOrNull()
        assertNotNull(item)

        val ext = item.extension.find { it.url == "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl" }
        assertNotNull(ext)

        val code =
            ext
                .value
                ?.asCodeableConcept()
                ?.value
                ?.coding
                ?.firstOrNull()
                ?.code
                ?.value
        assertEquals("check-box", code)
    }

    /**
     * Test testDuplicateLinkIdValidation.
     */
    @Test
    fun testDuplicateLinkIdValidation() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Duplicate LinkId Test")

        // Add two items
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)

        // At first they are different, should be valid
        assertTrue(viewModel.validate().isSuccess)

        val questionnaire = viewModel.buildQuestionnaire()
        // Duplicate the linkId in the FHIR object manually for validation check
        val duplicateQuestionnaire =
            Questionnaire
                .Builder(status = questionnaire.status)
                .apply {
                    title = questionnaire.title?.toBuilder()
                    val dupItemBuilder =
                        Questionnaire.Item.Builder(
                            linkId =
                                dev.ohs.fhir.model.r4.String
                                    .Builder()
                                    .apply { value = "dup_id" },
                            type = questionnaire.item[0].type,
                        )
                    item.add(dupItemBuilder)
                    item.add(dupItemBuilder)
                }.build()
        assertTrue(
            io.healthplatform.chartcam.validation.FhirValidator
                .validate(duplicateQuestionnaire)
                .isFailure,
        )
    }

    /**
     * Test testChoiceOptionsValidation.
     */
    @Test
    fun testChoiceOptionsValidation() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Validation Test")
        viewModel.addItem(WidgetType.SINGLE_SELECT)

        val linkId =
            viewModel.state.value.items
                .first()
                .linkId

        // Initial state, no options, should be error
        viewModel.updateItem(linkId, "Select One", emptyList())
        assertTrue(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertTrue(viewModel.validate().isFailure)

        // Add options, error should clear
        viewModel.updateItem(linkId, "Select One", listOf("Opt1"))
        assertFalse(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertTrue(viewModel.validate().isSuccess)

        // Remove options again
        viewModel.updateItem(linkId, "Select One", emptyList())
        assertTrue(
            viewModel.state.value.items
                .first()
                .isError,
        )
        assertTrue(viewModel.validate().isFailure)
    }

    /**
     * Test testDuplicateFromId.
     */
    @Test
    fun testDuplicateFromId() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

        // 1. Create a source questionnaire
        val sourceViewModel = QuestionnaireBuilderViewModel(repo)
        sourceViewModel.updateTitle("Source Form")
        sourceViewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        sourceViewModel.addItem(WidgetType.MULTI_SELECT)

        // Fix the multi select item so it's valid
        val items = sourceViewModel.state.value.items
        val multiSelectId = items.first { it.widgetType == WidgetType.MULTI_SELECT }.linkId
        sourceViewModel.updateItem(multiSelectId, "Label", listOf("Opt1", "Opt2"))

        val sourceId = sourceViewModel.saveQuestionnaire()
        assertTrue(sourceId != null)

        // 2. Duplicate it
        val dupViewModel = QuestionnaireBuilderViewModel(repo, duplicateFromId = sourceId)
        val state = dupViewModel.state.value

        assertEquals("Source Form (Copy)", state.title)
        assertEquals(2, state.items.size)

        val item1 = state.items[0]
        assertEquals(WidgetType.SINGLE_LINE_TEXT, item1.widgetType)

        val item2 = state.items[1]
        assertEquals(WidgetType.MULTI_SELECT, item2.widgetType)
        assertEquals(listOf("Opt1", "Opt2"), item2.options)

        // 3. Ensure nextItemId is correctly set
        dupViewModel.addItem(WidgetType.PHOTO_CAMERA)
        val newItem =
            dupViewModel.state.value.items
                .last()
        assertTrue(newItem.linkId.startsWith("item_"))
        val idNum = newItem.linkId.removePrefix("item_").toInt()
        assertTrue(idNum > 2)
    }

    /**
     * Test testAllWidgetTypesMapping.
     */
    @Test
    fun testAllWidgetTypesMapping() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("All Widgets Test")
        WidgetType.values().forEach { widgetType ->
            viewModel.addItem(widgetType)
            val addedItem =
                viewModel.state.value.items
                    .last()

            // Fix validation for choice items
            if (widgetType == WidgetType.SINGLE_SELECT || widgetType == WidgetType.MULTI_SELECT) {
                viewModel.updateItem(addedItem.linkId, addedItem.label, listOf("OptA", "OptB"))
            }
        }

        val savedId = viewModel.saveQuestionnaire()
        assertTrue(savedId != null, "Should be valid and saved")

        // Duplicate to test the reverse mapping
        val dupViewModel = QuestionnaireBuilderViewModel(repo, duplicateFromId = savedId)
        val state = dupViewModel.state.value

        assertEquals(WidgetType.values().size, state.items.size)
        WidgetType.values().forEachIndexed { index, expectedType ->
            val actualType = state.items[index].widgetType
            // Note: SWITCH and CHECKBOX both map to QuestionnaireItemType.Boolean.
            // On duplicate, they map back to WidgetType.SWITCH since there's no itemControl code saved for CHECKBOX.
            if (expectedType == WidgetType.CHECKBOX) {
                assertEquals(WidgetType.SWITCH, actualType)
            } else {
                assertEquals(expectedType, actualType)
            }
        }
    }

    /**
     * Test testSaveQuestionnaireDuplicateNameError.
     */
    @Test
    fun testSaveQuestionnaireDuplicateNameError() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }

        val viewModel1 = QuestionnaireBuilderViewModel(repo)
        viewModel1.updateTitle("Unique Name")
        viewModel1.addItem(WidgetType.SINGLE_LINE_TEXT)
        val savedId = viewModel1.saveQuestionnaire()
        assertTrue(savedId != null)

        val viewModel2 = QuestionnaireBuilderViewModel(repo)
        viewModel2.updateTitle("Unique Name")
        viewModel2.addItem(WidgetType.SINGLE_LINE_TEXT)

        val failedId = viewModel2.saveQuestionnaire()
        assertEquals(null, failedId)
        assertTrue(viewModel2.state.value.isDuplicateNameError)

        // Update title should clear error
        viewModel2.updateTitle("Another Name")
        assertFalse(viewModel2.state.value.isDuplicateNameError)

        val successfulId = viewModel2.saveQuestionnaire()
        assertTrue(successfulId != null)
    }

    /**
     * Verifies that localized title and item resolvers are utilized during questionnaire duplication and item creation.
     */
    @Test
    fun testLocalizedResolvers() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val sourceViewModel =
            QuestionnaireBuilderViewModel(
                repository = repo,
                copyTitleResolver = { "$it (Copia)" },
                defaultItemLabelResolver = { "Nuevo Elemento" },
                widgetItemLabelResolver = { "Nuevo ${it.name}" },
                unknownTitleResolver = { "Desconocido" },
            )
        sourceViewModel.updateTitle("Formulario Fuente")
        sourceViewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        assertEquals(
            "Nuevo SINGLE_LINE_TEXT",
            sourceViewModel.state.value.items
                .first()
                .label,
        )
        val sourceId = sourceViewModel.saveQuestionnaire()
        assertTrue(sourceId != null)

        val dupViewModel =
            QuestionnaireBuilderViewModel(
                repository = repo,
                duplicateFromId = sourceId,
                copyTitleResolver = { "$it (Copia)" },
                defaultItemLabelResolver = { "Nuevo Elemento" },
                widgetItemLabelResolver = { "Nuevo ${it.name}" },
                unknownTitleResolver = { "Desconocido" },
            )
        assertEquals("Formulario Fuente (Copia)", dupViewModel.state.value.title)
    }

    /**
     * Test testValidateCatchesItemError.
     */
    @Test
    fun testValidateCatchesItemError() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("Valid Title")

        // SINGLE_SELECT without options is an error state
        viewModel.addItem(WidgetType.SINGLE_SELECT)
        assertTrue(
            viewModel.state.value.items
                .last()
                .isError,
        )

        assertTrue(viewModel.validate().isFailure)
    }

    /**
     * Verifies that non-Latin titles (Hebrew/CJK) generate valid, non-empty custom UUID-based IDs.
     */
    @Test
    fun testNonLatinTitleGeneratesValidId() {
        val repo = QuestionnaireRepository()
        kotlinx.coroutines.runBlocking { repo.loadDefaultForms() }
        val viewModel = QuestionnaireBuilderViewModel(repo)

        viewModel.updateTitle("臨床評估問卷")
        viewModel.addItem(WidgetType.SINGLE_LINE_TEXT)
        val q = viewModel.buildQuestionnaire()
        val qId = q.id
        assertNotNull(qId)
        assertTrue(qId.startsWith("custom-"))
        assertTrue(qId.length > "custom-".length)
    }

    /**
     * Tests saving duplicate questionnaire name triggers duplicate name error.
     */
    @Test
    fun testSaveDuplicateQuestionnaire() {
        val repo = QuestionnaireRepository()
        val vm1 = QuestionnaireBuilderViewModel(repo)
        vm1.updateTitle("Unique Form Name")
        vm1.addItem(WidgetType.SINGLE_LINE_TEXT)
        val id1 = vm1.saveQuestionnaire()
        assertNotNull(id1)

        val vm2 = QuestionnaireBuilderViewModel(repo)
        vm2.updateTitle("Unique Form Name")
        vm2.addItem(WidgetType.SINGLE_LINE_TEXT)
        val id2 = vm2.saveQuestionnaire()
        kotlin.test.assertNull(id2)
        assertTrue(vm2.state.value.isDuplicateNameError)
    }

    /**
     * Tests removing an item cleans up enableWhen conditions referencing it.
     */
    @Test
    fun testRemoveItemCleansEnableWhenCondition() {
        val repo = QuestionnaireRepository()
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        val item1Id =
            vm.state.value.items[0]
                .linkId
        val item2Id =
            vm.state.value.items[1]
                .linkId
        vm.updateItemEnableWhen(
            item2Id,
            listOf(BuilderEnableWhen(question = item1Id, operator = Questionnaire.QuestionnaireItemOperator.Exists, answerBoolean = true)),
        )
        assertEquals(
            1,
            vm.state.value.items[1]
                .enableWhen.size,
        )
        vm.removeItem(item1Id)
        assertEquals(2, vm.state.value.items.size)
        assertTrue(
            vm.state.value.items[0]
                .enableWhen
                .isEmpty(),
        )
    }

    /**
     * Tests building enableWhen conditions across integer, decimal, string, and default palette options.
     */
    @Test
    fun testBuildEnableWhenAnswerVariantsAndPaletteDefaultOptions() {
        val repo = QuestionnaireRepository()
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.updateTitle("EnableWhen Variants")
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm.addItem(WidgetType.FITZPATRICK_PALETTE, options = emptyList())
        val item2Id =
            vm.state.value.items[1]
                .linkId
        val conditions =
            listOf(
                BuilderEnableWhen(question = "item_1", operator = Questionnaire.QuestionnaireItemOperator.EqualTo, answerInteger = 42),
                BuilderEnableWhen(
                    question = "item_1",
                    operator = Questionnaire.QuestionnaireItemOperator.GreaterThan,
                    answerDecimal = 3.14,
                ),
                BuilderEnableWhen(question = "item_1", operator = Questionnaire.QuestionnaireItemOperator.EqualTo, answerString = "test"),
                BuilderEnableWhen(question = "item_1", operator = Questionnaire.QuestionnaireItemOperator.Exists, answerBoolean = null),
            )
        vm.updateItemEnableWhen(item2Id, conditions, Questionnaire.EnableWhenBehavior.All)
        val q = vm.buildQuestionnaire()
        assertEquals(2, q.item.size)
        val paletteItem = q.item[1]
        assertEquals(6, paletteItem.answerOption.size)
        assertEquals(4, paletteItem.enableWhen.size)
    }

    /**
     * Tests duplicating a questionnaire covering all FHIR item controls, standard types, and enableWhen formats.
     */
    @Test
    fun testDuplicateAllFhirItemTypesAndItemControls() {
        val repo = QuestionnaireRepository()
        val controls =
            listOf(
                "photo",
                "video",
                "switch",
                "slider",
                "pain-vas",
                "wong-baker",
                "palette",
                "color-palette",
                "fitzpatrick",
                "body-map",
                "segmented-control",
                "choice-cards",
                "check-box",
            )
        val items = mutableListOf<Questionnaire.Item>()
        val eqOp =
            dev.ohs.fhir.model.r4
                .Enumeration(value = Questionnaire.QuestionnaireItemOperator.EqualTo)
        controls.forEachIndexed { i, ctrl ->
            val ext =
                dev.ohs.fhir.model.r4.Extension(
                    url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                    value =
                        dev.ohs.fhir.model.r4.Extension.Value.CodeableConcept(
                            dev.ohs.fhir.model.r4.CodeableConcept(
                                coding =
                                    listOf(
                                        dev.ohs.fhir.model.r4
                                            .Coding(
                                                code =
                                                    dev.ohs.fhir.model.r4
                                                        .Code(value = ctrl),
                                            ),
                                    ),
                            ),
                        ),
                )
            items.add(
                Questionnaire.Item(
                    linkId =
                        dev.ohs.fhir.model.r4
                            .String(value = "ctrl_$i"),
                    type =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                    extension = listOf(ext),
                    repeats =
                        if (ctrl == "check-box") {
                            dev.ohs.fhir.model.r4
                                .Boolean(value = true)
                        } else {
                            null
                        },
                ),
            )
        }

        items.add(
            Questionnaire.Item(
                linkId =
                    dev.ohs.fhir.model.r4
                        .String(value = "cb_no_rep"),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                extension =
                    listOf(
                        dev.ohs.fhir.model.r4.Extension(
                            url = "http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl",
                            value =
                                dev.ohs.fhir.model.r4.Extension.Value.CodeableConcept(
                                    dev.ohs.fhir.model.r4.CodeableConcept(
                                        coding =
                                            listOf(
                                                dev.ohs.fhir.model.r4
                                                    .Coding(
                                                        code =
                                                            dev.ohs.fhir.model.r4
                                                                .Code(value = "check-box"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
                repeats =
                    dev.ohs.fhir.model.r4
                        .Boolean(value = false),
            ),
        )

        val standardTypes =
            listOf(
                Questionnaire.QuestionnaireItemType.Attachment,
                Questionnaire.QuestionnaireItemType.Boolean,
                Questionnaire.QuestionnaireItemType.Choice,
                Questionnaire.QuestionnaireItemType.String,
                Questionnaire.QuestionnaireItemType.Text,
                Questionnaire.QuestionnaireItemType.Date,
                Questionnaire.QuestionnaireItemType.DateTime,
                Questionnaire.QuestionnaireItemType.Decimal,
                Questionnaire.QuestionnaireItemType.Integer,
                Questionnaire.QuestionnaireItemType.Group,
                Questionnaire.QuestionnaireItemType.Display,
            )
        standardTypes.forEachIndexed { i, st ->
            items.add(
                Questionnaire.Item(
                    linkId =
                        dev.ohs.fhir.model.r4
                            .String(value = "type_$i"),
                    type =
                        dev.ohs.fhir.model.r4
                            .Enumeration(value = st),
                    repeats =
                        if (st == Questionnaire.QuestionnaireItemType.Choice) {
                            dev.ohs.fhir.model.r4
                                .Boolean(value = true)
                        } else {
                            null
                        },
                ),
            )
        }

        items.add(
            Questionnaire.Item(
                linkId =
                    dev.ohs.fhir.model.r4
                        .String(value = "ew_all"),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = Questionnaire.QuestionnaireItemType.String),
                enableWhen =
                    listOf(
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q1"),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .String(
                                        dev.ohs.fhir.model.r4
                                            .String(value = "str"),
                                    ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q2"),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .Boolean(
                                        dev.ohs.fhir.model.r4
                                            .Boolean(value = true),
                                    ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q3"),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .Integer(
                                        dev.ohs.fhir.model.r4
                                            .Integer(value = 10),
                                    ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q4"),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer.Decimal(
                                    dev.ohs.fhir.model.r4
                                        .Decimal(
                                            value =
                                                dev.ohs.fhir.model.r4.FhirDecimal
                                                    .fromString("1.5"),
                                        ),
                                ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q5"),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .Decimal(
                                        dev.ohs.fhir.model.r4
                                            .Decimal(value = null),
                                    ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = "q6"),
                            operator =
                                dev.ohs.fhir.model.r4
                                    .Enumeration(value = null),
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .Boolean(
                                        dev.ohs.fhir.model.r4
                                            .Boolean(value = true),
                                    ),
                        ),
                        Questionnaire.Item.EnableWhen(
                            question =
                                dev.ohs.fhir.model.r4
                                    .String(value = null),
                            operator = eqOp,
                            answer =
                                Questionnaire.Item.EnableWhen.Answer
                                    .Boolean(
                                        dev.ohs.fhir.model.r4
                                            .Boolean(value = true),
                                    ),
                        ),
                    ),
                answerOption =
                    listOf(
                        Questionnaire.Item.AnswerOption(
                            value =
                                Questionnaire.Item.AnswerOption.Value.Coding(
                                    dev.ohs.fhir.model.r4
                                        .Coding(
                                            display =
                                                dev.ohs.fhir.model.r4
                                                    .String(value = "Option Display"),
                                        ),
                                ),
                        ),
                        Questionnaire.Item.AnswerOption(
                            value =
                                Questionnaire.Item.AnswerOption.Value
                                    .String(
                                        dev.ohs.fhir.model.r4
                                            .String(value = "Not Coding"),
                                    ),
                        ),
                    ),
            ),
        )

        // Item with Choice type and repeats = false
        items.add(
            Questionnaire.Item(
                linkId =
                    dev.ohs.fhir.model.r4
                        .String(value = "choice_no_rep"),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = Questionnaire.QuestionnaireItemType.Choice),
                repeats = null,
            ),
        )

        // Item with type.value == null and text.value == null
        items.add(
            Questionnaire.Item(
                linkId =
                    dev.ohs.fhir.model.r4
                        .String(value = "null_type_item"),
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = null),
                text =
                    dev.ohs.fhir.model.r4
                        .String(value = null),
            ),
        )

        items.add(
            Questionnaire.Item(
                linkId =
                    dev.ohs.fhir.model.r4
                        .String(value = null),
                text = null,
                type =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = Questionnaire.QuestionnaireItemType.String),
            ),
        )

        val qSource =
            Questionnaire(
                id = "q-duplicate-source",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                title =
                    dev.ohs.fhir.model.r4
                        .String(value = null),
                item = items,
            )
        repo.saveQuestionnaire(qSource)

        val vm = QuestionnaireBuilderViewModel(repo, duplicateFromId = "q-duplicate-source")
        assertTrue(
            vm.state.value.items
                .isNotEmpty(),
        )
        val qBuilt = vm.buildQuestionnaire()
        assertTrue(qBuilt.item.isNotEmpty())

        val qEmpty =
            Questionnaire(
                id = "q-empty",
                status =
                    dev.ohs.fhir.model.r4
                        .Enumeration(value = dev.ohs.fhir.model.r4.terminologies.PublicationStatus.Active),
                item = emptyList(),
            )
        repo.saveQuestionnaire(qEmpty)
        val vmEmpty = QuestionnaireBuilderViewModel(repo, duplicateFromId = "q-empty")
        assertTrue(
            vmEmpty.state.value.items
                .isEmpty(),
        )

        // Duplicate from non-existent ID
        val vmNonExistent = QuestionnaireBuilderViewModel(repo, duplicateFromId = "non-existent-form-id")
        assertTrue(
            vmNonExistent.state.value.items
                .isEmpty(),
        )
    }

    /**
     * Tests moveItem boundaries, update non-matching item, and blank title fallback.
     */
    @Test
    fun testBuilderItemMovementAndBlankTitleFallback() {
        val repo = QuestionnaireRepository()
        val vm = QuestionnaireBuilderViewModel(repo)
        vm.addItem(WidgetType.SINGLE_LINE_TEXT)
        vm.addItem(WidgetType.MULTI_LINE_TEXT)
        val id1 =
            vm.state.value.items[0]
                .linkId
        val id2 =
            vm.state.value.items[1]
                .linkId

        // Move first item up (cannot move up, index > 0 is false)
        vm.moveItemUp(id1)
        assertEquals(
            id1,
            vm.state.value.items[0]
                .linkId,
        )

        // Move non-existent item up
        vm.moveItemUp("non_existent_id")

        // Move last item down (cannot move down, index < size - 1 is false)
        vm.moveItemDown(id2)
        assertEquals(
            id2,
            vm.state.value.items[1]
                .linkId,
        )

        // Move non-existent item down
        vm.moveItemDown("non_existent_id")

        // Update item with non-matching linkId
        vm.updateItem("unmatched_link_id", "New Label", emptyList())
        assertEquals(
            id1,
            vm.state.value.items[0]
                .linkId,
        )

        // Update item with blank label
        vm.updateItem(id1, "   ", emptyList())
        assertTrue(
            vm.state.value.items[0]
                .isError,
        )

        // Add segmented tiles and update with empty options
        vm.addItem(WidgetType.SEGMENTED_TILES)
        val segId =
            vm.state.value.items
                .last()
                .linkId
        vm.updateItem(segId, "Segmented", emptyList())
        assertTrue(
            vm.state.value.items
                .last()
                .isError,
        )

        // Blank title build fallback
        vm.updateTitle("   ")
        val q = vm.buildQuestionnaire()
        assertNotNull(q.id)
    }
}
