package com.iterio.app.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.iterio.app.domain.model.AllowedApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * インストール済みアプリの情報を取得するヘルパークラス
 */
@Singleton
class InstalledAppsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager

    // キャッシュ関連
    private var cachedApps: List<AllowedApp>? = null
    private var cacheTimestamp = 0L
    private val CACHE_DURATION_MS = 5 * 60 * 1000L // 5分

    /**
     * インストール済みのランチャーアプリ一覧を取得
     * @param forceRefresh キャッシュを無視して再取得する場合はtrue
     */
    suspend fun getInstalledUserApps(forceRefresh: Boolean = false): List<AllowedApp> = withContext(Dispatchers.IO) {
        // キャッシュが有効な場合はキャッシュを返す
        // Note: ローカル変数に保持してレースコンディションを防止
        if (!forceRefresh && isCacheValid()) {
            cachedApps?.let { cache ->
                Timber.d("Returning cached apps (${cache.size} apps)")
                return@withContext cache
            }
        }

        Timber.d("Loading installed apps (forceRefresh=$forceRefresh)")
        val launchableApps = getLaunchableApps()
        Timber.d("Found ${launchableApps.size} launchable apps")
        val apps = launchableApps
            .mapNotNull { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    AllowedApp(
                        packageName = packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = packageManager.getApplicationIcon(packageName),
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    )
                } catch (e: PackageManager.NameNotFoundException) {
                    Timber.w(e, "App not found: $packageName")
                    null
                } catch (e: Exception) {
                    Timber.e(e, "Unexpected error loading app: $packageName")
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }

        // キャッシュを更新
        cachedApps = apps
        cacheTimestamp = System.currentTimeMillis()
        Timber.d("Cached ${apps.size} apps")

        apps
    }

    /**
     * 指定されたパッケージ名リストからAllowedAppリストを取得
     */
    suspend fun getAppsByPackageNames(packageNames: List<String>): List<AllowedApp> = withContext(Dispatchers.IO) {
        packageNames.mapNotNull { packageName ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                AllowedApp(
                    packageName = packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    icon = packageManager.getApplicationIcon(packageName),
                    isSystemApp = isSystemApp(packageName)
                )
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.w(e, "App not found: $packageName")
                null
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error loading app: $packageName")
                null
            }
        }
    }

    /**
     * パッケージが存在するか検証し、存在するもののみを返す
     * 削除されたアプリを許可リストから除外するために使用
     */
    fun validatePackages(packages: List<String>): List<String> {
        return packages.filter { packageName ->
            try {
                packageManager.getApplicationInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.i("Removed deleted app from list: $packageName")
                false
            }
        }
    }

    /**
     * ランチャーから起動可能なアプリのパッケージ名一覧を取得
     */
    private fun getLaunchableApps(): List<String> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(mainIntent, 0)
        }
        return resolveInfos
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName } // 自分自身を除外
    }

    /**
     * システムアプリかどうかを判定
     */
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * キャッシュが有効かどうかを判定
     */
    private fun isCacheValid(): Boolean {
        return cachedApps != null &&
               System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION_MS
    }

    /**
     * キャッシュを無効化する
     */
    fun invalidateCache() {
        cachedApps = null
        cacheTimestamp = 0L
        Timber.d("App cache invalidated")
    }
}
