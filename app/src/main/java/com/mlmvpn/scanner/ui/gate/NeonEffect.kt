package com.mlmvpn.scanner.ui.gate

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext

/** Seven-colour neon palette the user asked for (rainbow, animated). */
val NEON_RAINBOW = listOf(
    Color(0xFFFF006E), Color(0xFFFF6B00), Color(0xFFFFD300), Color(0xFF00FF87),
    Color(0xFF00CFFF), Color(0xFF7B61FF), Color(0xFFFF2D55),
)

/** The named presets the settings page offers. */
enum class NeonStyle(val label: String, val colors: List<Color>) {
    RAINBOW("هفت‌رنگ", NEON_RAINBOW),
    ICE("یخی", listOf(Color(0xFF00CFFF), Color(0xFF7B61FF), Color(0xFF00FFD1))),
    EMBER("آتشین", listOf(Color(0xFFFF6B00), Color(0xFFFFD300), Color(0xFFFF006E))),
    MINT("نعنایی", listOf(Color(0xFF00FF87), Color(0xFF00CFFF), Color(0xFF00FFD1))),
    OFF("خاموش", emptyList()),
}

/** Neon effect preferences; shared by the gate screen, rows and the connect button. */
object NeonPrefs {
    private const val FILE = "neon_prefs"
    private const val KEY_STYLE = "style"

    fun style(context: android.content.Context): NeonStyle {
        val s = context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .getString(KEY_STYLE, NeonStyle.RAINBOW.name) ?: NeonStyle.RAINBOW.name
        return runCatching { NeonStyle.valueOf(s) }.getOrDefault(NeonStyle.RAINBOW)
    }

    fun setStyle(context: android.content.Context, style: NeonStyle) {
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            .edit().putString(KEY_STYLE, style.name).apply()
    }
}

/**
 * A live neon border around whatever it wraps. Draws ONLY a border + a soft outer
 * glow — no full-screen wash — so it is safe around long lists of server rows.
 *
 * The colour cycles through the preset on a delay-loop clock read inside the draw
 * phase (same pattern as DriftingLights): a tick invalidates one display list, not
 * a recomposition, and it pauses while the app is backgrounded.
 */
@Composable
fun rememberNeonColors(context: android.content.Context): List<Color> {
    val style = remember { NeonPrefs.style(context) }
    return style.colors
}

/**
 * A composable colour that changes every second, cycling through the user's neon preset.
 * Returns Color.Unspecified when the preset is OFF, so callers can fall back to their
 * own static colour. Used for the connect ring after connecting, the traffic stats
 * and the live wave chart.
 */
@Composable
fun rememberSecondlyNeonColor(context: android.content.Context): Color {
    val colors = rememberNeonColors(context)
    if (colors.isEmpty()) return Color.Unspecified
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(colors) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            tick.intValue++
        }
    }
    val idx = tick.intValue % colors.size
    return colors[idx]
}

/** Smooth animated version: interpolates between colours over the second, never snaps. */
@Composable
fun rememberSmoothNeonColor(context: android.content.Context): Color {
    val colors = rememberNeonColors(context)
    if (colors.isEmpty()) return Color.Unspecified
    val tick = remember { mutableIntStateOf(0) }
    LaunchedEffect(colors) {
        while (true) {
            kotlinx.coroutines.delay(1_000)
            tick.intValue++
        }
    }
    return androidx.compose.animation.core.animateColorAsState(
        targetValue = colors[tick.intValue % colors.size],
        animationSpec = androidx.compose.animation.core.tween(950),
        label = "neonSecondly",
    ).value
}

@Composable
fun Modifier.neonBorder(
    colors: List<Color>,
    cornerRadius: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(12f),
    strokeWidth: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp(1f),
    glowAlpha: Float = 0.35f,
): Modifier {
    if (colors.isEmpty()) return this

    val clock = remember { mutableFloatStateOf(0f) }
    com.mlmvpn.scanner.ui.LaunchedWhileVisible(Unit) {
        val origin = System.nanoTime()
        while (true) {
            clock.floatValue = (System.nanoTime() - origin) / 1_000_000f * 0.001f  // full cycle ~7s
            kotlinx.coroutines.delay(50)
        }
    }

    return this.drawBehind {
        val t = clock.floatValue
        val pos = (t % 1f) * colors.size
        val i1 = pos.toInt() % colors.size
        val i2 = (i1 + 1) % colors.size
        val f = pos - pos.toInt()
        val c1 = colors[i1]
        val c2 = colors[i2]
        val mix = Color(
            c1.red + (c2.red - c1.red) * f,
            c1.green + (c2.green - c1.green) * f,
            c1.blue + (c2.blue - c1.blue) * f,
        )

        // soft outer glow
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(mix.copy(alpha = glowAlpha), Color.Transparent, mix.copy(alpha = glowAlpha * 0.6f)),
            ),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
        )
        // crisp border
        drawRoundRect(
            color = mix,
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            style = Stroke(width = strokeWidth.toPx()),
        )
    }
}

/**
 * Falling rain particles in neon colours, drawn around whatever it wraps — the connect
 * button's "rain of light". Particles drop, fade, and cycle through the preset's colours
 * every second; the clock is a delay loop read inside the draw phase (no recomposition),
 * paused while the screen is not shown.
 */
@Composable
fun Modifier.neonRain(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    particleCount: Int = 26,
): Modifier {
    if (colors.isEmpty()) return this

    val clock = remember { mutableFloatStateOf(0f) }
    com.mlmvpn.scanner.ui.LaunchedWhileVisible(Unit) {
        val origin = System.nanoTime()
        while (true) {
            clock.floatValue = (System.nanoTime() - origin) / 1_000_000f
            kotlinx.coroutines.delay(50)
        }
    }

    // Deterministic particle layout: x position, fall speed, size, colour phase.
    val seeds = remember(particleCount) {
        List(particleCount) { i ->
            val r = (i * 2654435761L % 1000) / 1000f
            Triple(r, 0.35f + (i % 7) * 0.11f, 1.2f + (i % 5) * 0.7f)
        }
    }

    return this.drawBehind {
        val t = clock.floatValue
        val h = size.height
        val w = size.width

        seeds.forEachIndexed { i, (x0, speed, size) ->
            // Colour cycles through the preset every ~1s per particle (staggered).
            val cycle = ((t * 0.001f) + i * 0.14f) % 1f
            val pos = cycle * colors.size
            val i1 = pos.toInt() % colors.size
            val i2 = (i1 + 1) % colors.size
            val f = pos - pos.toInt()
            val c1 = colors[i1]
            val c2 = colors[i2]
            val color = Color(
                c1.red + (c2.red - c1.red) * f,
                c1.green + (c2.green - c1.green) * f,
                c1.blue + (c2.blue - c1.blue) * f,
            )

            // Fall: y loops top→bottom; alpha fades near both ends so drops appear/disappear.
            val y = ((t * speed * 0.0006f) + x0) % 1f
            val alpha = (sin(y * Math.PI).toFloat()).coerceIn(0f, 1f) * 0.85f

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x0 * w, y * h),
                    radius = size * 6f,
                ),
                radius = size * 6f,
                center = Offset(x0 * w, y * h),
            )
        }
    }
}

private fun sin(x: Double): Float = kotlin.math.sin(x).toFloat()
