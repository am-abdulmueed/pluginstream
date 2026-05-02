package com.lagradost.cloudstream3.ui

import android.os.Bundle
import android.view.View
import com.lagradost.cloudstream3.databinding.FragmentEmptyBinding

class EmptyFragment : BaseFragment<FragmentEmptyBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentEmptyBinding::inflate)
) {
    override fun fixLayout(view: View) {
    }

    override fun onBindingCreated(binding: FragmentEmptyBinding) {
        // Deliberate crash for testing purposes
        throw RuntimeException("Test crash: Protube tab opened!")
    }
}