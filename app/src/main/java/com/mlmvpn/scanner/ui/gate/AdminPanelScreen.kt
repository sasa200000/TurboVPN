package com.mlmvpn.scanner.ui.gate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlmvpn.scanner.subscription.SubscriptionStore
import com.mlmvpn.scanner.ui.settings.Ios
import com.mlmvpn.scanner.ui.settings.IosScreen
import com.mlmvpn.scanner.ui.settings.SettingsGroup
import com.mlmvpn.scanner.ui.settings.SettingsRow
import com.mlmvpn.scanner.ui.settings.SettingsSectionHeader
import com.mlmvpn.scanner.ui.settings.SettingsFooter
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val isFa = com.mlmvpn.scanner.utils.AppLocaleManager.isFarsi()
    var codes by remember { mutableStateOf(SubscriptionStore.allCodes(context)) }
    var lastCode by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun refresh() { codes = SubscriptionStore.allCodes(context) }

    IosScreen(title = if (isFa) "پنل مدیریت کدها" else "Code Admin Panel", onBack = onBack, backLabel = if (isFa) "بازگشت" else "Back") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                Spacer(Modifier.height(14.dp))
                SettingsSectionHeader(if (isFa) "ساخت کد جدید" else "Generate code")
                SettingsGroup {
                    SubscriptionStore.TIERS.keys.forEachIndexed { index, tier ->
                        if (index > 0) Separator()
                        val fa = SubscriptionStore.TIER_LABELS_FA[tier]
                        val en = SubscriptionStore.TIER_LABELS_EN[tier]
                        SettingsRow(
                            title = if (isFa) (fa ?: tier) else (en ?: tier),
                            value = if (isFa) "کد بساز" else "make code",
                            showChevron = false,
                            onClick = {
                                val code = SubscriptionStore.generateCode(context, tier)
                                if (code != null) {
                                    lastCode = code
                                    notice = null
                                    refresh()
                                }
                            },
                        )
                    }
                }
            }

            lastCode?.let {
                item {
                    Spacer(Modifier.height(12.dp))
                    SettingsSectionHeader(if (isFa) "آخرین کد ساخته‌شده (کپی کنید)" else "Last generated code (copy it)")
                    SettingsGroup {
                        SettingsRow(
                            title = it,
                            value = if (isFa) "کپی" else "copy",
                            showChevron = false,
                            onClick = {
                                runCatching {
                                    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    cm.setPrimaryClip(android.content.ClipData.newPlainText("code", it))
                                    notice = if (isFa) "کپی شد!" else "Copied!"
                                }
                            },
                        )
                    }
                }
            }

            notice?.let {
                item {
                    Text(
                        it,
                        color = Ios.Green,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
            }

            item {
                Spacer(Modifier.height(14.dp))
                SettingsSectionHeader(if (isFa) "کدهای موجود" else "All codes")
                SettingsGroup {
                    if (codes.length() == 0) {
                        SettingsRow(
                            title = if (isFa) "هنوز کدی ساخته نشده" else "No codes yet",
                            showChevron = false,
                        )
                    } else {
                        (0 until codes.length()).forEach { index ->
                            val e = codes.getJSONObject(index)
                            val code = e.optString("code", "")
                            val tier = e.optString("tier", "")
                            val active = e.optBoolean("active", false)
                            val exp = e.optLong("expires_at", 0L)
                            val created = e.optLong("created_at", 0L)
                            if (index > 0) Separator()

                            SettingsRow(
                                title = code,
                                subtitle = buildString {
                                    val tierLabel = if (isFa) (SubscriptionStore.TIER_LABELS_FA[tier] ?: tier) else (SubscriptionStore.TIER_LABELS_EN[tier] ?: tier)
                                    append(tierLabel)
                                    append(" · ")
                                    append(if (active) (if (isFa) "فعال" else "active") else (if (isFa) "لغو شده" else "revoked"))
                                    append(" · ")
                                    append(dateStr(created, isFa))
                                },
                                value = if (isFa) (if (active) "لغو" else "—") else (if (active) "revoke" else "—"),
                                showChevron = false,
                                onClick = {
                                    if (active) {
                                        SubscriptionStore.revokeCode(context, code)
                                        refresh()
                                    }
                                },
                            )
                        }
                    }
                }
                SettingsFooter(if (isFa) "با لغو کد، کاربر دیگر نمی‌تواند آن را فعال کند." else "Revoking makes a code unusable.")
            }
        }
    }
}

private fun dateStr(ms: Long, fa: Boolean): String = runCatching {
    val fmt = SimpleDateFormat(if (fa) "yyyy/MM/dd HH:mm" else "MM-dd HH:mm", Locale.US)
    fmt.format(Date(ms))
}.getOrDefault("—")
