/**
 * @file StandardQuestionnaireTranslations.kt
 * Contains localized title, item prompt, and answer option translation tables for
 * ChartCam's standard bundled FHIR Questionnaires (`std-form`, `basic-followup`, `facial-cornea-profile`).
 */
package io.healthplatform.chartcam.repository

/**
 * Provides multi-language translations (Spanish, Japanese, Hebrew, Traditional Chinese)
 * for the standard bundled FHIR Questionnaires.
 */
internal object StandardQuestionnaireTranslations {
    /**
     * Returns translations for standard answer options.
     *
     * @param optionValue The raw option value or display.
     * @return Map of language code to translated option text.
     */
    fun getOptionTranslations(optionValue: String): Map<String, String> =
        when (optionValue.lowercase()) {
            "routine" ->
                mapOf(
                    "es" to "Rutina",
                    "ja" to "定期",
                    "he" to "שגרתי",
                    "zh" to "常規",
                )
            "urgent" ->
                mapOf(
                    "es" to "Urgente",
                    "ja" to "緊急",
                    "he" to "דחוף",
                    "zh" to "緊急",
                )
            else -> emptyMap()
        }

    /**
     * Returns standard translated titles for bundled forms.
     *
     * @param qId The questionnaire template ID.
     * @return Map of language code to translated title.
     */
    fun getTitleTranslations(qId: String): Map<String, String> =
        when (qId) {
            "std-form" ->
                mapOf(
                    "es" to "Formulario Clínico Estándar",
                    "ja" to "標準臨床問診票",
                    "he" to "טופס קליני סטנדרטי",
                    "zh" to "標準臨床問診表",
                )
            "basic-followup" ->
                mapOf(
                    "es" to "Seguimiento Básico",
                    "ja" to "基本フォローアップ",
                    "he" to "מעקב בסיסי",
                    "zh" to "基本追蹤",
                )
            "facial-cornea-profile" ->
                mapOf(
                    "es" to "Examen de perfil facial y córnea",
                    "ja" to "顔貌側面および角膜検査",
                    "he" to "בדיקת פרופיל פנים וקרנית",
                    "zh" to "面部側臉與角膜檢查",
                )
            else -> emptyMap()
        }

    /**
     * Returns standard translated item text for bundled forms.
     *
     * @param qId The questionnaire template ID.
     * @param linkId The item linkId.
     * @return Map of language code to translated item question text.
     */
    fun getItemTranslations(
        qId: String,
        linkId: String,
    ): Map<String, String> =
        when (qId) {
            "std-form" -> getStdFormItemTranslations(linkId)
            "basic-followup" -> getBasicFollowupItemTranslations(linkId)
            "facial-cornea-profile" -> getFacialCorneaProfileItemTranslations(linkId)
            else -> emptyMap()
        }

    /**
     * Item translations for the facial profile and cornea examination template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getFacialCorneaProfileItemTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "clinical_notes" ->
                mapOf(
                    "es" to "Observaciones clínicas y notas",
                    "ja" to "臨床所見およびメモ",
                    "he" to "תצפיות קליניות והערות",
                    "zh" to "臨床觀察與備註",
                )
            "profile_left" ->
                mapOf(
                    "es" to "Perfil izquierdo (córnea y nariz)",
                    "ja" to "左側面プロファイル（角膜・鼻）",
                    "he" to "פרופיל שמאל (קרנית ואף)",
                    "zh" to "左側臉（角膜與鼻子）",
                )
            "front_view" ->
                mapOf(
                    "es" to "Vista frontal",
                    "ja" to "正面視",
                    "he" to "מבט חזיתי",
                    "zh" to "正臉視角",
                )
            "profile_right" ->
                mapOf(
                    "es" to "Perfil derecho (córnea y nariz)",
                    "ja" to "右側面プロファイル（角膜・鼻）",
                    "he" to "פרופיל ימין (קרנית ואף)",
                    "zh" to "右側臉（角膜與鼻子）",
                )
            else -> emptyMap()
        }

    /**
     * Item translations for the standard clinical photo template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormItemTranslations(linkId: String): Map<String, String> =
        getStdFormGeneralTranslations(linkId).ifEmpty { getStdFormPhotoTranslations(linkId) }

    /**
     * General translations for standard form.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormGeneralTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "notes" ->
                mapOf(
                    "es" to "Notas Clínicas",
                    "ja" to "臨床記録",
                    "he" to "הערות קליניות",
                    "zh" to "臨床筆記",
                )
            "front" ->
                mapOf(
                    "es" to "Frente",
                    "ja" to "正面",
                    "he" to "חזית",
                    "zh" to "正面",
                )
            "front_ruler" ->
                mapOf(
                    "es" to "Frente + Regla",
                    "ja" to "正面 + 定規",
                    "he" to "חזית + סרגל",
                    "zh" to "正面 + 尺",
                )
            else -> emptyMap()
        }

    /**
     * Photo orientation translations for standard form.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getStdFormPhotoTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "right" ->
                mapOf(
                    "es" to "Lado Derecho",
                    "ja" to "右側",
                    "he" to "צד ימין",
                    "zh" to "右側",
                )
            "right_ruler" ->
                mapOf(
                    "es" to "Lado Derecho + Regla",
                    "ja" to "右側 + 定規",
                    "he" to "צד ימין + סרגל",
                    "zh" to "右側 + 尺",
                )
            "back" ->
                mapOf(
                    "es" to "Espalda",
                    "ja" to "背面",
                    "he" to "גב",
                    "zh" to "背面",
                )
            "back_ruler" ->
                mapOf(
                    "es" to "Espalda + Regla",
                    "ja" to "背面 + 定規",
                    "he" to "גב + סרגל",
                    "zh" to "背面 + 尺",
                )
            "left" ->
                mapOf(
                    "es" to "Lado Izquierdo",
                    "ja" to "左側",
                    "he" to "צד שמאל",
                    "zh" to "左側",
                )
            "left_ruler" ->
                mapOf(
                    "es" to "Lado Izquierdo + Regla",
                    "ja" to "左側 + 定規",
                    "he" to "צד שמאל + סרגל",
                    "zh" to "左側 + 尺",
                )
            else -> emptyMap()
        }

    /**
     * Item translations for the basic follow-up template.
     *
     * @param linkId The item linkId.
     * @return Map of language code to translated text.
     */
    private fun getBasicFollowupItemTranslations(linkId: String): Map<String, String> =
        when (linkId) {
            "notes" ->
                mapOf(
                    "es" to "Notas de seguimiento",
                    "ja" to "フォローアップ記録",
                    "he" to "הערות מעקב",
                    "zh" to "追蹤記錄",
                )
            "followup_type" ->
                mapOf(
                    "es" to "Tipo de seguimiento",
                    "ja" to "フォローアップの種類",
                    "he" to "סוג המעקב",
                    "zh" to "追蹤類型",
                )
            "urgent_reason" ->
                mapOf(
                    "es" to "Motivo de urgencia",
                    "ja" to "緊急の理由",
                    "he" to "סיבת הדחיפות",
                    "zh" to "緊急原因",
                )
            "patient_consent" ->
                mapOf(
                    "es" to "El paciente consintió las fotos",
                    "ja" to "患者が写真撮影に同意しました",
                    "he" to "המטופל הסכים לצילום תמונות",
                    "zh" to "病患已同意拍攝相片",
                )
            "front" ->
                mapOf(
                    "es" to "Vista frontal",
                    "ja" to "正面写真",
                    "he" to "מבט חזיתי",
                    "zh" to "正面視圖",
                )
            else -> emptyMap()
        }
}
