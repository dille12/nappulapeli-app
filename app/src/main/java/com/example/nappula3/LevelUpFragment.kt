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
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.json.JSONObject
import androidx.core.graphics.toColorInt
import com.google.android.material.button.MaterialButton

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

        pawnNameLabel.text = "LEVEL UP!"
        itemButtonsContainer.removeAllViews()

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
            mb.setOnClickListener {
                if (!closing) {
                    closing = true
                    sendChoice(name)
                    // Remove only this fragment (bottom panel). Header remains.
                    parentFragmentManager.beginTransaction()
                        .remove(this@LevelUpFragment)
                        .commitAllowingStateLoss()
                    // Clear pending state
                    act.pendingLevelUpItems.clear()
                    act.pendingLevelUpPawn = null

                    // Revert back to HUD
                    val hudLines = (activity as MainActivity).latestHudLines
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.bottomPanelContainer, HUDFragment.newInstance(hudLines))
                        .commit()
                }
            }
            itemButtonsContainer.addView(mb)
        }
    }

    private fun sendChoice(itemName: String) {
        val act = requireActivity() as MainActivity
        val json = JSONObject()
            .put("type", "levelUpChoice")
            .put("pawn", act.pendingLevelUpPawn ?: act.playerName.orEmpty())
            .put("item", itemName)
        act.sendJson(json.toString())
    }
}
