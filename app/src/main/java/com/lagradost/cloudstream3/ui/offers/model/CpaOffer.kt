package com.lagradost.cloudstream3.ui.offers.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * CPAlead Offers API Response
 */
data class OffersResponse(
    @JsonProperty("status") val status: String,
    @JsonProperty("number_offers") val numberOffers: Int,
    @JsonProperty("country") val country: String?,
    @JsonProperty("devices") val devices: String?,
    @JsonProperty("offers") val offers: List<CpaOffer>
)

/**
 * Individual CPA Offer
 */
data class CpaOffer(
    @JsonProperty("id") val id: Int,
    @JsonProperty("title") val title: String,
    @JsonProperty("description") val description: String?,
    @JsonProperty("conversion") val conversion: String?,
    @JsonProperty("device") val device: String?,
    @JsonProperty("daily_cap") val dailyCap: Int?,
    @JsonProperty("is_fast_pay") val isFastPay: Boolean?,
    @JsonProperty("link") val link: String,
    @JsonProperty("preview_link") val previewLink: String?,
    @JsonProperty("amount") val amount: Double,
    @JsonProperty("payout_currency") val payoutCurrency: String?,
    @JsonProperty("payout_type") val payoutType: String?,
    @JsonProperty("countries") val countries: List<String>?,
    @JsonProperty("epc") val epc: Double?,
    @JsonProperty("creatives") val creatives: OfferCreatives?,
    @JsonProperty("offer_rank") val offerRank: Int?,
    @JsonProperty("payouts_per_country") val payoutsPerCountry: Map<String, Double>?
)

/**
 * Offer Creative (Image)
 */
data class OfferCreatives(
    @JsonProperty("url") val url: String?
)
