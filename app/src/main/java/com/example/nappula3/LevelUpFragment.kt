package com.example.nappula3

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class LevelUpFragment : Fragment() {

    private lateinit var pawnNameLabel: TextView
    private lateinit var itemButtonsContainer: LinearLayout
    private var closing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_level_up, container, false)
        pawnNameLabel = v.findViewById(R.id.pawnNameLabel)
        itemButtonsContainer = v.findViewById(R.id.itemButtonsContainer)
        populateItems()
        return v
    }

    @SuppressLint("SetTextI18n")
    private fun populateItems() {
        val act = requireActivity() as MainActivity
        val items = act.pendingLevelUpItems
        val pawn = act.pendingLevelUpPawn ?: act.playerName.orEmpty()

        pawnNameLabel.text = getString(R.string.levelup_title)
        itemButtonsContainer.removeAllViews()

        // Check if we have items to show
        if (items.isEmpty()) {
            val noItemsText = TextView(requireContext()).apply {
                text = getString(R.string.levelup_no_options)
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setTextColor(Color.GRAY)
            }
            itemButtonsContainer.addView(noItemsText)
            return
        }

        for ((name, desc) in items) {
            val mb = MaterialButton(requireContext())
            val spannable = SpannableString("$name\n$desc")
            spannable.setSpan(AbsoluteSizeSpan(20, true), 0, name.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(AbsoluteSizeSpan(14, true), name.length + 1, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            mb.text = spannable
            mb.cornerRadius = 32
            mb.strokeWidth = 3
            mb.setStrokeColorResource(R.color.black)
            mb.setBackgroundColor("#455A64".toColorInt())
            mb.setTextColor(Color.WHITE)

            // Add some margin between buttons
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            mb.layoutParams = layoutParams

            mb.setOnClickListener {
                if (!closing) {
                    closing = true
                    sendChoice(name)
                    handleLevelUpCompletion()
                }
            }
            itemButtonsContainer.addView(mb)
        }
    }

    private fun handleLevelUpCompletion() {
        val act = requireActivity() as MainActivity

        // Clear pending state in MainActivity
        act.pendingLevelUpItems.clear()
        act.pendingLevelUpPawn = null
        act.pendingLevelUp = false

        // Notify GameFragment that level up is completed
        val gameFragment = parentFragment as? GameFragment
        gameFragment?.onLevelUpCompleted()
    }

    private fun sendChoice(itemName: String) {
        val act = requireActivity() as MainActivity
        val json = JSONObject()
            .put("type", "levelUpChoice")
            .put("pawn", act.pendingLevelUpPawn ?: act.playerName.orEmpty())
            .put("item", itemName)
        act.sendJson(json.toString())
    }

    override fun onResume() {
        super.onResume()
        // Refresh the items in case they changed
        if (::itemButtonsContainer.isInitialized) {
            populateItems()
        }
    }
}
