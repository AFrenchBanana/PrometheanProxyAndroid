package com.prometheanproxy.ui.sessions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.prometheanproxy.R

// TODO: Ensure you have a Session.kt data class that is Parcelable
// import com.prometheanproxy.ui.beacons.Beacon // TODO: Change to Session

class SessionDetailFragment : Fragment() {

    private lateinit var session: Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // TODO: Ensure the key "session" matches how you pass it in the bundle
            session = it.getParcelable("session")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Create R.layout.fragment_session_detail
        return inflater.inflate(R.layout.fragment_session_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
         view.findViewById<TextView>(R.id.session_detail_uuid).text = session.uuid
         view.findViewById<TextView>(R.id.session_detail_hostname).text = session.hostname
         view.findViewById<TextView>(R.id.session_detail_os).text = session.operatingSystem
         view.findViewById<TextView>(R.id.session_detail_user).text = session.userID
         view.findViewById<TextView>(R.id.session_detail_address).text = session.address
         view.findViewById<TextView>(R.id.session_detail_next_session).text = session.nextSession // Renamed from nextBeacon
         view.findViewById<TextView>(R.id.session_detail_timer).text = session.timer
         view.findViewById<TextView>(R.id.session_detail_jitter).text = session.jitter
    }
}