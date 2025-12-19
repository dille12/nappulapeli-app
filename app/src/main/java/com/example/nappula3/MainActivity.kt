package com.example.nappula3

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import androidx.activity.OnBackPressedCallback



class MainActivity : AppCompatActivity() {

    companion object {
        private const val KEY_LATEST_IP = "latest_ip"
    }

    var playerName: String? = null
    var playerImageBase64: String? = null

    val playerStats = mutableMapOf<String, Any>()

    var pendingLevelUpItems: MutableList<Pair<String, String>> = mutableListOf()
    var pendingLevelUpPawn: String? = null

    var pendingLevelUp: Boolean? = false

    var latestHudLines: List<Map<String, Any>> = listOf()

    var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    var latestIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        latestIp = savedInstanceState?.getString(KEY_LATEST_IP)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing = back always disabled
                }
            })

        playerStats["XP"] = 0
        playerStats["Level"] = 1
        playerStats["XP to next level"] = 10

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .applyFadeAnimations()
                .replace(R.id.fragment_container, ConnectionFragment())
                .commit()
        }

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_LATEST_IP, latestIp)
    }

    fun notifyConnectionSuccess() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ConnectionFragment) {
            fragment.onConnected()
        }
    }

    fun notifyConnectionFailed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is ConnectionFragment) {
            fragment.onConnectionFailed()
        }
    }


    fun connectWebSocket(ip: String) {
        if (ip.isBlank()) {
            Log.e("WS", "Refusing to connect: IP is blank")
            notifyConnectionFailed()
            return
        }
        latestIp = ip
        Log.d("WS", "ws://$ip:8765") //
        val request = Request.Builder()
            .url("ws://$ip:8765")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                runOnUiThread {
                    notifyConnectionSuccess()
                }

            }

            override fun onMessage(ws: WebSocket, text: String) {
                runOnUiThread {
                    handleMessage(text)
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                webSocket = null
                runOnUiThread {
                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (fragment is ConnectionFragment) {
                        fragment.onConnectionFailed()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .applyFadeAnimations()
                            .replace(R.id.fragment_container, ConnectionFragment.newInstance(latestIp))
                            .runOnCommit {
                                val connectionFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                                if (connectionFragment is ConnectionFragment) {
                                    connectionFragment.onConnectionFailed()
                                }
                            }
                            .commit()
                    }
                }

            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
                runOnUiThread {
                    val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (fragment is ConnectionFragment) {
                        fragment.onConnectionFailed()
                    } else {
                        supportFragmentManager.beginTransaction()
                            .applyFadeAnimations()
                            .replace(R.id.fragment_container, ConnectionFragment.newInstance(latestIp))
                            .runOnCommit {
                                val connectionFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                                if (connectionFragment is ConnectionFragment) {
                                    connectionFragment.onConnectionFailed()
                                }
                            }
                            .commit()
                    }
                }
            }

        })
    }

    // Helper function to send JSON
    fun sendJson(json: String) {
        Log.d("WS", "Sending a JSON")
        webSocket?.send(json)
    }

    // Callback for incoming messages
    // Updated methods for MainActivity.kt


    // Updated methods for MainActivity.kt

    // Add these new variables to your MainActivity class
    var mostKilledBy: Map<String, Any?>? = null  // nemesis data
    var mostKilled: Map<String, Any?>? = null    // victim data
    var nextWeapon: Map<String, Any?>? = null    // next weapon in gun game progression
    var shopItems: List<Map<String, Any?>> = emptyList()  // available shop items

    private fun handleMessage(message: String) {

        Log.d("WS", "recv: $message")

        val json = try { JSONObject(message) } catch (e: Exception) {
            Log.e("WS", "Invalid JSON", e); return
        }

        when (json.optString("type")) {
            "completePawn" -> {
                val name = json.optString("name")
                // Save data
                playerName = name
                playerImageBase64 = json.optString("image")
                val colorArray = json.getJSONArray("teamColor")
                val r = (colorArray.getInt(0) * 0.25).toInt()
                val g = (colorArray.getInt(1) * 0.25).toInt()
                val b = (colorArray.getInt(2) * 0.25).toInt()
                // Convert to Android color int
                val colorInt = android.graphics.Color.rgb(r, g, b)

                // Check if GameFragment is already showing
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (currentFragment is GameFragment) {
                    // Just refresh the existing fragment
                    currentFragment.refreshData()
                } else {
                    // Create new GameFragment
                    val fragment = GameFragment.newInstance(colorInt)

                    // Transition to GameFragment
                    supportFragmentManager.beginTransaction()
                        .applyFadeAnimations()
                        .replace(R.id.fragment_container, fragment)
                        .commit()
                }
            }

            "statUpdate" -> {
                val statsObj = json.getJSONObject("stats")

                for (key in statsObj.keys()) {
                    val raw = statsObj.get(key)

                    val value = when (raw) {
                        is Int -> raw
                        is Long -> {
                            if (raw in Int.MIN_VALUE..Int.MAX_VALUE) raw.toInt() else raw
                        }
                        is Double -> {
                            if (raw % 1.0 == 0.0) raw.toInt() else raw.toFloat()
                        }
                        is Boolean -> raw
                        is String -> raw
                        else -> raw.toString()
                    }

                    playerStats[key] = value
                }

                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.updateStats(playerStats)
                }
            }


            "hudInfo" -> {
                val linesJson = json.getJSONArray("lines")
                val hudLines = mutableListOf<Map<String, Any>>()
                for (i in 0 until linesJson.length()) {
                    val obj = linesJson.getJSONObject(i)
                    val text = obj.getString("text")
                    val colorObj = obj.getJSONObject("color")
                    val r = colorObj.getInt("r")
                    val g = colorObj.getInt("g")
                    val b = colorObj.getInt("b")
                    val colorInt = Color.rgb(r, g, b)
                    hudLines.add(mapOf("text" to text, "color" to colorInt))
                }
                latestHudLines = hudLines

                Log.d("WS", "Updated HUD data with ${hudLines.size} items")

                // Update GameFragment HUD if it's currently showing
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.updateHudData(hudLines)
                }
            }

            "rivalryInfo" -> {
                // Parse nemesis (most killed by)
                if (json.has("mostKilledBy") && !json.isNull("mostKilledBy")) {
                    val nemesisObj = json.getJSONObject("mostKilledBy")
                    mostKilledBy = mapOf(
                        "name" to nemesisObj.optString("name"),
                        "image" to nemesisObj.optString("image"),
                        "kills" to nemesisObj.optInt("kills", 0)
                    )
                } else {
                    mostKilledBy = null
                }

                // Parse victim (most killed)
                if (json.has("mostKilled") && !json.isNull("mostKilled")) {
                    val victimObj = json.getJSONObject("mostKilled")
                    mostKilled = mapOf(
                        "name" to victimObj.optString("name"),
                        "image" to victimObj.optString("image"),
                        "kills" to victimObj.optInt("kills", 0)
                    )
                } else {
                    mostKilled = null
                }

                Log.d("WS", "Updated rivalry data - Nemesis: ${mostKilledBy?.get("name")}, Victim: ${mostKilled?.get("name")}")

                // Update GameFragment rivalry data if it's currently showing
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.updateRivalryData(mostKilledBy, mostKilled)
                }
            }

            "levelUpChoices" -> {
                val itemsArray = json.getJSONArray("items")
                val pawnName = json.optString("pawn")

                val items = mutableListOf<Pair<String, String>>()  // name + description
                for (i in 0 until itemsArray.length()) {
                    val obj = itemsArray.getJSONObject(i)
                    val name = obj.optString("name")
                    val desc = obj.optString("desc")
                    items.add(Pair(name, desc))
                }

                // Save these choices somewhere accessible by fragment
                pendingLevelUpItems = items
                pendingLevelUpPawn = pawnName
                pendingLevelUp = true

                // The GameFragment will automatically detect this and switch to level up tab
            }

            "shopUpdate" -> {
                // Parse next weapon



                if (json.has("nextWeapon") && !json.isNull("nextWeapon")) {
                    val weaponObj = json.getJSONObject("nextWeapon")
                    nextWeapon = mapOf(
                        "name" to weaponObj.optString("name"),
                        "price" to weaponObj.optInt("price", 0),
                        "image" to weaponObj.optString("image"),
                        "backgroundColor" to parseColorValue(weaponObj.opt("backgroundColor"))
                    )
                } else {
                    nextWeapon = null
                }

                // Parse shop items
                val itemsArray = json.optJSONArray("items")
                val items = mutableListOf<Map<String, Any?>>()
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {

                        val itemObj = itemsArray.getJSONObject(i)
                        Log.d("WS", itemObj.optString("name"))
                        items.add(mapOf(
                            "name" to itemObj.optString("name"),
                            "price" to itemObj.optInt("price", 0),
                            "image" to itemObj.optString("image"),
                            "description" to itemObj.optString("description", ""),
                            "backgroundColor" to parseColorValue(itemObj.opt("backgroundColor"))
                        ))
                    }
                }
                shopItems = items

                Log.d("WS", "Updated shop data - Next weapon: ${nextWeapon?.get("name")}, Items: ${items.size}")

                // Update GameFragment shop if it's currently showing
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.updateShopData(nextWeapon, shopItems)
                }
            }

            "rerollResponse" -> {
                val success = json.optBoolean("success", false)
                val message = json.optString("message", "")
                val rerollType = json.optString("rerollType", "weapon")

                Log.d("WS", "Reroll response: $rerollType - Success: $success - Message: $message")

                // Update GameFragment with reroll result
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.handleRerollResponse(success, rerollType, message)
                }
            }

            "drinkRegistrationResponse" -> {
                val success = json.optBoolean("success", false)
                val drinkType = json.optString("drinkType")
                val drinkValue = json.optInt("drinkValue", 0)
                val message = json.optString("message", "")

                Log.d("WS", "Drink registration response: $drinkType (+$drinkValue) - Success: $success - Message: $message")

                // Update GameFragment with registration result
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.handleDrinkRegistrationResponse(success, drinkType, drinkValue, message)
                }
            }

            "purchaseResponse" -> {
                val success = json.optBoolean("success", false)
                val itemName = json.optString("itemName")
                val message = json.optString("message", "")

                Log.d("WS", "Purchase response: $itemName - Success: $success - Message: $message")

                // Update GameFragment with purchase result
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.handlePurchaseResponse(success, itemName, message)
                }
            }

            "teamSwitch" -> {
                val colorArray = json.getJSONArray("newTeamColor")
                val r = (colorArray.getInt(0) * 0.25).toInt()
                val g = (colorArray.getInt(1) * 0.25).toInt()
                val b = (colorArray.getInt(2) * 0.25).toInt()
                // Convert to Android color int
                val newColorInt = android.graphics.Color.rgb(r, g, b)

                Log.d("WS", "Team switch! New color: RGB($r, $g, $b)")

                // Update GameFragment background color if visible
                val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (fragment is GameFragment) {
                    fragment.updateTeamColor(newColorInt)
                }
            }

            "ult_ready" -> {
                // Dispatch to WaitingFragment or GameFragment if needed
            }

            // Add other types as needed
        }
    }

    private fun parseColorValue(value: Any?): Int? {
        return when (value) {
            is String -> {
                try {
                    Color.parseColor(value)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            is org.json.JSONObject -> {
                val r = value.optInt("r", -1)
                val g = value.optInt("g", -1)
                val b = value.optInt("b", -1)
                if (r in 0..255 && g in 0..255 && b in 0..255) {
                    Color.rgb(r, g, b)
                } else {
                    null
                }
            }
            is org.json.JSONArray -> {
                if (value.length() >= 3) {
                    val r = value.optInt(0, -1)
                    val g = value.optInt(1, -1)
                    val b = value.optInt(2, -1)
                    if (r in 0..255 && g in 0..255 && b in 0..255) {
                        Color.rgb(r, g, b)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }
}
