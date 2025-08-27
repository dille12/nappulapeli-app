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

    var playerName: String? = null
    var playerImageBase64: String? = null

    val playerStats = mutableMapOf<String, Any>()

    var pendingLevelUpItems: MutableList<Pair<String, String>> = mutableListOf()
    var pendingLevelUpPawn: String? = null

    var pendingLevelUp: Boolean? = false

    var latestHudLines: List<Map<String, Any>> = listOf()

    var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Do nothing = back always disabled
                }
            })

        playerStats["xp"] = 0
        playerStats["level"] = 1
        playerStats["xpToNextLevel"] = 10

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ConnectionFragment())
                .commit()
        }

    }

    override fun onResume() {
        super.onResume()
        if (webSocket == null) {
            connectWebSocket()
        }
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


    fun connectWebSocket() {
        val request = Request.Builder()
            .url("ws://192.168.0.102:8765") // change IP as needed
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
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ConnectionFragment())
                        .commit()
                    notifyConnectionFailed()
                }

            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                webSocket = null
                runOnUiThread {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ConnectionFragment())
                        .commit()
                    notifyConnectionFailed()
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

                // Pass the data to GameFragment
                val fragment = GameFragment().apply {
                    arguments = Bundle().apply {
                        putInt("bgColor", colorInt)
                    }
                }

                // Transition to GameFragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()

            }

            "statUpdate" -> {
                val statsObj = json.getJSONObject("stats")
                for (key in statsObj.keys()) {
                    val value = statsObj.getInt(key)  // or getDouble/getString depending
                    playerStats[key] = value
                }

                // Update GameFragment UI if visible
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

                Log.d("WS", "Switching to HUD")
                val hudFragment = HUDFragment.newInstance(latestHudLines)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.bottomPanelContainer, hudFragment)
                    .commit()

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

                // Show the LevelUpFragment

            }

            "ult_ready" -> {
                // Dispatch to WaitingFragment or GameFragment if needed
            }

            // Add other types as needed
        }
    }
}

