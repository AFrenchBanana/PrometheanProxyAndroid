package com.prometheanproxy.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.prometheanproxy.R
class SessionAdapter(private val sessions: List<Session>) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        // TODO: Create R.layout.item_session
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        holder.bind(session)
    }

    override fun getItemCount(): Int = sessions.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val uuidTextView: TextView = itemView.findViewById(R.id.session_uuid)
        private val hostnameTextView: TextView = itemView.findViewById(R.id.session_hostname)
        private val osTextView: TextView = itemView.findViewById(R.id.session_os)
        private val userTextView: TextView = itemView.findViewById(R.id.session_user)
        private val addressTextView: TextView = itemView.findViewById(R.id.session_address)
        private val lastSessionTextView: TextView = itemView.findViewById(R.id.session_last_session)
        private val nextSessionTextView: TextView = itemView.findViewById(R.id.session_next_session)
        private val timerTextView: TextView = itemView.findViewById(R.id.session_timer)
        private val jitterTextView: TextView = itemView.findViewById(R.id.session_jitter)
        private val interactButton: Button = itemView.findViewById(R.id.interact_button)

        fun bind(session: Session) {
            uuidTextView.text = session.uuid
            hostnameTextView.text = session.hostname
            osTextView.text = session.operatingSystem
            userTextView.text = session.userID
            addressTextView.text = session.address
            lastSessionTextView.text = session.lastSession
            nextSessionTextView.text = session.nextSession
            timerTextView.text = session.timer
            jitterTextView.text = session.jitter

            interactButton.setOnClickListener {
                val bundle = Bundle().apply {
                    putParcelable("session", session)
                }
                itemView.findNavController().navigate(R.id.action_sessionsFragment_to_sessionDetailFragment, bundle)
            }
        }
    }
}