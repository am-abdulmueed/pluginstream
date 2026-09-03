package com.lagradost.cloudstream3.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.DialogDisclaimerBinding

import com.lagradost.cloudstream3.utils.AppUtils
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin

class DisclaimerDialog : BottomSheetDialogFragment() {
    private data class DisclaimerItem(
        val version: String,
        val markdown: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        val binding = DialogDisclaimerBinding.inflate(LayoutInflater.from(requireContext()))

        val markwon = Markwon.builder(requireContext())
            .usePlugin(LinkifyPlugin.create())
            .build()

        val disclaimerMarkdown = try {
            val jsonString = requireContext().assets.open("disclaimer.json").bufferedReader().use { it.readText() }
            val items = AppUtils.parseJson<List<DisclaimerItem>>(jsonString)
            items.firstOrNull()?.markdown ?: getString(R.string.legal_notice_text)
        } catch (e: Exception) {
            getString(R.string.legal_notice_text)
        }

        markwon.setMarkdown(binding.disclaimerText, disclaimerMarkdown)
        binding.titleText.text = getString(R.string.legal_notice)

        // X button removed from layout (commented in dialog_disclaimer.xml)
        // binding.closeButton.setOnClickListener {
        //     dismiss()
        // }

        dialog.setContentView(binding.root)

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<android.view.View>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        return dialog
    }

    companion object {
        fun newInstance(): DisclaimerDialog {
            return DisclaimerDialog()
        }
    }
}
