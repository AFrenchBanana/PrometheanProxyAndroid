package com.prometheanproxy.ui.sessions

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.prometheanproxy.R
import com.prometheanproxy.connectivity.Connectivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

// TODO: You'll need to create a Session.kt data class similar to Beacon.kt
// TODO: You'll need to create a SessionAdapter similar to BeaconAdapter
// import com.prometheanproxy.ui.beacons.Beacon // TODO: Change to Session
// import com.prometheanproxy.ui.beacons.BeaconAdapter // TODO: Change to SessionAdapter


class SessionsFragment : Fragment() {

    private lateinit var sessionsRecyclerView: RecyclerView
    private lateinit var sessionAdapter: SessionAdapter // TODO: Change from BeaconAdapter
    private val sessions = mutableListOf<Session>() // TODO: Change from Beacon

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO: Create R.layout.fragment_sessions
        val view = inflater.inflate(R.layout.fragment_sessions, container, false)
        sessionsRecyclerView = view.findViewById(R.id.sessions_recycler_view) // TODO: Update ID in layout
        sessionsRecyclerView.layoutManager = LinearLayoutManager(context)
        sessionAdapter = SessionAdapter(sessions) // TODO: Change from BeaconAdapter
        sessionsRecyclerView.adapter = sessionAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCurrentSessions()
    }

    private fun getCurrentSessions() {
        Log.d("PrometheanProxy", "getCurrentSessions() called")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = Connectivity.getInsecureOkHttpClient().build()

                val sharedPref = activity?.getSharedPreferences("com.prometheanproxy.login", Context.MODE_PRIVATE)
                val url = sharedPref?.getString("url", "")

                if (url.isNullOrEmpty() || Connectivity.authToken.isNullOrEmpty()) {
                    Log.e("PrometheanProxy", "URL or AuthToken is missing from SharedPreferences.")
                    Log.e("PrometheanProxy", "URL: $url, AuthToken: ${Connectivity.authToken}")
                    return@launch
                }

                // TODO: Update the API endpoint for sessions
                val fullUrl = "https://$url/api/connections?filter=sessions"
                Log.d("PrometheanProxy", "Making request to: $fullUrl")

                val request = Request.Builder()
                    .url(fullUrl)
                    .addHeader("Authorization", "bearer ${Connectivity.authToken}")
                    .build()

                client.newCall(request).execute().use { response ->
                    Log.d("PrometheanProxy", "Received response with code: ${response.code}")
                    val responseBody = response.body?.string()

                    if (response.isSuccessful) {
                        Log.d("PrometheanProxy", "Response was successful.")
                        Log.d("PrometheanProxy", "Response body: $responseBody")

                        if (responseBody.isNullOrBlank()) {
                            Log.e("PrometheanProxy", "Response body is null or empty.")
                            return@use
                        }

                        val jsonObject = org.json.JSONObject(responseBody)
                        // TODO: Update JSON parsing based on session data structure
                        val sessionsArray = jsonObject.getJSONArray("sessions")

                        val newSessions = mutableListOf<Session>() // TODO: Change from Beacon
                        for (i in 0 until sessionsArray.length()) {
                            val sessionObject = sessionsArray.getJSONObject(i)
                            // TODO: Update Session properties based on your Session.kt data class
                            val session = Session(
                                // Example properties, adjust as needed
                                // userID = sessionObject.getString("userID"),
                                // uuid = sessionObject.getString("uuid"),
                                // address = sessionObject.getString("address"),
                                // hostname = sessionObject.getString("hostname"),
                                // operatingSystem = sessionObject.getString("operating_system"),
                                // lastSession = sessionObject.getString("last_session"), // Renamed from last_beacon
                                // nextSession = sessionObject.getString("next_session"), // Renamed from next_beacon
                                // timer = sessionObject.getString("timer"),
                                // jitter = sessionObject.getString("jitter")
                            )
                            newSessions.add(session)
                        }

                        withContext(Dispatchers.Main) {
                            sessions.clear()
                            sessions.addAll(newSessions)
                            sessionAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.e("PrometheanProxy", "Response was not successful. Response body: $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("PrometheanProxy", "Error getting current sessions: ${e.message}", e)
            }
        }
    }
}