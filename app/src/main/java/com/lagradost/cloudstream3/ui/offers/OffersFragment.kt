package com.lagradost.cloudstream3.ui.offers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentOffersBinding
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.offers.model.CpaOffer
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class OffersFragment : BaseFragment<FragmentOffersBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentOffersBinding::inflate)
) {
    private val viewModel: OffersViewModel by viewModels()
    private lateinit var adapter: OffersAdapter
    private var isDebugPanelVisible = false

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentOffersBinding) {
        // Setup RecyclerView
        adapter = OffersAdapter { offer ->
            navigateToOfferDetail(offer)
        }

        binding.offersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@OffersFragment.adapter
        }

        // Observe offers data
        viewModel.offers.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    showLoading()
                }
                is Resource.Success -> {
                    showOffers(resource.value)
                }
                is Resource.Failure -> {
                    showError(resource.errorString)
                }
            }
        }

        // Observe debug logs
        viewModel.debugLogs.observe(viewLifecycleOwner) { logs ->
            binding.debugLogText.text = logs
        }

        // Retry button
        binding.offersRetryButton.setOnClickListener {
            loadOffers()
        }

        // Setup SwipeRefresh
        binding.offersSwipeRefresh.setOnRefreshListener {
            loadOffers(forceRefresh = true)
        }

        // Debug toggle button - Only visible in DEBUG builds
        binding.debugToggleButton.visibility = if (com.lagradost.cloudstream3.BuildConfig.DEBUG) View.VISIBLE else View.GONE
        binding.debugToggleButton.setOnClickListener {
            toggleDebugPanel()
        }

        // Close debug button
        binding.closeDebugButton.setOnClickListener {
            toggleDebugPanel()
        }

        // Copy debug log button
        binding.copyDebugLogButton.setOnClickListener {
            copyDebugLogs()
        }

        // Load offers - Only if data is not already loaded
        if (viewModel.offers.value !is Resource.Success) {
            loadOffers()
        }
    }

    private fun toggleDebugPanel() {
        isDebugPanelVisible = !isDebugPanelVisible
        binding?.debugLogPanel?.visibility = if (isDebugPanelVisible) View.VISIBLE else View.GONE
    }

    private fun copyDebugLogs() {
        val logs = viewModel.getDebugLogs()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Debug Logs", logs)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Debug logs copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun loadOffers(forceRefresh: Boolean = false) {
        hideOfflineScreen()
        viewModel.fetchOffers(requireContext(), forceRefresh)
    }

    private fun showLoading() {
        if (binding?.offersSwipeRefresh?.isRefreshing == true) return

        binding?.offersProgressBar?.visibility = View.VISIBLE
        binding?.offersSwipeRefresh?.visibility = View.GONE
        binding?.offersEmptyLayout?.visibility = View.GONE
        hideOfflineScreen()
    }

    private fun showOffers(offers: List<CpaOffer>) {
        binding?.offersProgressBar?.visibility = View.GONE
        binding?.offersSwipeRefresh?.isRefreshing = false
        binding?.offersSwipeRefresh?.visibility = View.VISIBLE
        hideOfflineScreen()

        if (offers.isEmpty()) {
            binding?.offersEmptyLayout?.visibility = View.VISIBLE
        } else {
            binding?.offersEmptyLayout?.visibility = View.GONE
            adapter.submitList(offers)
        }
    }

    private fun showError(message: String?) {
        binding?.offersProgressBar?.visibility = View.GONE
        binding?.offersSwipeRefresh?.visibility = View.GONE
        binding?.offersSwipeRefresh?.isRefreshing = false
        binding?.offersEmptyLayout?.visibility = View.GONE
        showOfflineScreen()
    }

    private fun showOfflineScreen() {
        binding?.offlineScreen?.visibility = View.VISIBLE
        binding?.offlineShimmer?.startShimmer()
    }

    private fun hideOfflineScreen() {
        binding?.offlineScreen?.visibility = View.GONE
        binding?.offlineShimmer?.stopShimmer()
    }

    private fun navigateToOfferDetail(offer: CpaOffer) {
        val bundle = Bundle().apply {
            putInt(OfferDetailFragment.OFFER_ID_KEY, offer.id)
            putString(OfferDetailFragment.OFFER_TITLE_KEY, offer.title)
            putDouble(OfferDetailFragment.OFFER_AMOUNT_KEY, offer.amount)
            putString(OfferDetailFragment.OFFER_LINK_KEY, offer.link)
            putString(OfferDetailFragment.OFFER_DESCRIPTION_KEY, offer.description)
            putString(OfferDetailFragment.OFFER_CONVERSION_KEY, offer.conversion)
            putString(OfferDetailFragment.OFFER_DEVICE_KEY, offer.device)
            putString(OfferDetailFragment.OFFER_CURRENCY_KEY, offer.payoutCurrency)
            putString(OfferDetailFragment.OFFER_IMAGE_KEY, offer.creatives?.url)
            putStringArrayList(OfferDetailFragment.OFFER_COUNTRIES_KEY, ArrayList(offer.countries ?: emptyList()))
        }

        findNavController().navigate(R.id.action_offers_to_offer_detail, bundle)
    }
}
