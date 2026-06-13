package com.lagradost.cloudstream3.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.utils.AppDiagnostics

const val EMAIL_ADDRESS = "am.abdulmueed3@gmail.com"

fun sendEmailIntent(context: Context, subject: String, body: String, onFail: () -> Unit) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        onFail()
    }
}

class ContactDeveloperDialog : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contact_developer, null)
        
        val closeButton = view.findViewById<android.widget.ImageView>(R.id.closeButton)
        val contentContainer = view.findViewById<android.widget.LinearLayout>(R.id.contentContainer)
        val inflater = LayoutInflater.from(requireContext())

        // Streaming Support
        val streamingView = inflater.inflate(R.layout.item_support_dialog, contentContainer, false)
        val streamingIcon = streamingView.findViewById<android.widget.ImageView>(R.id.itemIcon)
        val streamingTitle = streamingView.findViewById<android.widget.TextView>(R.id.itemTitle)
        val streamingDesc = streamingView.findViewById<android.widget.TextView>(R.id.itemDesc)
        
        streamingIcon.setImageResource(R.drawable.ic_baseline_play_arrow_24)
        streamingTitle.text = "Streaming Support"
        streamingDesc.text = "Report streaming bugs or request providers"
        
        streamingView.setOnClickListener {
            val logs = AppDiagnostics.readLogs(50)
            val subject = "[PluginStream-Stream] Support/Request"
            val body = "Issue/Request Type: [Streaming Bug / New Provider Request]\n\nDetails:\n[Describe which link or provider is not working...]\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        contentContainer.addView(streamingView)

        // Games Support
        val gamesView = inflater.inflate(R.layout.item_support_dialog, contentContainer, false)
        val gamesIcon = gamesView.findViewById<android.widget.ImageView>(R.id.itemIcon)
        val gamesTitle = gamesView.findViewById<android.widget.TextView>(R.id.itemTitle)
        val gamesDesc = gamesView.findViewById<android.widget.TextView>(R.id.itemDesc)
        
        gamesIcon.setImageResource(R.drawable.ic_game_selector)
        gamesTitle.text = "Games Support"
        gamesDesc.text = "Report game bugs or request new games"
        
        gamesView.setOnClickListener {
            val logs = AppDiagnostics.readLogs(50)
            val subject = "[PluginStream-Games] New Game/Bug Report"
            val body = "Action: [Add New Game / Game Not Loading]\n\nGame Name:\nGenre:\nAdditional Info: [Provide link or description here]\n\nNote: Thanks for making the game hub better!\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        contentContainer.addView(gamesView)

        // General Bug
        val generalView = inflater.inflate(R.layout.item_support_dialog, contentContainer, false)
        val generalIcon = generalView.findViewById<android.widget.ImageView>(R.id.itemIcon)
        val generalTitle = generalView.findViewById<android.widget.TextView>(R.id.itemTitle)
        val generalDesc = generalView.findViewById<android.widget.TextView>(R.id.itemDesc)
        
        generalIcon.setImageResource(R.drawable.ic_baseline_bug_report_24)
        generalTitle.text = "General Bug Report"
        generalDesc.text = "Report any other bugs"
        
        generalView.setOnClickListener {
            val logs = AppDiagnostics.readLogs(100)
            val subject = "⚠️ Bug Report - PluginStream v${BuildConfig.VERSION_NAME}"
            val body = "${AppDiagnostics.getDeviceInfo()}\n---\nProblem Description:\n[Describe the problem you are facing...]\n---\nLatest Logs:\n$logs\n\nPlease fix this as soon as possible. Thanks!"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        contentContainer.addView(generalView)

        closeButton.setOnClickListener {
            dismiss()
        }
        
        dialog.setContentView(view)
        return dialog
    }
}
