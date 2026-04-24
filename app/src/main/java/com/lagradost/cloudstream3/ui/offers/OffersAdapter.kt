package com.lagradost.cloudstream3.ui.offers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.ItemOfferBinding
import com.lagradost.cloudstream3.ui.offers.model.CpaOffer
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage

class OffersAdapter(
    private val onOfferClick: (CpaOffer) -> Unit
) : ListAdapter<CpaOffer, OffersAdapter.OfferViewHolder>(OfferDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val binding = ItemOfferBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OfferViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OfferViewHolder(
        private val binding: ItemOfferBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(offer: CpaOffer) {
            // Load offer image
            val imageUrl = offer.creatives?.url
            if (!imageUrl.isNullOrEmpty()) {
                binding.offerImage.loadImage(imageUrl)
            } else {
                binding.offerImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Set title
            binding.offerTitle.text = offer.title

            // Set amount
            binding.offerAmount.text = String.format("%.2f", offer.amount)

            // Set currency
            binding.offerCurrency.text = offer.payoutCurrency ?: "USD"

            // Set payout type
            binding.offerPayoutType.text = when (offer.payoutType?.uppercase()) {
                "CPI" -> "Install"
                "CPE" -> "Action"
                "CPR" -> "Registration"
                else -> offer.payoutType ?: "Offer"
            }

            // Click listener
            binding.root.setOnClickListener {
                onOfferClick(offer)
            }
        }
    }

    class OfferDiffCallback : DiffUtil.ItemCallback<CpaOffer>() {
        override fun areItemsTheSame(oldItem: CpaOffer, newItem: CpaOffer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CpaOffer, newItem: CpaOffer): Boolean {
            return oldItem == newItem
        }
    }
}
