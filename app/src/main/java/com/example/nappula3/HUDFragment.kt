package com.example.nappula3

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class HUDFragment : Fragment() {

    private lateinit var hudContainer: LinearLayout
    private var hudLines: List<Map<String, Any>> = emptyList()

    companion object {
        fun newInstance(hudLines: List<Map<String, Any>>): HUDFragment {
            val fragment = HUDFragment()
            fragment.hudLines = hudLines
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_hud, container, false)
        hudContainer = view.findViewById(R.id.hudContainer)
        populateHud()
        return view
    }

    fun populateHud() {
        hudContainer.removeAllViews()
        for (line in hudLines) {
            val text = line["text"] as? String ?: continue
            val colorInt = line["color"] as? Int ?: Color.WHITE
            val tv = TextView(requireContext())
            tv.text = text
            tv.setTextColor(colorInt)
            tv.textSize = 16f
            hudContainer.addView(tv)
        }
    }

    // Update HUD lines dynamically
    fun updateHud(newHudLines: List<Map<String, Any>>) {
        hudLines = newHudLines
        populateHud()
    }
}
