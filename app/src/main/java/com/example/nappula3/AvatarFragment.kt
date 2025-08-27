package com.example.nappula3

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.core.widget.doOnTextChanged
import android.util.Base64
import okhttp3.WebSocket
import java.io.ByteArrayOutputStream

class AvatarFragment : Fragment() {

    private lateinit var nameInput: EditText
    private lateinit var avatarImage: ImageView
    private lateinit var selectButton: Button
    private lateinit var nextButton: Button



    private var selectedBitmap: Bitmap? = null
    private val PICK_IMAGE = 100

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_avatar, container, false)

        nameInput = view.findViewById(R.id.nameInput)
        avatarImage = view.findViewById(R.id.avatarImage)
        selectButton = view.findViewById(R.id.selectImageButton)
        nextButton = view.findViewById(R.id.nextButton)

        nextButton.isEnabled = false



        // Image selection
        selectButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(gallery, PICK_IMAGE)
        }

        // Enable Next button only if name is not empty and image is selected
        nameInput.doOnTextChanged { text, start, before, count ->
            checkNextEnabled()
        }
        nextButton.setOnClickListener {
            val name = nameInput.text.toString()
            val bitmap = selectedBitmap ?: return@setOnClickListener

            // Convert bitmap to PNG bytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val imageBytes = baos.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            // Build JSON
            val json = """
        {
            "type": "avatar",
            "name": "$name",
            "image": "$base64Image"
        }
    """.trimIndent()

            // Send JSON via MainActivity helper
            (activity as? MainActivity)?.sendJson(json)

            // Navigate to WaitingFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WaitingFragment())
                .addToBackStack(null)
                .commit()
        }


        return view
    }



    private fun checkNextEnabled() {
        nextButton.isEnabled = nameInput.text.isNotBlank() && selectedBitmap != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE) {
            val uri: Uri? = data?.data
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                avatarImage.setImageBitmap(bitmap)
                selectedBitmap = bitmap
                checkNextEnabled()
            }
        }
    }
}
