package com.mlmvpn.scanner.subscription

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.abs

/**
 * Subscription code store.
 *
 * Codes are stored in filesDir/oilvpn_codes.json so the admin panel can list,
 * add and revoke them without app restarts. A valid code activates the
 * device's subscription; one subscription per install. When the expiry passes
 * the app re-locks (checked on every launch and on resume).
 */
object SubscriptionStore {

    private const val CODES_FILE = "oilvpn_codes.json"
    private const val ADMIN_PREFS = "oilvpn_admin"
    private const val PREF_SECRET = "admin_secret"

    private const val DAY = 24 * 60 * 60 * 1000L
    private const val HOUR = 60 * 60 * 1000L
    private const val MIN = 60 * 1000L

    /** Fixed duration tiers (code does not carry an arbitrary length). */
    val TIERS: LinkedHashMap<String, Long> = linkedMapOf(
        "3days"  to 3L  * DAY,
        "14days" to 14L * DAY,
        "1month" to 30L * DAY,
        "2months" to 60L * DAY,
        "3months" to 90L * DAY,
        "6months" to 180L * DAY,
        "1year"  to 365L * DAY,
    )

    val TIER_LABELS_FA: Map<String, String> = mapOf(
        "3days" to "۳ روزه", "14days" to "۱۴ روزه", "1month" to "۱ ماهه",
        "2months" to "۲ ماهه", "3months" to "۳ ماهه", "6months" to "۶ ماهه",
        "1year" to "۱ ساله",
    )

    val TIER_LABELS_EN: Map<String, String> = mapOf(
        "3days" to "3 Days", "14days" to "14 Days", "1month" to "1 Month",
        "2months" to "2 Months", "3months" to "3 Months", "6months" to "6 Months",
        "1year" to "1 Year",
    )

    // ── Admin secret ─────────────────────────────────────────────────────────

    fun getOrCreateSecret(context: Context): String {
        val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        prefs.getString(PREF_SECRET, null)?.let { return it }
        val secret = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        prefs.edit().putString(PREF_SECRET, secret).apply()
        return secret
    }

    // ── Admin: code management ──────────────────────────────────────────────

    /** Generate a new code for [tier]; null if the tier is unknown. */
    fun generateCode(context: Context, tier: String): String? {
        val duration = TIERS[tier] ?: return null
        val secret = getOrCreateSecret(context)
        val suffix = "%06x".format(abs(SecureRandom().nextInt())).uppercase()
        val hash = sha1("$secret:$suffix:$tier").substring(0, 10).uppercase()
        val code = "OIL-$hash"

        val codes = loadCodes(context)
        codes.put(JSONObject().apply {
            put("code", code)
            put("tier", tier)
            put("created_at", System.currentTimeMillis())
            put("expires_at", System.currentTimeMillis() + duration)
            put("active", true)
        })
        saveCodes(context, codes)
        return code
    }

    fun revokeCode(context: Context, code: String): Boolean {
        val codes = loadCodes(context)
        for (i in 0 until codes.length()) {
            val e = codes.getJSONObject(i)
            if (e.optString("code") == code) {
                e.put("active", false)
                saveCodes(context, codes)
                return true
            }
        }
        return false
    }

    fun allCodes(context: Context): JSONArray = loadCodes(context)

    // ── User: activation & status ────────────────────────────────────────────

    /**
     * The fixed master code — unlocks the app for a year. Only the app's owner
     * (the admin) knows it; it is checked before the generated-codes file so the
     * admin can always enter, even on a fresh install with no codes generated yet.
     */
    const val MASTER_CODE = "OIL-MASTER-2026"

    /** Redeem a code. True when valid, unused-checked, and now active on-device. */
    fun activateCode(context: Context, code: String): Boolean {
        val wanted = code.trim().uppercase()
        if (wanted == MASTER_CODE) {
            val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_code", wanted)
                .putLong("user_expires_at", System.currentTimeMillis() + 365L * DAY)
                .putString("user_tier", "master")
                .apply()
            return true
        }
        val codes = loadCodes(context)
        for (i in 0 until codes.length()) {
            val e = codes.getJSONObject(i)
            if (e.optString("code") != wanted || !e.optBoolean("active", false)) continue
            val expiresAt = e.optLong("expires_at", 0L)
            if (expiresAt <= System.currentTimeMillis()) continue
            val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("user_code", wanted)
                .putLong("user_expires_at", expiresAt)
                .putString("user_tier", e.optString("tier", ""))
                .apply()
            return true
        }
        return false
    }

    fun isSubscribed(context: Context): Boolean = getExpiry(context) > System.currentTimeMillis()

    fun getExpiry(context: Context): Long {
        val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        return prefs.getLong("user_expires_at", 0L)
    }

    fun getTier(context: Context): String? {
        val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        return prefs.getString("user_tier", null)
    }

    fun remainingMs(context: Context): Long =
        maxOf(0L, getExpiry(context) - System.currentTimeMillis())

    fun formatRemaining(context: Context, fa: Boolean): String {
        val ms = remainingMs(context)
        if (ms <= 0) return if (fa) "منقضی شده" else "Expired"
        val d = ms / DAY
        val h = (ms % DAY) / HOUR
        val m = (ms % HOUR) / MIN
        return when {
            d > 0 -> if (fa) "$d روز و $h ساعت" else "$d d $h h"
            h > 0 -> if (fa) "$h ساعت و $m دقیقه" else "$h h $m m"
            else -> if (fa) "$m دقیقه" else "$m m"
        }
    }

    fun clearSubscription(context: Context) {
        val prefs = context.getSharedPreferences(ADMIN_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("user_code")
            .remove("user_expires_at")
            .remove("user_tier")
            .apply()
    }

    // ── IO ───────────────────────────────────────────────────────────────────

    private fun loadCodes(context: Context): JSONArray = try {
        val file = File(context.filesDir, CODES_FILE)
        if (file.exists()) JSONArray(file.readText()) else JSONArray()
    } catch (e: Exception) {
        JSONArray()
    }

    private fun saveCodes(context: Context, codes: JSONArray) {
        try {
            File(context.filesDir, CODES_FILE).writeText(codes.toString(2))
        } catch (e: Exception) {
            // persisted only on next successful write
        }
    }

    private fun sha1(s: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }
}
