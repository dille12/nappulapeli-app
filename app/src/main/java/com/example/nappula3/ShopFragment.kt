package com.example.nappula3

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.json.JSONObject

class ShopFragment : Fragment() {

    private lateinit var currencyText: TextView
    private lateinit var registerDrinksButton: MaterialButton
    private lateinit var nextWeaponCard: CardView
    private lateinit var nextWeaponButton: MaterialButton
    private lateinit var rerollWeaponButton: MaterialButton
    private lateinit var itemsContainer: LinearLayout

    private var currentCurrency: Int = 0
    private var nextWeapon: Map<String, Any>? = null
    private var shopItems: List<Map<String, Any>> = emptyList()
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
        nextWeaponCard = view.findViewById(R.id.nextWeaponCard)
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
            currentCurrency = main.playerStats["Currency"] as? Int ?: 0

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

            nextWeaponButton.text = "$name\n💰 $price Drinks"

            // Set weapon image as background if available
            imageBase64?.let { base64 ->
                try {
                    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        val drawable = BitmapDrawable(resources, bitmap)
                        drawable.alpha = 128 // Make it semi-transparent as background
                        nextWeaponButton.background = drawable
                    }
                } catch (e: Exception) {
                    Log.e("ShopFragment", "Failed to load weapon image", e)
                }
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
                // Create new row
                currentRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 16
                    }
                }
                itemsContainer.addView(currentRow)
            }

            val itemCard = createItemCard(item)
            currentRow?.addView(itemCard)
        }
    }

    private fun createItemCard(item: Map<String, Any>): CardView {
        val name = item["name"] as? String ?: "Unknown Item"
        val price = item["price"] as? Int ?: 0
        val imageBase64 = item["image"] as? String
        val description = item["description"] as? String ?: ""

        val cardView = CardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                if (itemsContainer.childCount % 2 == 0) {
                    rightMargin = 8
                } else {
                    leftMargin = 8
                }
            }
            radius = 12f
            cardElevation = 4f
            setCardBackgroundColor(Color.argb(180, 45, 45, 45))
        }

        val contentLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            gravity = android.view.Gravity.CENTER
        }

        // Item image
        val itemImage = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(64, 64).apply {
                bottomMargin = 8
            }
            scaleType = ImageView.ScaleType.CENTER_CROP

            imageBase64?.let { base64 ->
                try {
                    val imageBytes = Base64.decode(base64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    if (bitmap != null) {
                        setImageBitmap(bitmap)
                    }
                } catch (e: Exception) {
                    Log.e("ShopFragment", "Failed to load item image for $name", e)
                }
            }
        }

        // Item name
        val nameText = TextView(requireContext()).apply {
            text = name
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4
            }
        }

        // Item description (if available)
        val descText = TextView(requireContext()).apply {
            text = description
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = android.view.Gravity.CENTER
            maxLines = 2
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }

        // Price and buy button
        val buyButton = MaterialButton(requireContext()).apply {
            text = "💰 $price"
            textSize = 12f
            val canAfford = currentCurrency >= price
            isEnabled = canAfford
            alpha = if (canAfford) 1.0f else 0.6f

            setOnClickListener {
                requestItemPurchase(name)
            }
        }

        contentLayout.addView(itemImage)
        contentLayout.addView(nameText)
        if (description.isNotEmpty()) {
            contentLayout.addView(descText)
        }
        contentLayout.addView(buyButton)

        cardView.addView(contentLayout)
        return cardView
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
    fun updateShop(currency: Int, nextWeapon: Map<String, Any>?, items: List<Map<String, Any>>, rerollCost: Int = 25) {
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