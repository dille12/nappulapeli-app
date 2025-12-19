package com.example.nappula3

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class ShopFragment : Fragment() {

    private lateinit var currencyText: TextView
    private lateinit var registerDrinksButton: MaterialButton
    //private lateinit var nextWeaponCard: CardView
    private lateinit var nextWeaponButton: MaterialButton
    private lateinit var rerollWeaponButton: MaterialButton
    private lateinit var itemsContainer: LinearLayout

    private var currentCurrency: Int = 0
    private var nextWeapon: Map<String, Any?>? = null
    private var shopItems: List<Map<String, Any?>> = emptyList()
    private var rerollCost: Int = 25 // Default reroll cost

    companion object {
        fun newInstance(): ShopFragment {
            return ShopFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shop, container, false)

        currencyText = view.findViewById(R.id.currencyText)
        registerDrinksButton = view.findViewById(R.id.registerDrinksButton)
        //nextWeaponCard = view.findViewById(R.id.nextWeaponCard)
        nextWeaponButton = view.findViewById(R.id.nextWeaponButton)
        rerollWeaponButton = view.findViewById(R.id.rerollWeaponButton)
        itemsContainer = view.findViewById(R.id.itemsContainer)

        loadShopData()
        setupRegisterDrinksButton()
        populateShop()

        return view
    }

    private fun loadShopData() {
        val main = activity as? MainActivity
        if (main != null) {
            // Get currency from player stats
            currentCurrency = when (val v = main.playerStats["Currency"]) {
                is Int -> v
                is String -> v.toIntOrNull() ?: 0
                else -> 0
            }

            // Get shop data from MainActivity (you'll need to add these)
            nextWeapon = main.nextWeapon
            shopItems = main.shopItems
        }
    }

    private fun populateShop() {
        if (!isAdded) return

        // Update currency display
        currencyText.text = "💰 $currentCurrency Drinks"

        // Setup register drinks button
        setupRegisterDrinksButton()

        // Setup next weapon button
        setupNextWeapon()

        // Setup reroll button
        setupRerollButton()

        // Setup shop items
        setupShopItems()
    }

    private fun setupRegisterDrinksButton() {
        registerDrinksButton.setOnClickListener {
            showDrinkRegistrationDialog()
        }
    }

    private fun showDrinkRegistrationDialog() {
        if (!isAdded) return

        val options = arrayOf(
            "🍺 330ml Drink (+100 drinks)",
            "🍺 500ml Drink (+150 drinks)",
            "🥃 Shot (+200 drinks)"
        )

        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("Register Your Drink")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> requestDrinkRegistration("330ml", 100)
                1 -> requestDrinkRegistration("500ml", 150)
                2 -> requestDrinkRegistration("shot", 200)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun requestDrinkRegistration(drinkType: String, drinkValue: Int) {
        val main = activity as? MainActivity ?: return
        val json = JSONObject()
            .put("type", "registerDrink")
            .put("drinkType", drinkType)
            .put("drinkValue", drinkValue)
            .put("pawnName", main.playerName)

        main.sendJson(json.toString())
        Log.d("ShopFragment", "Requested drink registration: $drinkType (+$drinkValue drinks)")
    }

    private fun setupNextWeapon() {
        nextWeapon?.let { weapon ->
            val name = weapon["name"] as? String ?: "Unknown Weapon"
            val price = weapon["price"] as? Int ?: 0
            val imageBase64 = weapon["image"] as? String
            val description = weapon["description"] as? String ?: ""
            val backgroundColor = when (val c = weapon["backgroundColor"]) {
                is Int -> c
                is Double -> c.toInt()
                is String -> {
                    try {
                        if (c.startsWith("#")) Color.parseColor(c)
                        else c.toInt()
                    } catch (_: Exception) {
                        Color.parseColor("#FF6B35")
                    }
                }
                else -> Color.parseColor("#FF6B35")
            }

            Log.d("WS", "SETUP IMAGE: $imageBase64")
            Log.d("WS", "SETUP COLOR: $backgroundColor")


            nextWeaponButton.text = buildItemText(name, price, description)

            nextWeaponButton.post {
                nextWeaponButton.backgroundTintList = null
                applyButtonBackground(nextWeaponButton, imageBase64, backgroundColor)
            }

            // Set click listener for purchase
            nextWeaponButton.setOnClickListener {
                requestWeaponPurchase()
            }

            // Enable/disable based on affordability
            val canAfford = currentCurrency >= price
            nextWeaponButton.isEnabled = canAfford
            nextWeaponButton.alpha = if (canAfford) 1.0f else 0.6f

        } ?: run {
            // No next weapon available
            nextWeaponButton.text = "All Weapons Unlocked!"
            nextWeaponButton.isEnabled = false
            nextWeaponButton.alpha = 0.6f
        }
    }

    private fun setupRerollButton() {
        rerollWeaponButton.text = "🎲 Reroll Weapon (💰 $rerollCost Drinks)"

        // Enable/disable based on affordability and weapon availability
        val canAfford = currentCurrency >= rerollCost
        val hasWeapon = nextWeapon != null

        rerollWeaponButton.isEnabled = canAfford && hasWeapon
        rerollWeaponButton.alpha = if (canAfford && hasWeapon) 1.0f else 0.6f

        // Set click listener for reroll
        rerollWeaponButton.setOnClickListener {
            requestWeaponReroll()
        }

        // Hide reroll button if no weapon available
        rerollWeaponButton.visibility = if (hasWeapon) View.VISIBLE else View.GONE
    }

    private fun setupShopItems() {
        itemsContainer.removeAllViews()

        if (shopItems.isEmpty()) {
            val emptyText = TextView(requireContext()).apply {
                text = "No items available"
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            itemsContainer.addView(emptyText)
            return
        }

        // Create rows with 2 items each
        var currentRow: LinearLayout? = null
        for ((index, item) in shopItems.withIndex()) {
            if (index % 2 == 0) {
                currentRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    isBaselineAligned = false
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dpToPx(12)
                    }
                }
                itemsContainer.addView(currentRow)
            }

            val itemButton = createItemButton(item, index % 2 == 0)
            currentRow?.addView(itemButton)

            // Add placeholder to keep columns aligned when item count is odd
            if (index % 2 == 0 && index == shopItems.lastIndex) {
                currentRow?.addView(createPlaceholderButton())
            }
        }
    }

    private fun createItemButton(item: Map<String, Any?>, isLeftColumn: Boolean): MaterialButton {
        val name = item["name"] as? String ?: "Unknown Item"
        val price = item["price"] as? Int ?: 0
        val imageBase64 = (item["image"] as? String)?.takeIf { it.isNotBlank() }
        val description = item["description"] as? String ?: ""
        val backgroundColor = when (val c = item["backgroundColor"]) {
            is Int -> c
            is Double -> c.toInt()
            is String -> {
                try {
                    if (c.startsWith("#")) Color.parseColor(c)
                    else c.toInt()
                } catch (_: Exception) {
                    Color.parseColor("#FF6B35")
                }
            }
            else -> Color.parseColor("#FF6B35")
        }

        val button = MaterialButton(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(120), 1f).apply {
                if (isLeftColumn) {
                    rightMargin = dpToPx(8)
                } else {
                    leftMargin = dpToPx(8)
                }
            }
            text = buildItemText(name, price, description)
            textSize = 14f
            setTextColor(Color.WHITE)
            isAllCaps = false
            gravity = android.view.Gravity.CENTER
            cornerRadius = dpToPx(12)
            strokeWidth = dpToPx(1)
            strokeColor = ColorStateList.valueOf(Color.argb(80, 255, 255, 255))
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
        }

        button.post {
            applyButtonBackground(button, imageBase64, backgroundColor)
        }

        val canAfford = currentCurrency >= price
        button.isEnabled = canAfford
        button.alpha = if (canAfford) 1.0f else 0.6f

        button.setOnClickListener {
            requestItemPurchase(name)
        }

        return button
    }

    private fun createPlaceholderButton(): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(120), 1f)
            visibility = View.INVISIBLE
        }
    }

    //@RequiresApi(Build.VERSION_CODES.Q)
    private fun applyButtonBackground(
        button: MaterialButton,
        imageBase64: String?,
        fallbackColor: Int
    ) {
        val cornerRadius = (button.cornerRadius.takeIf { it > 0 } ?: dpToPx(12)).toFloat()
        val rippleColor = button.rippleColor
            ?: ColorStateList.valueOf(Color.argb(60, 255, 255, 255))
        Log.d("WS","Creating a button")
        Log.d("WS", "$imageBase64")
        fun applyBaseBackground() {
            Log.d("WS", "Falling back to base background!!!")
            val basePanel = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = cornerRadius
                setColor(fallbackColor)
            }

            val rippleBackground = RippleDrawable(rippleColor, basePanel, basePanel)
            button.backgroundTintList = null
            button.background = rippleBackground
        }

        if (imageBase64.isNullOrEmpty()) {
            applyBaseBackground()
            return
        }

        try {
            val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap == null) {
                applyBaseBackground()
                return
            }

            val basePanel = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = cornerRadius
                setColor(fallbackColor)
            }

            val pixelArtDrawable = BitmapDrawable(resources, bitmap).apply {
                alpha = 180
                isFilterBitmap = false
                setAntiAlias(false)
                setDither(false)

                setTint(Color.WHITE)
                setTintMode(BlendMode.SRC_ATOP)
            }

            val bw = button.width
            val bh = button.height

            val maxSize = (minOf(bw, bh) * 0.6f).toInt()

            val iw = bitmap.width
            val ih = bitmap.height

            val scale = minOf(
                maxSize.toFloat() / iw,
                maxSize.toFloat() / ih
            )

            val dw = (iw * scale).toInt()
            val dh = (ih * scale).toInt()

            val left = (bw - dw) / 2
            val top = (bh - dh) / 2

            pixelArtDrawable.setBounds(
                left,
                top,
                left + dw,
                top + dh
            )


            val layeredBackground = LayerDrawable(arrayOf(basePanel, pixelArtDrawable))

            val rippleBackground = RippleDrawable(rippleColor, layeredBackground, basePanel)

            button.backgroundTintList = null
            button.background = rippleBackground

        } catch (e: Exception) {
            Log.e("ShopFragment", "Failed to load button background", e)
            applyBaseBackground()
        }
    }


    private fun buildItemText(name: String, price: Int, description: String): String {
        val lines = mutableListOf("$name", "💰 $price Drinks")
        if (description.isNotBlank()) {
            lines.add(description)
        }
        return lines.joinToString("\n")
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    private fun requestWeaponPurchase() {
        nextWeapon?.let { weapon ->
            val main = activity as? MainActivity ?: return
            val json = JSONObject()
                .put("type", "purchaseRequest")
                .put("itemType", "weapon")
                .put("itemName", weapon["name"])
                .put("pawnName", main.playerName)
                .put("price", weapon["price"])

            main.sendJson(json.toString())
            Log.d("ShopFragment", "Requested weapon purchase: ${weapon["name"]}")
        }
    }

    private fun requestWeaponReroll() {
        val main = activity as? MainActivity ?: return
        val json = JSONObject()
            .put("type", "rerollRequest")
            .put("rerollType", "weapon")
            .put("pawnName", main.playerName)
            .put("cost", rerollCost)

        main.sendJson(json.toString())
        Log.d("ShopFragment", "Requested weapon reroll, cost: $rerollCost")
    }

    private fun requestItemPurchase(itemName: String) {
        val item = shopItems.find { it["name"] == itemName } ?: return
        val main = activity as? MainActivity ?: return

        val json = JSONObject()
            .put("type", "purchaseRequest")
            .put("itemType", "item")
            .put("itemName", itemName)
            .put("pawnName", main.playerName)
            .put("price", item["price"])

        main.sendJson(json.toString())
        Log.d("ShopFragment", "Requested item purchase: $itemName")
    }

    // Public method to update shop data
    fun updateShop(currency: Int, nextWeapon: Map<String, Any?>?, items: List<Map<String, Any?>>, rerollCost: Int = 25) {
        if (!isAdded) return

        this.currentCurrency = currency
        this.nextWeapon = nextWeapon
        this.shopItems = items
        this.rerollCost = rerollCost

        populateShop()
    }

    // Public method to update just currency (from statUpdate)
    fun updateCurrency(newCurrency: Int) {
        if (!isAdded) return

        this.currentCurrency = newCurrency
        currencyText.text = "💰 $newCurrency Drinks"

        // Update affordability of items
        populateShop()
    }

    override fun onResume() {
        super.onResume()
        loadShopData()
        populateShop()
    }
}
