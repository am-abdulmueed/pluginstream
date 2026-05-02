package com.lagradost.cloudstream3.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.contact_developer_dialog, null)
        
        view.findViewById<View>(R.id.contact_streaming_support).setOnClickListener {
            val logs = AppDiagnostics.readLogs(50)
            val subject = "[PluginStream-Stream] Support/Request"
            val body = "Issue/Request Type: [Streaming Bug / New Provider Request]\n\nDetails:\n[Describe which link or provider is not working...]\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        view.findViewById<View>(R.id.contact_games_support).setOnClickListener {
            val logs = AppDiagnostics.readLogs(50)
            val subject = "[PluginStream-Games] New Game/Bug Report"
            val body = "Action: [Add New Game / Game Not Loading]\n\nGame Name:\nGenre:\nAdditional Info: [Provide link or description here]\n\nNote: Thanks for making the game hub better!\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        view.findViewById<View>(R.id.contact_protube_support).setOnClickListener {
            val logs = AppDiagnostics.readLogs(50)
            val subject = "[PluginStream-ProTube] Feedback/Issue"
            val body = "Feedback Type: [Video Quality / Search Issue / Feature Request]\n\nDescribe the Problem:\n[Describe what can be improved in ProTube...]\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        view.findViewById<View>(R.id.contact_general_bug).setOnClickListener {
            val logs = AppDiagnostics.readLogs(100)
            val subject = "⚠️ Bug Report - PluginStream v${BuildConfig.VERSION_NAME}"
            val body = "${AppDiagnostics.getDeviceInfo()}\n---\nProblem Description:\n[Describe the problem you are facing...]\n---\nLatest Logs:\n$logs\n\nPlease fix this as soon as possible. Thanks!"
            sendEmailIntent(requireContext(), subject, body) {
                Toast.makeText(requireContext(), "No email client found", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
        
        dialog.setContentView(view)
        return dialog
    }
}
