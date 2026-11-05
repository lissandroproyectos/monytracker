package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRed
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateCardLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CelebrationData(
    val title: String,
    val message: String,
    val commitmentTitle: String,
    val amountPaid: Double,
    val remainingAmount: Double,
    val currencySymbol: String,
    val isPaidOff: Boolean
)

private enum class ParticleType {
    POM_POM,
    CONFETTI_STRIP,
    SPARKLE
}

private data class Particle(
    val startXRatio: Float,
    val startYRatio: Float,
    val vx: Float,
    val vy: Float,
    val gravity: Float,
    val wobbleFreq: Float,
    val wobbleAmp: Float,
    val size: Float,
    val rotationSpeed: Float,
    val initialRotation: Float,
    val color: Color,
    val type: ParticleType
)

private val CELEBRATION_COLORS = listOf(
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Gold/Amber
    Color(0xFFEF4444), // Coral Red
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFF14B8A6), // Teal
    Color(0xFFFBBF24), // Yellow
    Color(0xFFFFFFFF)  // White Spark
)

@Composable
fun CelebrationOverlay(
    data: CelebrationData,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val progress = remember { Animatable(0f) }

    val particles = remember {
        List(60) {
            val rand = Random(it * 101 + 7)
            val type = when (it % 3) {
                0 -> ParticleType.POM_POM
                1 -> ParticleType.CONFETTI_STRIP
                else -> ParticleType.SPARKLE
            }
            Particle(
                startXRatio = rand.nextFloat(),
                startYRatio = rand.nextFloat() * 0.35f - 0.15f,
                vx = (rand.nextFloat() - 0.5f) * 450f,
                vy = rand.nextFloat() * 250f + 120f,
                gravity = rand.nextFloat() * 500f + 350f,
                wobbleFreq = rand.nextFloat() * 8f + 4f,
                wobbleAmp = rand.nextFloat() * 40f + 10f,
                size = rand.nextFloat() * 12f + 8f,
                rotationSpeed = (rand.nextFloat() - 0.5f) * 720f,
                initialRotation = rand.nextFloat() * 360f,
                color = CELEBRATION_COLORS[rand.nextInt(CELEBRATION_COLORS.size)],
                type = type
            )
        }
    }

    LaunchedEffect(data) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}

        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3500, easing = LinearEasing)
        )
        // Auto-dismiss after celebration finishes
        delay(1200)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
                .testTag("celebration_overlay"),
            contentAlignment = Alignment.Center
        ) {
            // Full-screen canvas drawing falling celebratory pompons and confetti
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = progress.value
                val w = size.width
                val h = size.height

                particles.forEach { p ->
                    val x = (p.startXRatio * w) + (p.vx * t) + (sin(t * p.wobbleFreq) * p.wobbleAmp)
                    val y = (p.startYRatio * h) + (p.vy * t) + (0.5f * p.gravity * t * t)
                    val currentRotation = p.initialRotation + p.rotationSpeed * t

                    // Alpha fades out towards the end
                    val alpha = if (t > 0.75f) ((1f - t) / 0.25f).coerceIn(0f, 1f) else 1f
                    val particleColor = p.color.copy(alpha = alpha)

                    if (y in -50f..(h + 60f) && x in -50f..(w + 50f)) {
                        when (p.type) {
                            ParticleType.POM_POM -> {
                                drawPomPom(
                                    center = Offset(x, y),
                                    baseRadius = p.size,
                                    color = particleColor,
                                    rotation = currentRotation
                                )
                            }
                            ParticleType.CONFETTI_STRIP -> {
                                rotate(degrees = currentRotation, pivot = Offset(x, y)) {
                                    drawRect(
                                        color = particleColor,
                                        topLeft = Offset(x - p.size * 0.4f, y - p.size),
                                        size = Size(p.size * 0.8f, p.size * 2f)
                                    )
                                }
                            }
                            ParticleType.SPARKLE -> {
                                drawSparkle(
                                    center = Offset(x, y),
                                    radius = p.size * 1.1f,
                                    color = particleColor
                                )
                            }
                        }
                    }
                }
            }

            // Central Celebration Card
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp)
                        .clickable(enabled = false) {}
                        .testTag("celebration_card"),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    border = BorderStroke(1.5.dp, if (data.isPaidOff) EmeraldPrimary else AccentAmber)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Fluffy celebration icon / pompons
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(
                                    if (data.isPaidOff) EmeraldPrimary.copy(alpha = 0.2f)
                                    else AccentAmber.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (data.isPaidOff) "🥳" else "🎉",
                                fontSize = 42.sp
                            )
                        }

                        // Headline
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (data.isPaidOff) EmeraldLight else AccentAmber,
                                textAlign = TextAlign.Center
                            )
                            if (data.isPaidOff) {
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = EmeraldPrimary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "¡LIBRE DE ESTA DEUDA! 🎊",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Detailed message
                        Text(
                            text = data.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondaryDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        // Financial Summary Pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SlateBackground,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Compromiso:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = data.commitmentTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryDark
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Monto pagado:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                    Text(
                                        text = "${data.currencySymbol} ${String.format(Locale.US, "%.2f", data.amountPaid)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Estado actual:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondaryDark
                                    )
                                    if (data.isPaidOff) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldLight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = "100% Saldada",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldLight
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Resta: ${data.currencySymbol} ${String.format(Locale.US, "%.2f", data.remainingAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentAmber
                                        )
                                    }
                                }
                            }
                        }

                        // Close / Celebrate Button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("confirm_celebration_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (data.isPaidOff) EmeraldPrimary else AccentAmber
                            )
                        ) {
                            Text(
                                text = if (data.isPaidOff) "¡Genial, adiós deuda! 🥳" else "¡Continuar! 🎊",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Draw a fluffy celebratory pom-pom with center puff + petal puffs around it
private fun DrawScope.drawPomPom(
    center: Offset,
    baseRadius: Float,
    color: Color,
    rotation: Float
) {
    // Center puff
    drawCircle(
        color = color,
        radius = baseRadius * 0.75f,
        center = center
    )
    // 5 outer puffs around it to give a real fluffy pom-pom look
    val puffRadius = baseRadius * 0.45f
    val distance = baseRadius * 0.65f
    val radRot = (rotation * PI / 180.0).toFloat()
    for (i in 0 until 5) {
        val angle = radRot + (i * 2.0 * PI / 5.0).toFloat()
        val px = center.x + cos(angle) * distance
        val py = center.y + sin(angle) * distance
        drawCircle(
            color = color.copy(alpha = (color.alpha * 0.9f).coerceIn(0f, 1f)),
            radius = puffRadius,
            center = Offset(px, py)
        )
    }
}

// Draw a 4-pointed sparkle star
private fun DrawScope.drawSparkle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path = path, color = color)
}
