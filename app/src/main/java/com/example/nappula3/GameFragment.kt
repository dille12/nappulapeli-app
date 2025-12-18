package com.example.nappula3

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import org.json.JSONObject

class GameFragment : Fragment() {

    private lateinit var avatarPreview: ImageView
    private lateinit var nameTextLabel: TextView
    private lateinit var levelText: TextView
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var musicRequestFab: FloatingActionButton

    private var scanJob: Job? = null

    // Keep references to fragments to avoid recreation
    private var hudFragment: HUDFragment? = null
    private var levelUpFragment: LevelUpFragment? = null
    private var shopFragment: ShopFragment? = null
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
        shopFragment = null
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
        musicRequestFab = view.findViewById(R.id.musicRequestFab)

        setupMusicRequestFab()
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
                    showLevelUpFragment()
                    true
                }
                R.id.nav_shop -> {
                    showShopFragment()
                    true
                }
                R.id.nav_stats -> {
                    showStatsFragment()
                    true
                }
                else -> false
            }
        }

        // Set default selection
        bottomNavigation.selectedItemId = R.id.nav_hud
        // Initialize badge state (initially no level up available)
        updateLevelUpButtonState(false)
    }

    private fun setupMusicRequestFab() {
        musicRequestFab.setOnClickListener {
            showMusicRequestDialog()
        }
    }

    private fun showMusicRequestDialog() {
        if (!isAdded) return

        val editText = EditText(requireContext()).apply {
            hint = "Paste YouTube link here..."
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(48, 48, 48, 48)
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("🎵 Request Song")
        builder.setMessage("Add a song to the game playlist!")
        builder.setView(editText)

        builder.setPositiveButton("Add to Playlist") { _, _ ->
            val youtubeLink = editText.text.toString().trim()
            if (youtubeLink.isNotEmpty()) {
                if (isValidYouTubeLink(youtubeLink)) {
                    requestSongAddition(youtubeLink)
                } else {
                    Toast.makeText(requireContext(), "❌ Please enter a valid YouTube link", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun isValidYouTubeLink(link: String): Boolean {
        return link.contains("youtube.com") || link.contains("youtu.be")
    }

    private fun requestSongAddition(youtubeLink: String) {
        val main = activity as? MainActivity ?: return

        val cleanedLink = cleanYouTubeLink(youtubeLink)

        val json = JSONObject()
            .put("type", "musicRequest")
            .put("youtubeLink", cleanedLink)
            .put("pawnName", main.playerName ?: "Unknown")

        main.sendJson(json.toString())
        Toast.makeText(requireContext(), "🎵 Video added to playlist!", Toast.LENGTH_SHORT).show()
        android.util.Log.d("GameFragment", "Requested video addition: $cleanedLink")
    }

    private fun cleanYouTubeLink(link: String): String {
        return when {
            link.contains("youtube.com/watch?v=") -> {
                val videoId = link.substringAfter("youtube.com/watch?v=").substringBefore("&")
                "https://www.youtube.com/watch?v=$videoId"
            }
            link.contains("youtu.be/") -> {
                val videoId = link.substringAfter("youtu.be/").substringBefore("?")
                "https://www.youtube.com/watch?v=$videoId"
            }
            else -> link
        }
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
        val main = activity as? MainActivity
        if (main?.pendingLevelUp != true) {
            // Guard against navigating to level up when nothing is pending
            bottomNavigation.selectedItemId = R.id.nav_hud
            return
        }

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

    private fun showShopFragment() {
        if (shopFragment == null) {
            shopFragment = ShopFragment.newInstance()
        }

        currentBottomFragment = shopFragment
        childFragmentManager.beginTransaction()
            .replace(R.id.bottomPanelContainer, shopFragment!!)
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
    fun updateRivalryData(nemesis: Map<String, Any?>?, victim: Map<String, Any?>?) {
        // Only update if fragment is added and HUD fragment exists
        if (isAdded) {
            hudFragment?.updateRivalryData(nemesis, victim)
        }
    }

    // Method to update team color when switching teams
    fun updateTeamColor(newColor: Int) {
        if (isAdded) {
            cachedBgColor = newColor
            view?.setBackgroundColor(newColor)

            // Optional: Add a brief visual effect to indicate team switch
            showTeamSwitchEffect()
        }
    }

    // Method to update shop data
    fun updateShopData(nextWeapon: Map<String, Any?>?, items: List<Map<String, Any?>>, rerollCost: Int = 25) {
        if (isAdded) {
            val currency = cachedPlayerStats["Currency"] as? Int ?: 0
            shopFragment?.updateShop(currency, nextWeapon, items, rerollCost)
        }
    }

    // Method to handle purchase responses
    fun handlePurchaseResponse(success: Boolean, itemName: String, message: String) {
        if (isAdded) {
            // Show a brief message to user
            val messageText = if (success) {
                "✅ Purchased $itemName!"
            } else {
                "❌ $message"
            }

            Toast.makeText(requireContext(), messageText, Toast.LENGTH_SHORT).show()

            if (success) {
                // Refresh shop data after successful purchase
                val main = activity as? MainActivity
                val currency = cachedPlayerStats["Currency"] as? Int ?: 0
                shopFragment?.updateShop(currency, main?.nextWeapon, main?.shopItems ?: emptyList())
            }
        }
    }

    // Method to handle reroll responses
    fun handleRerollResponse(success: Boolean, rerollType: String, message: String) {
        if (isAdded) {
            // Show a brief message to user
            val messageText = if (success) {
                "🎲 Rerolled $rerollType!"
            } else {
                "❌ $message"
            }

            Toast.makeText(requireContext(), messageText, Toast.LENGTH_SHORT).show()

            if (success) {
                // Refresh shop data after successful reroll
                val main = activity as? MainActivity
                val currency = cachedPlayerStats["Currency"] as? Int ?: 0
                shopFragment?.updateShop(currency, main?.nextWeapon, main?.shopItems ?: emptyList())
            }
        }
    }

    // Method to handle drink registration responses
    fun handleDrinkRegistrationResponse(success: Boolean, drinkType: String, drinkValue: Int, message: String) {
        if (isAdded) {
            // Show a brief message to user
            val messageText = if (success) {
                "🍺 Registered $drinkType (+$drinkValue drinks)!"
            } else {
                "❌ $message"
            }

            Toast.makeText(requireContext(), messageText, Toast.LENGTH_SHORT).show()

            if (success) {
                // Currency will be updated via statUpdate packet from server
                // No need to manually update here
            }
        }
    }

    private fun showTeamSwitchEffect() {
        // Add a brief flash effect or animation to make the team switch more noticeable
        view?.animate()
            ?.alpha(0.7f)
            ?.setDuration(150)
            ?.withEndAction {
                view?.animate()
                    ?.alpha(1.0f)
                    ?.setDuration(150)
                    ?.start()
            }
            ?.start()
    }

    private fun updateLevelUpButtonState(isEnabled: Boolean) {
        if (::bottomNavigation.isInitialized) {
            val levelUpItem = bottomNavigation.menu.findItem(R.id.nav_levelup)
            levelUpItem.isEnabled = isEnabled

            if (isEnabled) {
                addLevelUpBadge()
            } else {
                removeLevelUpBadge()

                // If the disabled tab was selected, return to the HUD
                if (bottomNavigation.selectedItemId == R.id.nav_levelup) {
                    bottomNavigation.selectedItemId = R.id.nav_hud
                }
            }
        }
    }

    private fun addLevelUpBadge() {
        if (::bottomNavigation.isInitialized) {
            // Use Material Components badge
            val badge = bottomNavigation.getOrCreateBadge(R.id.nav_levelup)
            badge.isVisible = true
            badge.backgroundColor = Color.RED
            badge.badgeTextColor = Color.WHITE
            // Empty badge (just a dot)
            badge.clearNumber()
        }
    }

    private fun removeLevelUpBadge() {
        if (::bottomNavigation.isInitialized) {
            bottomNavigation.removeBadge(R.id.nav_levelup)
        }
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

        // Update shop currency if shop is visible
        val currency = playerStats["Currency"] as? Int
        if (currency != null) {
            shopFragment?.updateCurrency(currency)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatsDisplay(playerStats: Map<String, Any>) {
        val level = playerStats["Level"]
        val xp = playerStats["XP"]
        val xpToNextLevel = playerStats["XP to next level"]

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
