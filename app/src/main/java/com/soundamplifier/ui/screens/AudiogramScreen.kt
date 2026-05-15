package com.soundamplifier.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundamplifier.ui.animation.sunoBounceScale
import com.soundamplifier.R
import com.soundamplifier.data.AUDIOGRAM_FREQUENCIES
import com.soundamplifier.viewmodel.AudiogramViewModel
import com.soundamplifier.viewmodel.HearingTestUiState
import com.soundamplifier.viewmodel.TestPhase
import kotlinx.coroutines.delay
import kotlin.math.log10

/** Hearing-test flow accent (testing + intro/transition/results headers on this screen only). */
private val TestAccentRed = Color(0xFFE53935)

/** Light testing screen (matches product mockup). */
private val HearingTestScreenBg = Color(0xFFF2F4F5)
private val HearingTestChipBg = Color(0xFFE3ECF5)
private val HearingTestChipFg = Color(0xFF455A64)
private val HearingTestMutedLabel = Color(0xFF78909C)
private val HearingTestTextPrimary = Color(0xFF263238)
private val HearingTestHzMuted = Color(0xFF90A4AE)
private val HearingTestCantButtonBg = Color(0xFFECEFF1)
private val HearingTestCantIconCircle = Color(0xFF546E7A)
private val HearingTestTrack = Color(0xFFE0E0E0)
private val HearingTestRedDeep = Color(0xFFC62828)

/** Audiogram chart lines (left vs right) — kept distinct for readability. */
private val ChartLeftEarColor = Color(0xFF1565C0)
private val ChartRightEarColor = Color(0xFFC62828)

private const val INSTRUCTION_AUTO_DISMISS_MS = 6000L

/** Overall 0..1 including sub-progress within the current frequency. */
fun overallTestProgress(ui: HearingTestUiState): Float {
    if (ui.phase != TestPhase.TESTING_LEFT && ui.phase != TestPhase.TESTING_RIGHT) return 0f
    val earBase = if (ui.phase == TestPhase.TESTING_LEFT) 0 else 6
    val cap = AudiogramViewModel.MAX_VOLUME_STEPS_PER_FREQ_FOR_UI.toFloat().coerceAtLeast(1f)
    val within = (ui.volumeStepCount / cap).coerceIn(0f, 0.92f)
    return ((earBase + ui.currentFrequencyIndex + within) / 12f).coerceIn(0f, 1f)
}

@Composable
fun AudiogramScreen(
    uiState: HearingTestUiState,
    onStart: () -> Unit,
    onHeard: () -> Unit,
    onCannotHear: () -> Unit,
    onContinueTransition: () -> Unit,
    onSaveAndApply: () -> Unit,
    onRetake: () -> Unit,
    onDismissInstruction: () -> Unit = {},
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState.phase) {
            TestPhase.INTRO -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IntroContent(onStart)
            }
            TestPhase.TESTING_LEFT,
            TestPhase.TESTING_RIGHT,
            -> TestingContent(
                uiState = uiState,
                onHeard = onHeard,
                onCannotHear = onCannotHear,
                onDismissInstruction = onDismissInstruction,
            )
            TestPhase.TRANSITION -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TransitionContent(uiState, onContinueTransition)
            }
            TestPhase.COMPLETE -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ResultsContent(uiState, onSaveAndApply, onRetake)
            }
        }
    }
}

@Composable
private fun IntroContent(onStart: () -> Unit) {
    Text(
        text = stringResource(R.string.hearing_test_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.hearing_test_intro),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.hearing_test_time),
        style = MaterialTheme.typography.labelMedium,
        color = TestAccentRed
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hearing_test_start))
    }
}

@Composable
private fun TestingContent(
    uiState: HearingTestUiState,
    onHeard: () -> Unit,
    onCannotHear: () -> Unit,
    onDismissInstruction: () -> Unit,
) {
    val leftPhase = uiState.phase == TestPhase.TESTING_LEFT
    val earLabel = if (leftPhase) {
        stringResource(R.string.hearing_test_left_ear)
    } else {
        stringResource(R.string.hearing_test_right_ear)
    }

    val tapScale = remember { Animatable(1f) }
    LaunchedEffect(uiState.interactionEpoch) {
        if (uiState.interactionEpoch > 0) {
            tapScale.snapTo(0.92f)
            tapScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
            )
        }
    }

    LaunchedEffect(uiState.showInstructionOverlay) {
        if (!uiState.showInstructionOverlay) return@LaunchedEffect
        delay(INSTRUCTION_AUTO_DISMISS_MS)
        onDismissInstruction()
    }

    val primaryProgress by animateFloatAsState(
        targetValue = overallTestProgress(uiState),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "progress",
    )

    val enabled = uiState.responseEnabled && !uiState.isPlayingTone

    val infinite = rememberInfiniteTransition(label = "tone")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val hz = AUDIOGRAM_FREQUENCIES[uiState.currentFrequencyIndex]
    val iconScale = if (uiState.isPlayingTone) pulse else 1f

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    scaleX = tapScale.value
                    scaleY = tapScale.value
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HearingTestScreenBg),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.36f
                    val baseR = size.minDimension * 0.11f
                    for (i in 1..6) {
                        drawCircle(
                            color = TestAccentRed.copy(alpha = 0.028f * (7 - i)),
                            radius = baseR * i * 1.2f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.1f),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HearingTestEarPill(earLabel = earLabel)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.hearing_test_frequency_caps,
                                uiState.currentFrequencyIndex + 1,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = HearingTestMutedLabel,
                            letterSpacing = 0.6.sp,
                        )
                        Text(
                            text = stringResource(R.string.hearing_test_step_n, uiState.volumeStepCount + 1),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TestAccentRed,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    HearingTestSlimProgress(progress = primaryProgress)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.hearing_test_current_frequency_label),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = HearingTestMutedLabel,
                        letterSpacing = 0.8.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = hz.toString(),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = HearingTestTextPrimary,
                            lineHeight = 56.sp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hearing_test_hz_unit),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = HearingTestHzMuted,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(216.dp)
                                .background(TestAccentRed.copy(alpha = 0.14f), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(188.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    ambientColor = TestAccentRed.copy(alpha = 0.45f),
                                    spotColor = TestAccentRed.copy(alpha = 0.5f),
                                )
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Hearing,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(92.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint = TestAccentRed,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(R.string.hearing_test_can_you_hear_it),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = HearingTestTextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(36.dp))

                    val cantIx = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .alpha(if (enabled) 1f else 0.42f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(HearingTestCantButtonBg)
                            .clickable(
                                interactionSource = cantIx,
                                indication = rememberRipple(
                                bounded = true,
                                color = HearingTestCantIconCircle.copy(alpha = 0.25f),
                            ),
                                enabled = enabled,
                                onClick = onCannotHear,
                            )
                            .sunoBounceScale(cantIx, enabled = enabled),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(HearingTestCantIconCircle),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.hearing_test_i_cant),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = HearingTestTextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val hearIx = remember { MutableInteractionSource() }
                    val hearBrush = Brush.horizontalGradient(
                        listOf(HearingTestRedDeep, TestAccentRed),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .alpha(if (enabled) 1f else 0.42f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(hearBrush)
                            .clickable(
                                interactionSource = hearIx,
                                indication = rememberRipple(
                                bounded = true,
                                color = Color.White.copy(alpha = 0.45f),
                            ),
                                enabled = enabled,
                                onClick = onHeard,
                            )
                            .sunoBounceScale(hearIx, enabled = enabled),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = TestAccentRed,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.hearing_test_i_hear),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.showInstructionOverlay,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            InstructionOverlayCard(
                onDismiss = onDismissInstruction,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun HearingTestEarPill(earLabel: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HearingTestChipBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Hearing,
            contentDescription = null,
            tint = HearingTestChipFg,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = earLabel,
            color = HearingTestChipFg,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HearingTestSlimProgress(progress: Float) {
    val p = progress.coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(HearingTestTrack),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(RoundedCornerShape(3.dp))
                .background(TestAccentRed),
        )
    }
}

@Composable
private fun InstructionOverlayCard(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.hearing_test_instruction_overlay),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.hearing_test_got_it))
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.hearing_test_skip_tip))
                }
            }
        }
    }
}

@Composable
private fun TransitionContent(uiState: HearingTestUiState, onContinue: () -> Unit) {
    val avgLeft = uiState.leftThresholds
        .filter { it >= 0 }
        .takeIf { it.size == 6 }
        ?.average()
        ?.toFloat()
        ?: 0f

    Text(
        text = stringResource(R.string.hearing_test_left_complete),
        style = MaterialTheme.typography.headlineSmall,
        color = TestAccentRed,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.hearing_test_switching_right_ear),
        style = MaterialTheme.typography.titleMedium,
        color = TestAccentRed,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.hearing_test_left_avg, "%.0f".format(avgLeft)),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.hearing_test_transition_body),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hearing_test_continue))
    }
}

@Composable
private fun ResultsContent(
    uiState: HearingTestUiState,
    onSaveAndApply: () -> Unit,
    onRetake: () -> Unit,
) {
    val leftF = uiState.leftThresholds.map { it.toFloat() }
    val rightF = uiState.rightThresholds.map { it.toFloat() }
    val bilateralAvg = (leftF + rightF).average().toFloat()
    val interpRes = when {
        bilateralAvg < 25f -> R.string.hearing_test_normal
        bilateralAvg < 40f -> R.string.hearing_test_mild
        bilateralAvg < 55f -> R.string.hearing_test_moderate
        bilateralAvg < 70f -> R.string.hearing_test_mod_severe
        else -> R.string.hearing_test_severe
    }
    val avgLeft = leftF.average().toFloat()
    val avgRight = rightF.average().toFloat()

    Text(
        text = stringResource(R.string.hearing_test_complete),
        style = MaterialTheme.typography.headlineMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.hearing_test_complete_status_line),
        style = MaterialTheme.typography.titleMedium,
        color = TestAccentRed,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))

    ResultsAudiogramChart(
        leftThresholds = leftF,
        rightThresholds = rightF,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(
            R.string.hearing_test_result_avgs,
            "%.0f".format(avgLeft),
            "%.0f".format(avgRight)
        ),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(interpRes),
        style = MaterialTheme.typography.titleMedium,
        color = TestAccentRed,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onSaveAndApply, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hearing_test_save))
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.hearing_test_retake))
    }
}

@Composable
private fun ResultsAudiogramChart(
    leftThresholds: List<Float>,
    rightThresholds: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val n = AUDIOGRAM_FREQUENCIES.size
        val maxDb = 80f
        drawAudiogramGrid(w, h, n, maxDb)
        plotEarLine(leftThresholds, ChartLeftEarColor, w, h, n, maxDb)
        plotEarLine(rightThresholds, ChartRightEarColor, w, h, n, maxDb)
    }
}

private fun DrawScope.drawAudiogramGrid(w: Float, h: Float, freqCount: Int, maxDb: Float) {
    val grid = Color.LightGray.copy(alpha = 0.7f)
    val steps = 5
    for (i in 0..steps) {
        val db = maxDb * i / steps
        val y = h * (db / maxDb)
        drawLine(grid, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
    for (i in 0 until freqCount) {
        val x = freqX(i, freqCount, w)
        drawLine(grid, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
    }
}

private fun freqX(i: Int, n: Int, w: Float): Float {
    if (n <= 1) return w / 2f
    val f = AUDIOGRAM_FREQUENCIES[i].toFloat()
    val f0 = AUDIOGRAM_FREQUENCIES.first().toFloat()
    val f1 = AUDIOGRAM_FREQUENCIES.last().toFloat()
    val t = (log10(f / f0) / log10(f1 / f0)).coerceIn(0f, 1f)
    return t * w
}

private fun DrawScope.plotEarLine(
    thresholds: List<Float>,
    color: Color,
    w: Float,
    h: Float,
    freqCount: Int,
    maxDb: Float,
) {
    if (thresholds.size < freqCount) return
    val points = thresholds.mapIndexed { i, db ->
        Offset(
            x = freqX(i, freqCount, w),
            y = h * (db.coerceIn(0f, maxDb) / maxDb)
        )
    }
    for (i in 0 until points.size - 1) {
        drawLine(color, points[i], points[i + 1], strokeWidth = 3f)
    }
    points.forEach { drawCircle(color, radius = 7f, center = it) }
}











