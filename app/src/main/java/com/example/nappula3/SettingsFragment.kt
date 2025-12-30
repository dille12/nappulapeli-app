package com.example.nappula3

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONObject

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val switchTeamNameButton: Button = view.findViewById(R.id.switchTeamNameButton)
        switchTeamNameButton.setOnClickListener {
            showTeamNameDialog()
        }

        return view
    }

    private fun showTeamNameDialog() {
        if (!isAdded) return

        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.settings_team_name_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setPadding(48, 48, 48, 48)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_switch_team_name_title))
            .setMessage(getString(R.string.settings_switch_team_name_message))
            .setView(editText)
            .setPositiveButton(getString(R.string.common_ok)) { _, _ ->
                val newTeamName = editText.text.toString().trim()
                if (newTeamName.isNotEmpty()) {
                    requestTeamNameSwitch(newTeamName)
                }
            }
            .setNegativeButton(getString(R.string.common_cancel), null)
            .show()
    }

    private fun requestTeamNameSwitch(newTeamName: String) {
        val main = activity as? MainActivity ?: return

        val json = JSONObject()
            .put("type", "teamNameSwitch")
            .put("playerName", main.playerName ?: getString(R.string.common_unknown))
            .put("teamName", newTeamName)

        main.sendJson(json.toString())
        Toast.makeText(requireContext(), getString(R.string.settings_team_name_sent), Toast.LENGTH_SHORT).show()
    }
}
