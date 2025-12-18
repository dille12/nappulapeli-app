package com.example.nappula3

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.core.content.edit

class ConnectionFragment : Fragment() {

    companion object {
        const val ARG_IP = "arg_ip"

        fun newInstance(initialIp: String?): ConnectionFragment {
            val fragment = ConnectionFragment()
            if (!initialIp.isNullOrBlank()) {
                fragment.arguments = Bundle().apply { putString(ARG_IP, initialIp) }
            }
            return fragment
        }
    }

    private lateinit var statusText: TextView
    private lateinit var nextButton: Button
    private lateinit var retryButton: Button
    private lateinit var ipInput: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connection, container, false)

        statusText = view.findViewById(R.id.statusText)
        nextButton = view.findViewById(R.id.nextButton)
        retryButton = view.findViewById(R.id.retryButton)
        ipInput = view.findViewById(R.id.ipInput)

        val persistedIp = savedInstanceState?.getString(ARG_IP)
            ?: arguments?.getString(ARG_IP)
            ?: requireActivity().getPreferences(Context.MODE_PRIVATE).getString(ARG_IP, "")

        if (!persistedIp.isNullOrBlank()) {
            ipInput.setText(persistedIp)
        }

        ipInput.addTextChangedListener { text ->
            val trimmed = text?.toString()?.trim() ?: ""
            persistIp(trimmed)
            (activity as? MainActivity)?.latestIp = trimmed
        }

        nextButton.isEnabled = false
        retryButton.visibility = View.GONE

        // Ask MainActivity to connect WebSocket if not connected
        val main = activity as? MainActivity
        if (main?.webSocket == null) {
            connectWithCurrentIp(main)
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
            connectWithCurrentIp(main)
        }

        return view
    }

    private fun connectWithCurrentIp(main: MainActivity?) {
        val ip = ipInput.text.toString().trim()
        persistIp(ip)
        main?.latestIp = ip
        if (ip.isBlank()) {
            Log.w("WS", "Skipping connect: IP is blank")
            statusText.text = "Enter the server IP to connect"
            nextButton.isEnabled = false
            retryButton.visibility = View.VISIBLE
            return
        }
        Log.d("WS", "Trying to connect to $ip")
        main?.connectWebSocket(ip)
    }

    private fun persistIp(ip: String) {
        requireActivity().getPreferences(Context.MODE_PRIVATE)
            .edit {
                putString(ARG_IP, ip)
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ARG_IP, ipInput.text.toString().trim())
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
