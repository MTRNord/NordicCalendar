package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _nominatimUrl = MutableStateFlow(
        prefs.getString(
            "nominatim_url",
            "https://nominatim.openstreetmap.org/search"
        )!!
    )
    val nominatimUrl: StateFlow<String> = _nominatimUrl.asStateFlow()

    fun setNominatimUrl(url: String) {
        prefs.edit().putString("nominatim_url", url).apply()
        _nominatimUrl.value = url
    }
}
