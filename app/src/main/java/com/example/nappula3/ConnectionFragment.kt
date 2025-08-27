package com.example.nappula3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class ConnectionFragment : Fragment() {

    private lateinit var statusText: TextView
    private lateinit var nextButton: Button
    private lateinit var retryButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connection, container, false)

        statusText = view.findViewById(R.id.statusText)
        nextButton = view.findViewById(R.id.nextButton)
        retryButton = view.findViewById(R.id.retryButton)

        nextButton.isEnabled = false
        retryButton.visibility = View.GONE

        // Ask MainActivity to connect WebSocket if not connected
        val main = activity as? MainActivity
        if (main?.webSocket == null) {
            main?.connectWebSocket()
        }

        // Next button navigates to AvatarFragment
        nextButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AvatarFragment())
                .addToBackStack(null)
                .commit()
        }

        // Retry button simply reconnects
        retryButton.setOnClickListener {
            retryButton.visibility = View.GONE
            statusText.text = "Reconnecting..."
            main?.connectWebSocket()
        }

        return view
    }

    // Called by MainActivity when connection succeeds
    fun onConnected() {
        statusText.text = "Connected!"
        nextButton.isEnabled = true
    }

    // Called by MainActivity when connection fails
    fun onConnectionFailed() {
        statusText.text = "Connection failed"
        nextButton.isEnabled = false
        retryButton.visibility = View.VISIBLE
    }
}
