package com.zobaer53.plantapp.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.toArgb
import androidx.core.net.toUri
import com.zobaer53.plantapp.ui.theme.PrimaryLight
import com.zobaer53.plantapp.ui.theme.SurfaceContainerDark

/** Opens third-party disease guides in the user's browser via a Custom Tab, so the app embeds no browser and needs no INTERNET permission. */
object GuideLauncher {

    /**
     * Opens [url] in a Custom Tab. Only absolute https URLs with a host are launched; returns false
     * (the caller shows a message) for anything else or when no browser can handle the URL.
     */
    fun open(context: Context, url: String): Boolean {
        val uri = url.trim().toUri()
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return false

        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder().setToolbarColor(PrimaryLight.toArgb()).build(),
            )
            .setColorSchemeParams(
                CustomTabsIntent.COLOR_SCHEME_DARK,
                CustomTabColorSchemeParams.Builder().setToolbarColor(SurfaceContainerDark.toArgb()).build(),
            )
            .build()
        if (context !is Activity) {
            customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
