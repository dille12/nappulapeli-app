package com.example.nappula3

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*

import kotlin.collections.mutableMapOf

class GameFragment : Fragment() {

    private lateinit var avatarPreview: ImageView
    private lateinit var nameTextLabel: TextView
    private lateinit var levelText: TextView

    private var scanJob: Job? = null

    private fun checkPendingLevelUpOrHud() {
        val main = activity as? MainActivity ?: return

        val fragment = if (main.pendingLevelUp == true) {
            LevelUpFragment()
        } else {
            HUDFragment()
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, fragment) // replaces whatever is there
            .commit()
    }

    override fun onStart() {
        super.onStart()
        scanJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(1000) // 1 second
                checkPendingLevelUp()
            }
        }
    }

    private fun checkPendingLevelUp() {
        val main = activity as? MainActivity ?: return
        val items = main.pendingLevelUpItems
        val pawnName = main.pendingLevelUpPawn
        val levelUp = main.pendingLevelUp

        if (levelUp == true) {
            val fragment = LevelUpFragment()
            main.supportFragmentManager.beginTransaction()
                .replace(R.id.bottomPanelContainer, fragment)
                .commit()

            main.pendingLevelUp = false
        }
    }

    override fun onStop() {
        super.onStop()
        scanJob?.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        avatarPreview = view.findViewById(R.id.avatarPreview)
        nameTextLabel = view.findViewById(R.id.nameTextLabel)
        levelText = view.findViewById(R.id.level)

        // Explicitly specify types
        val main = activity as MainActivity
        val playerName: String? = main.playerName
        val base64Image: String? = main.playerImageBase64

        arguments?.let { view.setBackgroundColor(it.getInt("bgColor", Color.WHITE)) }

        nameTextLabel.text = playerName ?: "Unknown"

        if (base64Image != null) {
            val imageBytes: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            avatarPreview.setImageBitmap(bitmap)
        }

        return view
    }

    @SuppressLint("SetTextI18n")
    fun updateStats(playerStats: MutableMap<String, Any>) {
        val level = playerStats["level"]
        val xp = playerStats["xp"]
        val xpToNextLevel = playerStats["xpToNextLevel"]
        levelText.text = "LEVEL: $level XP: $xp / $xpToNextLevel"
    }

}
