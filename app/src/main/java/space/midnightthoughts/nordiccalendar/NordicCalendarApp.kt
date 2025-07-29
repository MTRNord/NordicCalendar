package space.midnightthoughts.nordiccalendar

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class NordicCalendarApp : Application()

fun getCurrentAppLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val appLocales: LocaleList = localeManager.applicationLocales
        if (!appLocales.isEmpty) appLocales[0] else Locale.getDefault()
    } else {
        Locale.getDefault()
    }
}
