package com.mlmvpn.scanner.ui.gate

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlmvpn.scanner.R
import com.mlmvpn.scanner.subscription.SubscriptionStore
import com.mlmvpn.scanner.ui.settings.Ios
import com.mlmvpn.scanner.ui.theme.Primary
import com.mlmvpn.scanner.utils.S

private val NEON_COLORS = listOf(
    Color(0xFFFF006E),
    Color(0xFFFF6B00),
    Color(0xFFFFD300),
    Color(0xFF00FF87),
    Color(0xFF00CFFF),
    Color(0xFF7B61FF),
    Color(0xFFFF006E),
)

@Composable
fun SubscriptionGateScreen(
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var codeInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val isFa = remember { com.mlmvpn.scanner.utils.AppLocaleManager.isFarsi() }

    // Color-cycling clock for neon effect — read in draw phase, not recomposition.
    val neonClock = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val origin = System.nanoTime()
        while (true) {
            val elapsed = (System.nanoTime() - origin) / 1_000_000f
            neonClock.floatValue = elapsed * 0.08f  // ~one full cycle per ~47s
            kotlinx.coroutines.delay(50)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A1A)),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Neon title ──
            NeonText(
                text = "TurboVPN",
                colorCycleOffset = neonClock.floatValue,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isFa) "کد اشتراک را وارد کنید" else "Enter your subscription code",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )

            Spacer(Modifier.height(20.dp))

            // ── Neon telegram admin button ──
            NeonButtonRow(
                label = "ارتباط با ادمین",
                sublabel = "@sedef3345",
                icon = "telegram",
                colorCycleOffset = neonClock.floatValue + 2.1f,
                onClick = {
                    try {
                        uriHandler.openUri("https://t.me/sedef3345")
                    } catch (_: Exception) {
                        try { uriHandler.openUri("tg://resolve?domain=sedef3345") } catch (_: Exception) {}
                    }
                },
            )

            Spacer(Modifier.height(18.dp))

            // ── Neon code input box (compact: one line, fixed height) ──
            NeonBox(
                colorCycleOffset = neonClock.floatValue + 1.0f,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase().take(20)
                        error = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    placeholder = {
                        Text(
                            "OIL-XXXXXXXXXX",
                            color = Color.White.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = Primary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp,
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!loading && codeInput.isNotBlank()) {
                                loading = true
                                val ok = SubscriptionStore.activateCode(context, codeInput)
                                loading = false
                                if (ok) onSuccess()
                                else error = if (isFa) "کد نامعتبر یا منقضی شده" else "Invalid or expired code"
                            }
                        },
                    ),
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = Ios.Destructive,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Submit button ──
            Button(
                onClick = {
                    if (loading || codeInput.isBlank()) return@Button
                    loading = true
                    val ok = SubscriptionStore.activateCode(context, codeInput)
                    loading = false
                    if (ok) onSuccess()
                    else error = if (isFa) "کد نامعتبر یا منقضی شده" else "Invalid or expired code"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = Color.White,
                    disabledContainerColor = Primary.copy(alpha = 0.4f),
                    disabledContentColor = Color.White.copy(alpha = 0.6f),
                ),
                enabled = !loading && codeInput.isNotBlank(),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (isFa) "فعالسازی" else "Activate",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tier info ──
            Text(
                text = if (isFa) "کدهای موجود: ۳ روزه · ۱۴ روزه · ۱ ماه · ۲ ماه · ۳ ماه · ۶ ماه · ۱ ساله"
                         else "Available: 3 Days · 14 Days · 1 Month · 2 Months · 3 Months · 6 Months · 1 Year",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Neon animated text ──────────────────────────────────────────────────────

@Composable
private fun NeonText(
    text: String,
    colorCycleOffset: Float,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
) {
    val infinite = rememberInfiniteTransition(label = "neonText")
    val phase by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "neonPhase",
    )

    val colors = remember { NEON_COLORS }
    fun colorAt(index: Int): Color {
        val pos = ((index * 2 + phase * NEON_COLORS.size + colorCycleOffset) % NEON_COLORS.size)
        val i1 = pos.toInt() % NEON_COLORS.size
        val i2 = (i1 + 1) % NEON_COLORS.size
        val t = pos - pos.toInt()
        return lerp(colors[i1], colors[i2], t)
    }

    Text(
        text = buildAnnotatedString {
            text.forEachIndexed { i, c ->
                withStyle(SpanStyle(color = colorAt(i))) {
                    append(c)
                }
            }
        },
        fontSize = fontSize,
        fontWeight = fontWeight,
    )
}

// ── Neon animated button row ─────────────────────────────────────────────────

@Composable
private fun NeonButtonRow(
    label: String,
    sublabel: String,
    icon: String,
    colorCycleOffset: Float,
    onClick: () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "neonBtn")
    val shift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "neonShift",
    )
    val neonColor by animateColorAsState(
        targetValue = NEON_COLORS[((shift * NEON_COLORS.size + colorCycleOffset).toInt()) % NEON_COLORS.size],
        animationSpec = tween(200),
        label = "neonBtnColor",
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1A35),
    ) {
        Box {
            // Glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            neonColor.copy(alpha = 0.35f),
                            Color.Transparent,
                            neonColor.copy(alpha = 0.2f),
                        ),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✈️", fontSize = 22.sp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = neonColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(sublabel, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Text("→", color = neonColor, fontSize = 20.sp)
            }
        }
    }
}

// ── Neon animated box for code input ────────────────────────────────────────

@Composable
private fun NeonBox(
    colorCycleOffset: Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val infinite = rememberInfiniteTransition(label = "neonBox")
    val shift by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2500, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "neonBoxShift",
    )
    val neonColor by animateColorAsState(
        targetValue = NEON_COLORS[((shift * NEON_COLORS.size + colorCycleOffset).toInt()) % NEON_COLORS.size],
        animationSpec = tween(250),
        label = "neonBoxColor",
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF111128),
    ) {
        Box {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stroke = 1.5.dp.toPx()
                // Animate color position
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            neonColor,
                            neonColor.copy(alpha = 0.3f),
                            neonColor.copy(alpha = 0.6f),
                            neonColor,
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, 0f),
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                )
            }
            content()
        }
    }
}

// ── Utility ─────────────────────────────────────────────────────────────────

private fun lerp(a: Color, b: Color, t: Float): Color =
    Color(
        red   = a.red   + (b.red   - a.red)   * t,
        green = a.green + (b.green - a.green) * t,
        blue  = a.blue  + (b.blue  - a.blue)  * t,
        alpha = a.alpha + (b.alpha - b.alpha) * t,
    )
