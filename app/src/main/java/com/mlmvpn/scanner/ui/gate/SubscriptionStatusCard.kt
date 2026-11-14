package com.mlmvpn.scanner.ui.gate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mlmvpn.scanner.subscription.SubscriptionStore
import com.mlmvpn.scanner.ui.settings.Ios
import com.mlmvpn.scanner.ui.settings.SettingsGroup
import com.mlmvpn.scanner.ui.settings.SettingsRow
import com.mlmvpn.scanner.ui.settings.SettingsSectionHeader
import com.mlmvpn.scanner.ui.settings.SettingsFooter
import com.mlmvpn.scanner.ui.settings.IosScreen
import com.mlmvpn.scanner.ui.settings.SettingsSectionHeader
import kotlinx.coroutines.delay

/**
 * The "X days left" card on Settings. Ticks every minute so a long-lived session
 * rolls over honestly; recomposes on tier/expiry changes too.
 */
@Composable
fun SubscriptionStatusRow() {
    val context = LocalContext.current
    val isFa = com.mlmvpn.scanner.utils.AppLocaleManager.isFarsi()

    var remaining by remember { mutableStateOf(SubscriptionStore.formatRemaining(context, isFa)) }
    var expired by remember { mutableStateOf(!SubscriptionStore.isSubscribed(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = SubscriptionStore.formatRemaining(context, isFa)
            expired = !SubscriptionStore.isSubscribed(context)
            delay(30_000)
        }
    }

    SettingsSectionHeader(if (isFa) "اشتراک" else "Subscription")
    SettingsGroup {
        SettingsRow(
            title = if (isFa) "زمان باقی‌مانده" else "Time remaining",
            value = if (expired) (if (isFa) "تمام شده" else "Expired") else remaining,
            showChevron = false,
        )
    }
}

/**
 * The expanded card the Settings row group uses: remaining time + a row that opens
 * the admin panel. Kept here so the subscription UI lives in one file.
 */
@Composable
fun ExpandedSubscriptionCard(onOpenAdmin: () -> Unit) {
    val context = LocalContext.current
    val isFa = com.mlmvpn.scanner.utils.AppLocaleManager.isFarsi()

    var remaining by remember { mutableStateOf(SubscriptionStore.formatRemaining(context, isFa)) }
    var expired by remember { mutableStateOf(!SubscriptionStore.isSubscribed(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            remaining = SubscriptionStore.formatRemaining(context, isFa)
            expired = !SubscriptionStore.isSubscribed(context)
            delay(30_000)
        }
    }

    SettingsRow(
        title = if (isFa) "زمان باقی‌مانده اشتراک" else "Subscription time left",
        subtitle = if (expired) (if (isFa) "تمام شده — کد جدید بگیرید" else "Expired — get a new code") else remaining,
        icon = Icons.Default.Verified,
        tint = if (expired) Ios.Destructive else Ios.Green,
        showChevron = false,
    )
    androidx.compose.material3.Divider()
    SettingsRow(
        title = if (isFa) "پنل مدیریت کدها" else "Code admin panel",
        subtitle = if (isFa) "ساخت و لغو کد اشتراک" else "Generate and revoke codes",
        icon = Icons.Default.AdminPanelSettings,
        tint = Ios.Orange,
        onClick = onOpenAdmin,
    )
}

/** The neon-effect picker page, hosted by SettingsScreen's route stack. */
@Composable
fun NeonStylePage(onBack: () -> Unit) {
    val context = LocalContext.current
    val isFa = com.mlmvpn.scanner.utils.AppLocaleManager.isFarsi()
    var selected by remember { mutableStateOf(NeonPrefs.style(context)) }

    com.mlmvpn.scanner.ui.settings.IosScreen(
        title = if (isFa) "رقص نور نئونی" else "Neon Light Effect",
        onBack = onBack,
        backLabel = com.mlmvpn.scanner.store.tr("تنظیمات", "Settings"),
    ) {
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(14.dp))
        SettingsSectionHeader(if (isFa) "استایل نور" else "Effect style")
        SettingsGroup {
            NeonStyle.entries.forEach { style ->
                if (style != NeonStyle.OFF) {
                    androidx.compose.material3.Divider()
                }
                SettingsRow(
                    title = style.label,
                    value = if (selected == style) (if (isFa) "فعال" else "On") else null,
                    icon = Icons.Default.Colorize,
                    tint = if (isFa) style.colors.firstOrNull() ?: Ios.Gray else style.colors.firstOrNull() ?: Ios.Gray,
                    onClick = {
                        selected = style
                        NeonPrefs.setStyle(context, style)
                    },
                )
            }
            androidx.compose.material3.Divider()
            SettingsRow(
                title = if (isFa) "خاموش" else "Off",
                value = if (selected == NeonStyle.OFF) (if (isFa) "فعال" else "On") else null,
                icon = Icons.Default.HighlightOff,
                tint = Ios.Gray,
                onClick = {
                    selected = NeonStyle.OFF
                    NeonPrefs.setStyle(context, NeonStyle.OFF)
                },
            )
        }
        com.mlmvpn.scanner.ui.settings.SettingsFooter(
            if (isFa) "نور نئونی دور کد اشتراک، لینک تلگرام ادمین، سرورها و دکمه اتصال نشان داده می‌شود."
            else "Neon shows around the code box, the admin link, servers and the connect button."
        )
    }
}
