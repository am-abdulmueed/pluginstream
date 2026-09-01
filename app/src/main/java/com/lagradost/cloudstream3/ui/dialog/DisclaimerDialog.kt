package com.lagradost.cloudstream3.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.DialogDisclaimerBinding

class DisclaimerDialog : BottomSheetDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme)
        val binding = DialogDisclaimerBinding.inflate(LayoutInflater.from(requireContext()))

        binding.disclaimerText.text = getString(R.string.legal_notice_text)
        binding.titleText.text = getString(R.string.legal_notice)

        binding.closeButton.setOnClickListener {
            dismiss()
        }

        dialog.setContentView(binding.root)
        return dialog
    }

    companion object {
        fun newInstance(): DisclaimerDialog {
            return DisclaimerDialog()
        }
    }
}
