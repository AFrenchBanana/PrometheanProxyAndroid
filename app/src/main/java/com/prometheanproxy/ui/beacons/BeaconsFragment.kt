package com.prometheanproxy.ui.beacons

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

class BeaconsFragment : Fragment() {

    private lateinit var beaconsRecyclerView: RecyclerView
    private lateinit var beaconAdapter: BeaconAdapter
    private val beacons = mutableListOf<Beacon>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_beacons, container, false)
        beaconsRecyclerView = view.findViewById(R.id.beacons_recycler_view)
        beaconsRecyclerView.layoutManager = LinearLayoutManager(context)
        beaconAdapter = BeaconAdapter(beacons)
        beaconsRecyclerView.adapter = beaconAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCurrentBeacons()
    }

    private fun getCurrentBeacons() {
        Log.d("PrometheanProxy", "getCurrentBeacons() called")
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

                val fullUrl = "https://$url/api/connections?filter=beacons"
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
                        val beaconsArray = jsonObject.getJSONArray("beacons")

                        val newBeacons = mutableListOf<Beacon>()
                        for (i in 0 until beaconsArray.length()) {
                            val beaconObject = beaconsArray.getJSONObject(i)
                            val beacon = Beacon(
                                userID = beaconObject.getString("userID"),
                                uuid = beaconObject.getString("uuid"),
                                address = beaconObject.getString("address"),
                                hostname = beaconObject.getString("hostname"),
                                operatingSystem = beaconObject.getString("operating_system"),
                                lastBeacon = beaconObject.getString("last_beacon"),
                                nextBeacon = beaconObject.getString("next_beacon"),
                                timer = beaconObject.getString("timer"),
                                jitter = beaconObject.getString("jitter")
                            )
                            newBeacons.add(beacon)
                        }

                        withContext(Dispatchers.Main) {
                            beacons.clear()
                            beacons.addAll(newBeacons)
                            beaconAdapter.notifyDataSetChanged()
                        }
                    } else {
                        Log.e("PrometheanProxy", "Response was not successful. Response body: $responseBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("PrometheanProxy", "Error getting current beacons: ${e.message}", e)
            }
        }
    }
}