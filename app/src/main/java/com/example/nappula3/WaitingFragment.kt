package com.example.nappula3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import okhttp3.*
import android.widget.TextView
import org.json.JSONObject
class WaitingFragment : Fragment() {

    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wait, container, false)
        statusText = view.findViewById(R.id.statusText)
        statusText.text = "Waiting for game to start..."
        return view
    }
}

