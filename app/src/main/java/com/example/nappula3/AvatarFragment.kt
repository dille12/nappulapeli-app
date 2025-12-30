package com.example.nappula3

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.widget.doOnTextChanged
import android.util.Base64
import android.widget.ArrayAdapter
import java.io.ByteArrayOutputStream

class AvatarFragment : Fragment() {

    private lateinit var nameInput: EditText
    private lateinit var avatarImage: ImageView
    private lateinit var selectButton: Button
    private lateinit var flipButton: Button
    private lateinit var nextButton: Button
    private lateinit var directionText: TextView
    private lateinit var teamSpinner: Spinner

    private lateinit var infoButton: Button

    private var originalBitmap: Bitmap? = null
    private var displayBitmap: Bitmap? = null
    private var isFlipped = false
    private val PICK_IMAGE = 100
    private val TARGET_SIZE = 400

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_avatar, container, false)

        nameInput = view.findViewById(R.id.nameInput)
        avatarImage = view.findViewById(R.id.avatarImage)
        selectButton = view.findViewById(R.id.selectImageButton)
        flipButton = view.findViewById(R.id.flipButton)
        nextButton = view.findViewById(R.id.nextButton)
        directionText = view.findViewById(R.id.directionText)
        infoButton = view.findViewById(R.id.infoButton)
        teamSpinner = view.findViewById(R.id.teamSpinner)



        nextButton.isEnabled = false
        flipButton.isEnabled = false

        preloadSavedAvatar()

        selectButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(gallery, PICK_IMAGE)
        }

        flipButton.setOnClickListener {
            originalBitmap?.let { bitmap ->
                isFlipped = !isFlipped
                displayBitmap = if (isFlipped) flipBitmap(bitmap) else bitmap
                avatarImage.setImageBitmap(displayBitmap)
            }
        }

        nameInput.doOnTextChanged { text, start, before, count ->
            checkNextEnabled()
        }

        nextButton.setOnClickListener {
            proceedWithAvatar()
        }
        infoButton.setOnClickListener {
            showInfoDialog()
        } 

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.avatar_team_options,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        teamSpinner.adapter = adapter
    }

    private fun preloadSavedAvatar() {
        val prefs = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val savedName = prefs.getString(MainActivity.KEY_PLAYER_NAME, null)
        val savedImage = prefs.getString(MainActivity.KEY_PLAYER_IMAGE, null)
        val savedTeam = prefs.getInt(MainActivity.KEY_PLAYER_TEAM, -1)

        if (!savedName.isNullOrBlank()) {
            nameInput.setText(savedName)
        }

        if (!savedImage.isNullOrBlank()) {
            try {
                val imageBytes = Base64.decode(savedImage, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val compressedBitmap = compressBitmap(decodedBitmap)

                originalBitmap = compressedBitmap
                displayBitmap = compressedBitmap
                isFlipped = false
                avatarImage.setImageBitmap(compressedBitmap)
                flipButton.isEnabled = true
            } catch (e: IllegalArgumentException) {
                // Ignore invalid base64 data
            }
        }

        val teamSelection = savedTeam + 1
        if (teamSelection in 0..4) {
            teamSpinner.setSelection(teamSelection)
        } else {
            teamSpinner.setSelection(0)
        }

        checkNextEnabled()
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = maxOf(width, height)

        if (size <= TARGET_SIZE) return bitmap

        val scale = TARGET_SIZE.toFloat() / size
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun showInfoDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.avatar_info_title))
            .setMessage(getString(R.string.avatar_info_message))
            .setPositiveButton(getString(R.string.common_ok)) { _, _ -> }
            .show()
    }

    private fun showDirectionConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.avatar_direction_title))
            .setMessage(getString(R.string.avatar_direction_message))
            .setPositiveButton(getString(R.string.avatar_direction_positive)) { _, _ ->
                proceedWithAvatar()
            }
            .setNegativeButton(getString(R.string.avatar_direction_negative)) { _, _ -> }
            .show()
    }

    private fun proceedWithAvatar() {
        val name = nameInput.text.toString()
        val bitmap = displayBitmap ?: return
        val team = teamSpinner.selectedItemPosition - 1

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val imageBytes = baos.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        (activity as? MainActivity)?.onAvatarSubmitted(name, base64Image, team)

        val json = """
        {
            "type": "avatar",
            "name": "$name",
            "image": "$base64Image",
            "team": $team
        }
    """.trimIndent()

        (activity as? MainActivity)?.sendJson(json)


    }

    private fun flipBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.preScale(-1f, 1f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun checkNextEnabled() {
        nextButton.isEnabled = nameInput.text.isNotBlank() && displayBitmap != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE) {
            val uri: Uri? = data?.data
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                val compressedBitmap = compressBitmap(bitmap)
                originalBitmap = compressedBitmap
                displayBitmap = compressedBitmap
                isFlipped = false
                avatarImage.setImageBitmap(compressedBitmap)
                flipButton.isEnabled = true
                checkNextEnabled()
            }
        }
    }
}
