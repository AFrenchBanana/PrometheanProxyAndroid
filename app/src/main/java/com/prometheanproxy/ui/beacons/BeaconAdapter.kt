package com.prometheanproxy.ui.beacons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.prometheanproxy.R

class BeaconAdapter(private val beacons: List<Beacon>) : RecyclerView.Adapter<BeaconAdapter.BeaconViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_beacon, parent, false)
        return BeaconViewHolder(view)
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.bind(beacon)
    }

    override fun getItemCount(): Int = beacons.size

    class BeaconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val uuidTextView: TextView = itemView.findViewById(R.id.beacon_uuid)
        private val hostnameTextView: TextView = itemView.findViewById(R.id.beacon_hostname)
        private val osTextView: TextView = itemView.findViewById(R.id.beacon_os)
        private val userTextView: TextView = itemView.findViewById(R.id.beacon_user)
        private val addressTextView: TextView = itemView.findViewById(R.id.beacon_address)
        private val lastBeaconTextView: TextView = itemView.findViewById(R.id.beacon_last_beacon)
        private val nextBeaconTextView: TextView = itemView.findViewById(R.id.beacon_next_beacon)
        private val timerTextView: TextView = itemView.findViewById(R.id.beacon_timer)
        private val jitterTextView: TextView = itemView.findViewById(R.id.beacon_jitter)
        private val interactButton: Button = itemView.findViewById(R.id.interact_button)

        fun bind(beacon: Beacon) {
            uuidTextView.text = beacon.uuid
            hostnameTextView.text = beacon.hostname
            osTextView.text = beacon.operatingSystem
            userTextView.text = beacon.userID
            addressTextView.text = beacon.address
            lastBeaconTextView.text = beacon.lastBeacon
            nextBeaconTextView.text = beacon.nextBeacon
            timerTextView.text = beacon.timer
            jitterTextView.text = beacon.jitter

            interactButton.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("beacon", beacon)
                }
                itemView.findNavController().navigate(R.id.action_beaconsFragment_to_beaconDetailFragment, bundle)
            }
        }
    }
}