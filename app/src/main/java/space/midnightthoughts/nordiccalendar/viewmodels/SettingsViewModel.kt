package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * SettingsViewModel is the ViewModel responsible for managing settings-related data and operations.
 * It currently does not hold any specific state or logic but serves as a placeholder for future settings management.
 *
 * @property context The application context injected by Hilt.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

}
