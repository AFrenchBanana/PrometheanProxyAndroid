package com.promethean.proxy.ui.theme.main.dashboard

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.promethean.proxy.di.SettingsPrefs
import com.promethean.proxy.network.NetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.google.gson.annotations.SerializedName
data class ConnectionResponse(
    @SerializedName("beacons") val beacons: List<Beacon> = emptyList(),
    @SerializedName("sessions") val sessions: List<Session> = emptyList()
)

data class Beacon(
    @SerializedName("hostname") val hostname: String,
    @SerializedName("address") val address: String,
    @SerializedName("operating_system") val operatingSystem: String
)

data class Session(
    @SerializedName("hostname") val hostname: String,
    @SerializedName("address") val address: List<Any>,
    @SerializedName("operating_system") val operatingSystem: String?
)

enum class FilterMode { BEACON, SESSION, ALL }

@HiltViewModel
class DashboardViewModel @Inject constructor(
    val networkManager: NetworkManager,
    @SettingsPrefs private val prefs: SharedPreferences
) : ViewModel() {
    var connectionData by mutableStateOf<ConnectionResponse?>(null)
    var isLoading by mutableStateOf(true)

    suspend fun fetchData() {
        isLoading = true
        val json = withContext(Dispatchers.IO) {
            networkManager.getWithToken("/api/connections")
        }
        try {
            connectionData = Gson().fromJson(json, ConnectionResponse::class.java)
        } catch (e: Exception) {
            // Handle parsing error
        }
        isLoading = false
    }
}

class Dashboard {

    @Composable
    fun DashboardUI(
        viewModel: DashboardViewModel = viewModel()
    ) {
        var filterMode by remember { mutableStateOf(FilterMode.ALL) }

        LaunchedEffect(Unit) {
            viewModel.fetchData()
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { filterMode = FilterMode.BEACON },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == FilterMode.BEACON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Beacons")
                }
                Button(
                    onClick = { filterMode = FilterMode.SESSION },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == FilterMode.SESSION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Sessions")
                }
                Button(
                    onClick = { filterMode = FilterMode.ALL},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (filterMode == FilterMode.SESSION && filterMode == FilterMode.BEACON) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isLoading) {
                CircularProgressIndicator()
            } else {
                ConnectionTable(viewModel.connectionData, filterMode)
            }
        }
    }

    @Composable
    fun ConnectionTable(data: ConnectionResponse?, mode: FilterMode) {
        Column {
            // Table Header
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Hostname", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Address:Port", Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                Text("OS", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            }
            HorizontalDivider()

            LazyColumn {
                if (mode == FilterMode.BEACON) {
                    items(data?.beacons ?: emptyList()) { beacon ->
                        TableRow(beacon.hostname, beacon.address, beacon.operatingSystem)
                    }
                } else if (mode == FilterMode.SESSION){
                    items(data?.sessions ?: emptyList()) { session ->
                        val addr = if (session.address.size >= 2) "${session.address[0]}:${session.address[1]}" else "N/A"
                        TableRow(session.hostname, addr, session.operatingSystem ?: "Unknown")
                    }
                } else {
                    items(data?.sessions ?: emptyList()) { session ->
                        val addr =
                            if (session.address.size >= 2) "${session.address[0]}:${session.address[1]}" else "N/A"
                        TableRow(session.hostname, addr, session.operatingSystem ?: "Unknown")
                    }
                    items(data?.beacons ?: emptyList()) { beacon ->
                        TableRow(beacon.hostname, beacon.address, beacon.operatingSystem)
                    }
                }
            }
        }
    }

    @Composable
    fun TableRow(col1: String, col2: String, col3: String) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(col1, Modifier.weight(1f))
            Text(col2, Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
            Text(col3, Modifier.weight(1f))
        }
        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
    }
}
