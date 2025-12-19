package com.example.nappula3

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class StatsFragment : Fragment() {

    private var playerStats: Map<String, Any> = emptyMap()
    private var gamemodeInfo: String? = null

    companion object {
        fun newInstance(stats: Map<String, Any>, gamemodeInfo: String?): StatsFragment {
            return StatsFragment().apply {
                playerStats = stats
                this.gamemodeInfo = gamemodeInfo
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)

        val gamemodeText: TextView = view.findViewById(R.id.gamemodeInfo)
        val statsContainer: LinearLayout = view.findViewById(R.id.statsContainer)

        updateGamemodeText(gamemodeText)

        // Clear any existing views
        statsContainer.removeAllViews()

        // Add stats dynamically
        if (playerStats.isEmpty()) {
            val noStatsText = TextView(requireContext()).apply {
                text = "No stats available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            statsContainer.addView(noStatsText)
        } else {
            playerStats.forEach { (key, value) ->
                val statView = createStatView(key, value.toString())
                statsContainer.addView(statView)
            }
        }

        return view
    }

    private fun updateGamemodeText(gamemodeText: TextView) {
        if (gamemodeInfo.isNullOrBlank()) {
            gamemodeText.visibility = View.GONE
        } else {
            gamemodeText.visibility = View.VISIBLE
            gamemodeText.text = gamemodeInfo
        }
    }

    private fun createStatView(label: String, value: String): View {
        val statLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 8, 16, 8)
        }

        val labelText = TextView(requireContext()).apply {
            text = "$label:"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val valueText = TextView(requireContext()).apply {
            text = value
            textSize = 16f
            textAlignment = TextView.TEXT_ALIGNMENT_TEXT_END
        }

        statLayout.addView(labelText)
        statLayout.addView(valueText)

        return statLayout
    }

    fun updateStats(stats: Map<String, Any>) {
        playerStats = stats
        view?.let {
            val statsContainer: LinearLayout = it.findViewById(R.id.statsContainer)
            statsContainer.removeAllViews()

            if (stats.isEmpty()) {
                val noStatsText = TextView(requireContext()).apply {
                    text = "No stats available"
                    textSize = 16f
                    setPadding(16, 16, 16, 16)
                }
                statsContainer.addView(noStatsText)
            } else {
                stats.forEach { (key, value) ->
                    val statView = createStatView(key, value.toString())
                    statsContainer.addView(statView)
                }
            }
        }
    }

    fun updateGamemodeInfo(info: String?) {
        gamemodeInfo = info
        view?.findViewById<TextView>(R.id.gamemodeInfo)?.let { gamemodeText ->
            updateGamemodeText(gamemodeText)
        }
    }
}
