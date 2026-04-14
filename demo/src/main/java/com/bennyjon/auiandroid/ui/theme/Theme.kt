package com.bennyjon.auiandroid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.bennyjon.auiandroid.R
import com.bennyjon.auiandroid.ui.theme.warm.WarmDarkScheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmLightScheme
import com.bennyjon.auiandroid.ui.theme.warm.WarmTheme

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@Composable
fun AUIAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val supportsDynamicColors = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        supportsDynamicColors -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> WarmDarkScheme
        else -> WarmLightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (supportsDynamicColors) MaterialTheme.typography else WarmTheme.WarmTypography,
        content = content
    )
}
