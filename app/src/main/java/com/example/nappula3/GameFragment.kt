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
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*

class GameFragment : Fragment() {

    private lateinit var avatarPreview: ImageView
    private lateinit var nameTextLabel: TextView
    private lateinit var levelText: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private var scanJob: Job? = null

    // Keep references to fragments to avoid recreation
    private var hudFragment: HUDFragment? = null
    private var levelUpFragment: LevelUpFragment? = null
    private var currentBottomFragment: Fragment? = null

    // Cache data to prevent loss on view recreation
    private var cachedPlayerName: String? = null
    private var cachedBase64Image: String? = null
    private var cachedPlayerStats: MutableMap<String, Any> = mutableMapOf()
    private var cachedBgColor: Int = Color.WHITE

    companion object {
        private const val ARG_BG_COLOR = "bgColor"

        fun newInstance(bgColor: Int): GameFragment {
            return GameFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_BG_COLOR, bgColor)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore cached data if available
        savedInstanceState?.let { bundle ->
            cachedPlayerName = bundle.getString("cached_player_name")
            cachedBase64Image = bundle.getString("cached_base64_image")
            cachedBgColor = bundle.getInt("cached_bg_color", Color.WHITE)

            // Restore player stats
            val statsSize = bundle.getInt("stats_size", 0)
            for (i in 0 until statsSize) {
                val key = bundle.getString("stats_key_$i")
                val value = bundle.getInt("stats_value_$i")
                if (key != null) {
                    cachedPlayerStats[key] = value
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save current data
        outState.putString("cached_player_name", cachedPlayerName)
        outState.putString("cached_base64_image", cachedBase64Image)
        outState.putInt("cached_bg_color", cachedBgColor)

        // Save player stats
        outState.putInt("stats_size", cachedPlayerStats.size)
        cachedPlayerStats.entries.forEachIndexed { index, entry ->
            outState.putString("stats_key_$index", entry.key)
            outState.putInt("stats_value_$index", entry.value as Int)
        }
    }

    private fun checkPendingLevelUpOrHud() {
        val main = activity as? MainActivity ?: return

        // Update the level up button state based on pending level up
        updateLevelUpButtonState(main.pendingLevelUp == true)
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

        // Just update the button state, don't auto-switch
        updateLevelUpButtonState(main.pendingLevelUp == true)
    }

    override fun onStop() {
        super.onStop()
        scanJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear fragment references to prevent memory leaks
        hudFragment = null
        levelUpFragment = null
        currentBottomFragment = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        avatarPreview = view.findViewById(R.id.avatarPreview)
        nameTextLabel = view.findViewById(R.id.nameTextLabel)
        levelText = view.findViewById(R.id.level)
        bottomNavigation = view.findViewById(R.id.bottom_navigation)

        setupBottomNavigation()
        loadPlayerData()
        updateUI()

        // Apply background color immediately when view is created
        view.setBackgroundColor(cachedBgColor)

        return view
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_hud -> {
                    showHudFragment()
                    true
                }
                R.id.nav_levelup -> {
                    // Only allow if level up is available
                    val main = activity as? MainActivity
                    if (main?.pendingLevelUp == true || main?.pendingLevelUpItems?.isNotEmpty() == true) {
                        showLevelUpFragment()
                        true
                    } else {
                        // Don't switch if no level up available, but don't prevent the click
                        // Just show an empty state
                        showEmptyLevelUpFragment()
                        true
                    }
                }
                R.id.nav_stats -> {
                    showStatsFragment()
                    true
                }
                else -> false
            }
        }

        // Set default selection and initial button states
        bottomNavigation.selectedItemId = R.id.nav_hud
        updateLevelUpButtonState(false) // Initially disabled
    }

    private fun showHudFragment() {
        // Always get the latest HUD data and rivalry data
        val main = activity as? MainActivity
        hudFragment = if (main?.latestHudLines != null || main?.mostKilledBy != null || main?.mostKilled != null) {
            HUDFragment.newInstance(
                main?.latestHudLines ?: emptyList(),
                main?.mostKilledBy,
                main?.mostKilled
            )
        } else {
            HUDFragment.newInstance(emptyList())
        }

        currentBottomFragment = hudFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, hudFragment!!)
            .commit()
    }

    private fun showLevelUpFragment() {
        if (levelUpFragment == null) {
            levelUpFragment = LevelUpFragment()
        }

        currentBottomFragment = levelUpFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, levelUpFragment!!)
            .commit()
    }

    // Method to be called when level up is completed
    fun onLevelUpCompleted() {
        // Clear the cached level up fragment so it gets recreated next time
        levelUpFragment = null

        // Update button state
        updateLevelUpButtonState(false)

        // Switch back to HUD
        showHudFragment()
    }

    private fun showEmptyLevelUpFragment() {
        // Create a new empty level up fragment
        levelUpFragment = LevelUpFragment()
        currentBottomFragment = levelUpFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, levelUpFragment!!)
            .commit()
    }

    private fun showStatsFragment() {
        // For now, show a placeholder or you could create a different stats view
        val statsFragment = StatsFragment.newInstance(cachedPlayerStats)
        currentBottomFragment = statsFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, statsFragment)
            .commit()
    }

    // Method to update HUD when new data arrives
    fun updateHudData(newHudLines: List<Map<String, Any>>) {
        // Only update if fragment is added and HUD fragment exists
        if (isAdded) {
            hudFragment?.updateHud(newHudLines)
        }
    }

    // Method to update rivalry data
    fun updateRivalryData(nemesis: Map<String, Any>?, victim: Map<String, Any>?) {
        // Only update if fragment is added and HUD fragment exists
        if (isAdded) {
            hudFragment?.updateRivalryData(nemesis, victim)
        }
    }

    private fun updateLevelUpButtonState(isEnabled: Boolean) {
        if (::bottomNavigation.isInitialized) {
            val menu = bottomNavigation.menu
            val levelUpItem = menu.findItem(R.id.nav_levelup)
            levelUpItem?.isEnabled = isEnabled

            // Visual feedback
            val alpha = if (isEnabled) 1.0f else 0.5f
            levelUpItem?.icon?.alpha = (alpha * 255).toInt()

            // Add notification badge when enabled
            if (isEnabled) {
                addLevelUpBadge()
            } else {
                removeLevelUpBadge()
            }
        }
    }

    private fun addLevelUpBadge() {
        // You can use BadgeDrawable from Material Components
        // For now, we'll change the title to indicate availability
        val menu = bottomNavigation.menu
        val levelUpItem = menu.findItem(R.id.nav_levelup)
        levelUpItem?.title = "Level Up! ⭐"
        levelUpItem.isEnabled = true
    }

    private fun removeLevelUpBadge() {
        val menu = bottomNavigation.menu
        val levelUpItem = menu.findItem(R.id.nav_levelup)
        levelUpItem?.title = "Locked"
        levelUpItem.isEnabled = false
    }

    private fun loadPlayerData() {
        val main = activity as? MainActivity
        if (main != null) {
            // Update cached data with latest from MainActivity
            cachedPlayerName = main.playerName ?: cachedPlayerName
            cachedBase64Image = main.playerImageBase64 ?: cachedBase64Image
            cachedPlayerStats.putAll(main.playerStats)
        }

        // Get background color from arguments
        arguments?.let {
            cachedBgColor = it.getInt(ARG_BG_COLOR, cachedBgColor)
        }
    }

    private fun updateUI() {
        // Apply background color to the main container
        view?.setBackgroundColor(cachedBgColor)

        nameTextLabel.text = cachedPlayerName ?: "Unknown"

        cachedBase64Image?.let { base64Image ->
            try {
                val imageBytes: ByteArray = Base64.decode(base64Image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                avatarPreview.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // Handle decoding error gracefully
            }
        }

        if (cachedPlayerStats.isNotEmpty()) {
            updateStatsDisplay(cachedPlayerStats)
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateStats(playerStats: MutableMap<String, Any>) {
        // Update cached stats
        cachedPlayerStats.clear()
        cachedPlayerStats.putAll(playerStats)

        // Update display
        updateStatsDisplay(playerStats)
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatsDisplay(playerStats: Map<String, Any>) {
        val level = playerStats["level"]
        val xp = playerStats["xp"]
        val xpToNextLevel = playerStats["xpToNextLevel"]

        if (::levelText.isInitialized) {
            levelText.text = "LEVEL: $level XP: $xp / $xpToNextLevel"
        }
    }

    // Method to refresh data when returning to this fragment
    fun refreshData() {
        loadPlayerData()
        updateUI()
        // Ensure background color is applied
        view?.setBackgroundColor(cachedBgColor)
    }
}