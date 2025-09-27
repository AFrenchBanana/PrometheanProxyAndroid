package com.prometheanproxy.ui.beacons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.prometheanproxy.R

class BeaconDetailFragment : Fragment() {

    private lateinit var beacon: Beacon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            beacon = it.getParcelable("beacon")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_beacon_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.beacon_detail_uuid).text = beacon.uuid
        view.findViewById<TextView>(R.id.beacon_detail_hostname).text = beacon.hostname
        view.findViewById<TextView>(R.id.beacon_detail_os).text = beacon.operatingSystem
        view.findViewById<TextView>(R.id.beacon_detail_user).text = beacon.userID
        view.findViewById<TextView>(R.id.beacon_detail_address).text = beacon.address
        view.findViewById<TextView>(R.id.beacon_detail_last_beacon).text = beacon.lastBeacon
        view.findViewById<TextView>(R.id.beacon_detail_next_beacon).text = beacon.nextBeacon
        view.findViewById<TextView>(R.id.beacon_detail_timer).text = beacon.timer
        view.findViewById<TextView>(R.id.beacon_detail_jitter).text = beacon.jitter
    }
}