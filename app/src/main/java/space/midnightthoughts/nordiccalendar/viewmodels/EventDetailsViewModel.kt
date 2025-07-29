package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.dellisd.spatialk.geojson.Position
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event
import javax.inject.Inject

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("boundingbox") val boundingBox: List<String>? = null
)

data class BoundingBox(val south: Double, val north: Double, val west: Double, val east: Double)

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    private val _locationPosition = MutableStateFlow<Position?>(null)
    val locationPosition: StateFlow<Position?> = _locationPosition.asStateFlow()

    private val _locationBoundingBox = MutableStateFlow<BoundingBox?>(null)
    val locationBoundingBox: StateFlow<BoundingBox?> = _locationBoundingBox.asStateFlow()

    private val ktorClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                    explicitNulls = false
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 10000L
            socketTimeoutMillis = 15000L
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("Ktor", message)
                }
            }
            level = LogLevel.INFO
        }
    }

    init {
        val eventId = savedStateHandle.get<String>("eventId")?.toLongOrNull()
        if (eventId != null) {
            loadEvent(eventId)
        }
    }

    private fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getEventById(context, eventId)
            _event.value = event
        }
    }

    fun resolveLocation(address: String, locale: String = "de") {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val url =
                    prefs.getString("nominatim_url", "https://nominatim.openstreetmap.org/search")!!
                val response: List<NominatimResult> = ktorClient.get(url) {
                    url {
                        parameters.append("q", address)
                        parameters.append("format", "json")
                        parameters.append("addressdetails", "1")
                        parameters.append("limit", "1")
                        parameters.append("accept-language", locale)
                    }
                    headers.append("User-Agent", "nordic-calendar-app")
                }.body()
                if (response.isNotEmpty()) {
                    val pos = Position(response[0].lon.toDouble(), response[0].lat.toDouble())
                    _locationPosition.value = pos
                    val bbox = response[0].boundingBox
                    if (bbox != null && bbox.size == 4) {
                        // boundingbox: [south, north, west, east] as strings
                        _locationBoundingBox.value = BoundingBox(
                            south = bbox[0].toDouble(),
                            north = bbox[1].toDouble(),
                            west = bbox[2].toDouble(),
                            east = bbox[3].toDouble()
                        )
                    } else {
                        _locationBoundingBox.value = null
                    }
                } else {
                    _locationPosition.value = null
                    _locationBoundingBox.value = null
                    Log.w("EventDetailsViewModel", "No location found for address: $address")
                }
            } catch (e: Exception) {
                _locationPosition.value = null
                _locationBoundingBox.value = null
                Log.e("EventDetailsViewModel", "Error resolving location: ${e.message}")
            }
        }
    }
}
