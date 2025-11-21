package com.example.braintrap.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.example.braintrap.data.model.AppCategory
import com.example.braintrap.data.model.AppInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoProvider @Inject constructor(
    private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager
    
    // Common social media app package names
    private val socialMediaPackages = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.snapchat.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.facebook.orca", // Messenger
        "com.youtube.android",
        "com.reddit.frontpage",
        "com.linkedin.android",
        "com.pinterest",
        "com.whatsapp",
        "com.tencent.mm", // WeChat
        "com.viber.voip",
        "com.discord"
    )
    
    fun getInstalledApps(): List<AppInfo> {
        val apps = mutableListOf<AppInfo>()
        val installedPackages = packageManager.getInstalledPackages(0)
        
        for (packageInfo in installedPackages) {
            val appInfo = packageInfo.applicationInfo ?: continue
            
            // Skip system apps and this app itself
            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                packageInfo.packageName == context.packageName) {
                continue
            }
            
            try {
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                val icon = packageManager.getApplicationIcon(packageInfo.packageName)
                val category = getCategoryForPackage(packageInfo.packageName)
                
                apps.add(
                    AppInfo(
                        packageName = packageInfo.packageName,
                        appName = appName,
                        icon = icon,
                        isBlocked = false,
                        category = category
                    )
                )
            } catch (e: Exception) {
                // Skip apps that can't be loaded
            }
        }
        
        return apps.sortedBy { it.appName }
    }
    
    fun getSocialMediaApps(): List<AppInfo> {
        return getInstalledApps().filter { it.category == AppCategory.SOCIAL_MEDIA }
    }
    
    private fun getCategoryForPackage(packageName: String): AppCategory {
        return when {
            socialMediaPackages.contains(packageName) -> AppCategory.SOCIAL_MEDIA
            packageName.contains("game") || packageName.contains("play") -> AppCategory.GAMES
            packageName.contains("video") || packageName.contains("music") -> AppCategory.ENTERTAINMENT
            packageName.contains("productivity") || packageName.contains("office") -> AppCategory.PRODUCTIVITY
            else -> AppCategory.OTHER
        }
    }
}

