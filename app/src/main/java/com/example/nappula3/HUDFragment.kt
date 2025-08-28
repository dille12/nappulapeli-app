package com.example.nappula3

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class HUDFragment : Fragment() {

    private lateinit var hudContainer: LinearLayout
    private var hudLines: List<Map<String, Any>> = emptyList()
    private var nemesisData: Map<String, Any>? = null
    private var victimData: Map<String, Any>? = null

    companion object {
        fun newInstance(hudLines: List<Map<String, Any>>): HUDFragment {
            val fragment = HUDFragment()
            fragment.hudLines = hudLines
            return fragment
        }

        fun newInstance(hudLines: List<Map<String, Any>>, nemesis: Map<String, Any>?, victim: Map<String, Any>?): HUDFragment {
            val fragment = HUDFragment()
            fragment.hudLines = hudLines
            fragment.nemesisData = nemesis
            fragment.victimData = victim
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
        // Check if fragment is still attached and view is initialized
        if (!isAdded || !::hudContainer.isInitialized) {
            return
        }

        hudContainer.removeAllViews()

        // Add nemesis/victim section if available
        if (nemesisData != null || victimData != null) {
            val rivalryCard = createRivalryCard()
            hudContainer.addView(rivalryCard)
        }

        if (hudLines.isEmpty()) {
            if (nemesisData == null && victimData == null) {
                showEmptyState()
            }
            return
        }

        // Add title for buffs section
        val titleCard = createTitleCard()
        hudContainer.addView(titleCard)

        // Group similar items or show all items in cards
        for ((index, line) in hudLines.withIndex()) {
            val text = line["text"] as? String ?: continue
            val colorInt = line["color"] as? Int ?: Color.WHITE

            val hudCard = createHudCard(text, colorInt, index)
            hudContainer.addView(hudCard)
        }
    }

    private fun createRivalryCard(): CardView {
        // Double-check we're still attached
        if (!isAdded) {
            throw IllegalStateException("Fragment not attached when creating rivalry card")
        }

        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.argb(200, 80, 20, 20)) // Darker red-tinted background for rivalry
        }

        val mainLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
        }

        // Title for rivalry section
        val rivalryTitle = TextView(requireContext()).apply {
            text = "🎯 Kill Statistics"
            textSize = 18f
            setTextColor(Color.WHITE)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }
        mainLayout.addView(rivalryTitle)

        // Create nemesis row if available
        nemesisData?.let { nemesis ->
            val nemesisRow = createRivalryRow(
                "💀 Most killed by:",
                nemesis["name"] as? String ?: "Unknown",
                nemesis["image"] as? String,
                nemesis["kills"] as? Int ?: 0,
                Color.rgb(255, 100, 100) // Light red for nemesis
            )
            mainLayout.addView(nemesisRow)
        }

        // Create victim row if available
        victimData?.let { victim ->
            val victimRow = createRivalryRow(
                "🏆 Most killed:",
                victim["name"] as? String ?: "Unknown",
                victim["image"] as? String,
                victim["kills"] as? Int ?: 0,
                Color.rgb(100, 255, 100) // Light green for victim
            )
            mainLayout.addView(victimRow)
        }

        cardView.addView(mainLayout)
        return cardView
    }

    private fun createRivalryRow(label: String, name: String, imageBase64: String?, killCount: Int, accentColor: Int): LinearLayout {
        val rowLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 8, 0, 8)
        }

        // Label
        val labelText = TextView(requireContext()).apply {
            text = label
            textSize = 14f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = 12
            }
        }

        // Avatar (small circle)
        val avatarView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                rightMargin = 8
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = createCircleDrawable(accentColor)

            // Load avatar if available
            imageBase64?.let { base64 ->
                try {
                    val imageBytes: ByteArray = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    setImageBitmap(bitmap)
                } catch (e: Exception) {
                    // Keep the colored circle background if image fails to load
                }
            }
        }

        // Name and kill count
        val nameLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(requireContext()).apply {
            text = name
            textSize = 16f
            setTextColor(Color.WHITE)
        }

        val killText = TextView(requireContext()).apply {
            text = "$killCount kills"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        }

        nameLayout.addView(nameText)
        nameLayout.addView(killText)

        rowLayout.addView(labelText)
        rowLayout.addView(avatarView)
        rowLayout.addView(nameLayout)

        return rowLayout
    }

    private fun createTitleCard(): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 8)
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_dark))
        }

        val titleText = TextView(requireContext()).apply {
            text = "⚡ Active Buffs & Effects"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(20, 16, 20, 16)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        cardView.addView(titleText)
        return cardView
    }

    private fun createHudCard(text: String, colorInt: Int, index: Int): CardView {
        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 8, 16, 8)
            }
            radius = 8f
            cardElevation = 2f
            // Use semi-transparent dark background instead of white
            setCardBackgroundColor(Color.argb(180, 45, 45, 45)) // Semi-transparent dark gray
        }

        // Create the inner container
        val innerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 12, 16, 12)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Create colored indicator dot
        val indicator = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(12, 12).apply {
                rightMargin = 12
            }
            background = createCircleDrawable(colorInt)
        }

        // Create main text
        val textView = TextView(requireContext()).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE) // White text instead of black
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        // Create index/priority indicator
        val indexView = TextView(requireContext()).apply {
            this.text = "#${index + 1}"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(8, 4, 8, 4)
            background = createRoundedRectDrawable(Color.argb(100, 255, 255, 255)) // Semi-transparent white
        }

        innerLayout.addView(indicator)
        innerLayout.addView(textView)
        innerLayout.addView(indexView)
        cardView.addView(innerLayout)

        return cardView
    }

    private fun createCircleDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
        }
    }

    private fun createRoundedRectDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 8f
        }
    }

    private fun showEmptyState() {
        val emptyCard = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 32, 16, 16)
            }
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(Color.argb(180, 45, 45, 45)) // Semi-transparent dark
        }

        val emptyLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            gravity = android.view.Gravity.CENTER
        }

        val emptyIcon = TextView(requireContext()).apply {
            text = "📊"
            textSize = 48f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val emptyText = TextView(requireContext()).apply {
            text = "No active buffs or effects"
            textSize = 16f
            setTextColor(Color.WHITE) // White text
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        }

        val emptySubtext = TextView(requireContext()).apply {
            text = "Level up to gain powerful abilities!"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
        }

        emptyLayout.addView(emptyIcon)
        emptyLayout.addView(emptyText)
        emptyLayout.addView(emptySubtext)
        emptyCard.addView(emptyLayout)
        hudContainer.addView(emptyCard)
    }

    // Update HUD lines dynamically
    fun updateHud(newHudLines: List<Map<String, Any>>) {
        hudLines = newHudLines
        if (isAdded && ::hudContainer.isInitialized) {
            populateHud()
        }
    }

    // Update rivalry data
    fun updateRivalryData(nemesis: Map<String, Any>?, victim: Map<String, Any>?) {
        nemesisData = nemesis
        victimData = victim
        if (isAdded && ::hudContainer.isInitialized) {
            populateHud()
        }
    }

    // Update both HUD and rivalry data at once
    fun updateAll(newHudLines: List<Map<String, Any>>, nemesis: Map<String, Any>?, victim: Map<String, Any>?) {
        hudLines = newHudLines
        nemesisData = nemesis
        victimData = victim
        if (isAdded && ::hudContainer.isInitialized) {
            populateHud()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh HUD data from MainActivity when fragment becomes visible
        val main = activity as? MainActivity
        main?.latestHudLines?.let { latestLines ->
            if (latestLines != hudLines) {
                updateHud(latestLines)
            }
        }

        // Also refresh rivalry data
        if (main?.mostKilledBy != nemesisData || main?.mostKilled != victimData) {
            updateRivalryData(main?.mostKilledBy, main?.mostKilled)
        }
    }
}