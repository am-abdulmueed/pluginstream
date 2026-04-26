package com.lagradost.cloudstream3.ui.offers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentOfferDetailBinding
import com.lagradost.cloudstream3.ui.BaseFragment
import com.lagradost.cloudstream3.ui.offers.model.CpaOffer
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.utils.ImageLoader.loadImage
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

class OfferDetailFragment : BaseFragment<FragmentOfferDetailBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentOfferDetailBinding::inflate)
) {

    private var offer: CpaOffer? = null

    companion object {
        const val OFFER_ID_KEY = "offer_id"
        const val OFFER_TITLE_KEY = "offer_title"
        const val OFFER_AMOUNT_KEY = "offer_amount"
        const val OFFER_LINK_KEY = "offer_link"
        const val OFFER_DESCRIPTION_KEY = "offer_description"
        const val OFFER_CONVERSION_KEY = "offer_conversion"
        const val OFFER_DEVICE_KEY = "offer_device"
        const val OFFER_CURRENCY_KEY = "offer_currency"
        const val OFFER_IMAGE_KEY = "offer_image"
        const val OFFER_COUNTRIES_KEY = "offer_countries"
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    override fun onBindingCreated(binding: FragmentOfferDetailBinding) {
        // Get offer data from arguments
        offer = CpaOffer(
            id = arguments?.getInt(OFFER_ID_KEY) ?: 0,
            title = arguments?.getString(OFFER_TITLE_KEY) ?: "",
            description = arguments?.getString(OFFER_DESCRIPTION_KEY),
            conversion = arguments?.getString(OFFER_CONVERSION_KEY),
            device = arguments?.getString(OFFER_DEVICE_KEY),
            link = arguments?.getString(OFFER_LINK_KEY) ?: "",
            amount = arguments?.getDouble(OFFER_AMOUNT_KEY) ?: 0.0,
            payoutCurrency = arguments?.getString(OFFER_CURRENCY_KEY),
            creatives = null,
            countries = arguments?.getStringArrayList(OFFER_COUNTRIES_KEY),
            dailyCap = null,
            isFastPay = null,
            previewLink = null,
            payoutType = null,
            epc = null,
            offerRank = null,
            payoutsPerCountry = null
        )

        offer?.let { displayOffer(binding, it) }

        // Setup SwipeRefresh
        binding.detailSwipeRefresh.setOnRefreshListener {
            // Just stop the refreshing animation after a delay
            // as data is passed via arguments
            binding.detailSwipeRefresh.postDelayed({
                binding.detailSwipeRefresh.isRefreshing = false
            }, 1000)
        }
    }

    private fun displayOffer(binding: FragmentOfferDetailBinding, offer: CpaOffer) {
        // Load image
        val imageUrl = arguments?.getString(OFFER_IMAGE_KEY)
        if (!imageUrl.isNullOrEmpty()) {
            binding.detailOfferImage.loadImage(imageUrl)
        } else {
            binding.detailOfferImage.setImageResource(R.drawable.ic_placeholder)
        }

        // Set title
        binding.detailOfferTitle.text = offer.title

        // Install button click - open link in browser
        binding.detailInstallButton.setOnClickListener {
            openOfferLink(offer.link)
        }
        
        // Share button click - share offer with friends
        binding.detailShareButton.setOnClickListener {
            shareOffer(offer)
        }

        // Set country info
        if (!offer.countries.isNullOrEmpty()) {
            binding.detailCountryLayout.visibility = View.VISIBLE
            val firstCountry = offer.countries.first()
            binding.detailCountryFlag.text = getCountryFlag(firstCountry)
            binding.detailCountryName.text = getCountryName(firstCountry)
        }

        // Set device info
        if (!offer.device.isNullOrEmpty()) {
            binding.detailDeviceLayout.visibility = View.VISIBLE
            val deviceIcon = when (offer.device.lowercase()) {
                "android" -> R.drawable.ic_android
                "ios" -> R.drawable.ic_ios
                "desktop" -> R.drawable.ic_desktop
                "mobile" -> R.drawable.ic_mobile
                else -> R.drawable.ic_device
            }
            binding.detailDeviceIcon.setImageResource(deviceIcon)
            binding.detailDeviceName.text = when (offer.device.lowercase()) {
                "android" -> "Android"
                "ios" -> "iOS (iPhone/iPad)"
                "desktop" -> "Desktop"
                "mobile" -> "Mobile"
                else -> offer.device
            }
        }

        // Set description
        binding.detailDescription.text = offer.description ?: "Complete this offer to earn reward."

        // Set conversion requirements
        if (!offer.conversion.isNullOrEmpty()) {
            binding.detailConversionLayout.visibility = View.VISIBLE
            binding.detailConversion.text = offer.conversion
        }
    }

    private fun openOfferLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Share offer with friends
     * Format: Icon + Name + Supported Devices + Description + Link
     */
    private fun shareOffer(offer: CpaOffer) {
        try {
            // Build share message
            val shareMessage = buildShareMessage(offer)
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this offer: ${offer.title}")
                putExtra(Intent.EXTRA_TEXT, shareMessage)
            }
            
            // Show share chooser
            val chooserTitle = "Share Offer via"
            startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Build complete share message with all offer details
     */
    private fun buildShareMessage(offer: CpaOffer): String {
        val imageUrl = arguments?.getString(OFFER_IMAGE_KEY)
        
        // Build the message
        val message = buildString {
            // Offer Name/Title
            appendLine("🎁 *${offer.title}*")
            appendLine()
            
            // Supported Devices
            if (!offer.device.isNullOrEmpty()) {
                val deviceIcon = when (offer.device.lowercase()) {
                    "android" -> "🤖"
                    "ios" -> "🍎"
                    "desktop" -> "💻"
                    "mobile" -> "📱"
                    else -> "📲"
                }
                val deviceName = when (offer.device.lowercase()) {
                    "android" -> "Android"
                    "ios" -> "iOS (iPhone/iPad)"
                    "desktop" -> "Desktop"
                    "mobile" -> "Mobile"
                    else -> offer.device
                }
                appendLine("$deviceIcon *Supported Device:* $deviceName")
                appendLine()
            }
            
            // Countries
            if (!offer.countries.isNullOrEmpty()) {
                val firstCountry = offer.countries.first()
                val countryFlag = getCountryFlag(firstCountry)
                val countryName = getCountryName(firstCountry)
                appendLine("$countryFlag *Available in:* $countryName")
                appendLine()
            }
            
            // Description
            appendLine("📝 *Description:*")
            appendLine(offer.description ?: "Complete this offer to earn reward.")
            appendLine()
            
            // Offer Link
            appendLine("🔗 *Get this offer:*")
            appendLine(offer.link)
            appendLine()
            
            // App promo
            appendLine("──────────────")
            appendLine("📲 Download from PluginStream Max")
            appendLine("🌐 https://pluginstream.pages.dev")
        }
        
        return message
    }
    
    private fun getCountryFlag(countryCode: String): String {
        val codePoints = countryCode
            .uppercase()
            .map { char ->
                Character.codePointAt("$char", 0) + 127397
            }
        return String(codePoints.toIntArray(), 0, codePoints.size)
    }

    private fun getCountryName(countryCode: String): String {
        val countryNames = mapOf(
            "US" to "United States",
            "CA" to "Canada",
            "GB" to "United Kingdom",
            "AU" to "Australia",
            "NZ" to "New Zealand",
            "DE" to "Germany",
            "FR" to "France",
            "IN" to "India",
            "BR" to "Brazil",
            "MX" to "Mexico"
        )
        return countryNames[countryCode.uppercase()] ?: countryCode.uppercase()
    }
}
