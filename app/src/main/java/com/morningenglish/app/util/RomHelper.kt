package com.morningenglish.app.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Detects the device's ROM manufacturer and provides deep links to each
 * vendor's specific "auto-start" management screen.
 *
 * Why this matters: on MIUI / EMUI / ColorOS / FuntouchOS, BOOT_COMPLETED
 * broadcasts are silently blocked unless the app is in the user's
 * auto-start whitelist. Without this, the daily alarm won't fire after
 * device reboot.
 */
object RomHelper {

    enum class RomType(val displayName: String) {
        XIAOMI("小米 MIUI"),
        HUAWEI("华为 EMUI/HarmonyOS"),
        OPPO("OPPO ColorOS"),
        VIVO("vivo FuntouchOS"),
        ONEPLUS("一加 HydrogenOS"),
        SAMSUNG("三星 One UI"),
        MEIZU("魅族 Flyme"),
        LETV("乐视"),
        STOCK("原生/AOSP")
    }

    fun detect(context: Context): RomType {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        return when {
            manufacturer.contains("xiaomi") || brand.contains("xiaomi")
                || brand.contains("redmi") -> RomType.XIAOMI
            manufacturer.contains("huawei") || brand.contains("huawei")
                || brand.contains("honor") -> RomType.HUAWEI
            manufacturer.contains("oppo") || brand.contains("oppo")
                || brand.contains("realme") -> RomType.OPPO
            manufacturer.contains("vivo") || brand.contains("vivo") -> RomType.VIVO
            manufacturer.contains("oneplus") || brand.contains("oneplus") -> RomType.ONEPLUS
            manufacturer.contains("samsung") || brand.contains("samsung") -> RomType.SAMSUNG
            manufacturer.contains("meizu") -> RomType.MEIZU
            manufacturer.contains("letv") -> RomType.LETV
            else -> RomType.STOCK
        }
    }

    /**
     * Try to open the vendor-specific auto-start settings.
     * Uses an explicit ComponentName for the known activities, falls back
     * to generic ACTION_POWER_USAGE_SUMMARY if the OEM changed the package.
     */
    fun openAutoStartSettings(context: Context, romType: RomType) {
        val intent = when (romType) {
            RomType.XIAOMI -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            RomType.HUAWEI -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            RomType.OPPO -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            }
            RomType.VIVO -> Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            RomType.ONEPLUS -> Intent().apply {
                component = ComponentName(
                    "com.oneplus.security",
                    "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                )
            }
            RomType.MEIZU -> Intent().apply {
                component = ComponentName(
                    "com.meizu.safe",
                    "com.meizu.safe.security.SHOW_APPSEC"
                )
            }
            RomType.LETV -> Intent().apply {
                component = ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            }
            RomType.SAMSUNG, RomType.STOCK -> Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // OEM changed the component name — fall back to app settings
            val fallback = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) { /* give up */ }
        }
    }
}