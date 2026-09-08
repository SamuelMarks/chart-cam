/**
 * @file OnboardingTutorialScreen.kt
 * Contains declarations for OnboardingTutorialScreen.kt.
 *
 * Implements the onboarding workflow tutorial carousel with accessible controls and localization.
 */
package io.healthplatform.chartcam.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chartcam.chartcam.generated.resources.Res
import chartcam.chartcam.generated.resources.cd_go_to_page
import chartcam.chartcam.generated.resources.cd_tutorial_get_started
import chartcam.chartcam.generated.resources.cd_tutorial_next
import chartcam.chartcam.generated.resources.cd_tutorial_previous
import chartcam.chartcam.generated.resources.cd_tutorial_skip
import chartcam.chartcam.generated.resources.state_selected
import chartcam.chartcam.generated.resources.state_unselected
import chartcam.chartcam.generated.resources.tutorial_desc_1
import chartcam.chartcam.generated.resources.tutorial_desc_2
import chartcam.chartcam.generated.resources.tutorial_desc_3
import chartcam.chartcam.generated.resources.tutorial_desc_4
import chartcam.chartcam.generated.resources.tutorial_get_started
import chartcam.chartcam.generated.resources.tutorial_next
import chartcam.chartcam.generated.resources.tutorial_page_indicator_format
import chartcam.chartcam.generated.resources.tutorial_previous
import chartcam.chartcam.generated.resources.tutorial_skip
import chartcam.chartcam.generated.resources.tutorial_title_1
import chartcam.chartcam.generated.resources.tutorial_title_2
import chartcam.chartcam.generated.resources.tutorial_title_3
import chartcam.chartcam.generated.resources.tutorial_title_4
import io.healthplatform.chartcam.ui.components.LanguageMenu
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Data model for a single workflow tutorial step with localized copy and graphic asset.
 *
 * @param titleRes StringResource for the step heading.
 * @param descriptionRes StringResource for the descriptive summary.
 * @param icon ImageVector icon illustrating the feature.
 * @param testTag Unique tag for test automation.
 */
data class TutorialSlide(
    val titleRes: StringResource,
    val descriptionRes: StringResource,
    val icon: ImageVector,
    val testTag: String,
)

/**
 * Test tag constants and default slides for the onboarding workflow tutorial.
 */
object OnboardingTutorialDefaults {
    /** Test tag for the skip action button. */
    const val TAG_TUTORIAL_SKIP = "tutorial_skip_button"

    /** Test tag for the next page action button. */
    const val TAG_TUTORIAL_NEXT = "tutorial_next_button"

    /** Test tag for the previous page action button. */
    const val TAG_TUTORIAL_PREV = "tutorial_prev_button"

    /** Test tag for the final get started action button. */
    const val TAG_TUTORIAL_GET_STARTED = "tutorial_get_started_button"

    /** Test tag for the horizontal pager container. */
    const val TAG_TUTORIAL_PAGER = "tutorial_pager"

    /** Test tag for the page indicator dots container. */
    const val TAG_TUTORIAL_INDICATORS = "tutorial_indicator_row"

    /** Test tag for slide title text. */
    const val TAG_TUTORIAL_SLIDE_TITLE = "tutorial_slide_title"

    /** Test tag for slide description text. */
    const val TAG_TUTORIAL_SLIDE_DESC = "tutorial_slide_desc"

    /**
     * Default list of 4 core workflow slides covering privacy, patients, capture, and export.
     */
    val DEFAULT_SLIDES =
        listOf(
            TutorialSlide(
                titleRes = Res.string.tutorial_title_1,
                descriptionRes = Res.string.tutorial_desc_1,
                icon = Icons.Default.Security,
                testTag = "slide_security",
            ),
            TutorialSlide(
                titleRes = Res.string.tutorial_title_2,
                descriptionRes = Res.string.tutorial_desc_2,
                icon = Icons.Default.Person,
                testTag = "slide_patient",
            ),
            TutorialSlide(
                titleRes = Res.string.tutorial_title_3,
                descriptionRes = Res.string.tutorial_desc_3,
                icon = Icons.Default.CameraAlt,
                testTag = "slide_capture",
            ),
            TutorialSlide(
                titleRes = Res.string.tutorial_title_4,
                descriptionRes = Res.string.tutorial_desc_4,
                icon = Icons.Default.Share,
                testTag = "slide_export",
            ),
        )
}

/**
 * Fullscreen onboarding tutorial composable displaying a swipeable carousel of key clinical workflows.
 * Provides accessible controls, minimum 48dp touch targets, semantic headings, and TalkBack live region updates.
 *
 * @param onDismiss Callback invoked when the user skips or dismisses the tutorial.
 * @param onComplete Callback invoked when the user reaches the end and taps "Get Started".
 * @param modifier Optional modifier applied to the root container.
 * @param slides The list of tutorial slides to display. Defaults to [OnboardingTutorialDefaults.DEFAULT_SLIDES].
 */
@Composable
fun OnboardingTutorialScreen(
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    slides: List<TutorialSlide> = OnboardingTutorialDefaults.DEFAULT_SLIDES,
) {
    val currentLang by currentLanguageState.collectAsState()

    key(currentLang) {
        val pagerState = rememberPagerState(pageCount = { slides.size })
        val coroutineScope = rememberCoroutineScope()
        val isLastPage = pagerState.currentPage == slides.size - 1
        val isFirstPage = pagerState.currentPage == 0

        val pageIndicatorSemantics =
            stringResource(
                Res.string.tutorial_page_indicator_format,
                pagerState.currentPage + 1,
                slides.size,
            )

        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top Action Bar: Language Switcher and Dismiss Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LanguageMenu()

                    val cdSkip = stringResource(Res.string.cd_tutorial_skip)
                    TextButton(
                        onClick = onDismiss,
                        modifier =
                            Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SKIP)
                                .semantics {
                                    contentDescription = cdSkip
                                },
                    ) {
                        Text(
                            text = stringResource(Res.string.tutorial_skip),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Carousel Pager
                HorizontalPager(
                    state = pagerState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PAGER),
                ) { pageIndex ->
                    val slide = slides[pageIndex]
                    TutorialSlideContent(slide = slide)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Page Indicator Dots
                Row(
                    modifier =
                        Modifier
                            .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_INDICATORS)
                            .semantics {
                                liveRegion = LiveRegionMode.Polite
                                contentDescription = pageIndicatorSemantics
                            }.padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(slides.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        val size = if (isSelected) 10.dp else 8.dp
                        val goToPageLabel = stringResource(Res.string.cd_go_to_page, index + 1)
                        val stateDesc =
                            stringResource(
                                if (isSelected) Res.string.state_selected else Res.string.state_unselected,
                            )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable(
                                        role = Role.Tab,
                                        onClickLabel = goToPageLabel,
                                    ) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }.semantics {
                                        contentDescription = goToPageLabel
                                        stateDescription = stateDesc
                                    },
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(size)
                                        .clip(CircleShape)
                                        .background(color),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Navigation Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Previous Slide Button
                    if (!isFirstPage) {
                        val cdPrev = stringResource(Res.string.cd_tutorial_previous)
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            modifier =
                                Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_PREV)
                                    .semantics {
                                        contentDescription = cdPrev
                                    },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(text = stringResource(Res.string.tutorial_previous))
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    // Next / Get Started Button
                    if (isLastPage) {
                        val cdGetStarted = stringResource(Res.string.cd_tutorial_get_started)
                        Button(
                            onClick = onComplete,
                            modifier =
                                Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_GET_STARTED)
                                    .semantics {
                                        contentDescription = cdGetStarted
                                    },
                        ) {
                            Text(
                                text = stringResource(Res.string.tutorial_get_started),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        val cdNext = stringResource(Res.string.cd_tutorial_next)
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier =
                                Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_NEXT)
                                    .semantics {
                                        contentDescription = cdNext
                                    },
                        ) {
                            Text(text = stringResource(Res.string.tutorial_next))
                            Spacer(modifier = Modifier.size(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders the content of an individual tutorial slide with accessible icon, title, and description.
 *
 * @param slide The [TutorialSlide] containing localized resource definitions and icon.
 * @param modifier Optional modifier applied to the slide container.
 */
@Composable
fun TutorialSlideContent(
    slide: TutorialSlide,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = slide.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(slide.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SLIDE_TITLE)
                    .semantics { heading() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(slide.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(OnboardingTutorialDefaults.TAG_TUTORIAL_SLIDE_DESC),
        )
    }
}
