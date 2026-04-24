package com.lagradost.cloudstream3.ui.offers

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.lagradost.cloudstream3.mvvm.Resource
import com.lagradost.cloudstream3.ui.offers.model.CpaOffer
import com.lagradost.cloudstream3.ui.offers.model.OffersResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OffersViewModel : ViewModel() {

    private val _offers = MutableLiveData<Resource<List<CpaOffer>>>()
    val offers: LiveData<Resource<List<CpaOffer>>> = _offers

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    companion object {
        private const val TAG = "OffersViewModel"
        private const val API_URL = "https://www.cpalead.com/api/offers"
        private const val PUBLISHER_ID = "3277747" // Replace with actual publisher ID
    }

    fun fetchOffers(context: Context) {
        viewModelScope.launch {
            _offers.postValue(Resource.Loading())

            try {
                val url = "$API_URL?id=$PUBLISHER_ID" +
                        "&country=user" +
                        "&device=user" +
                        "&fields=id,title,description,conversion,device,link,amount,offer_rank,payout_currency,payout_type,countries,creatives,payouts_per_country" +
                        "&limit=5000"

                Log.d(TAG, "Fetching offers from: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                // Execute network call on IO thread
                val response = withContext(Dispatchers.IO) {
                    client.newCall(request).execute()
                }

                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        if (responseBody != null) {
                            val offersResponse = mapper.readValue(responseBody, OffersResponse::class.java)
                            
                            if (offersResponse.status == "success") {
                                // Sort by offer_rank (lowest rank = highest priority)
                                val sortedOffers = offersResponse.offers
                                    .filter { offer -> offer.offerRank != null }
                                    .sortedBy { offer -> offer.offerRank }

                                Log.d(TAG, "Successfully fetched ${sortedOffers.size} offers")
                                _offers.postValue(Resource.Success(sortedOffers))
                            } else {
                                Log.e(TAG, "API returned error status: ${offersResponse.status}")
                                _offers.postValue(Resource.Failure(false, "Failed to load offers"))
                            }
                        } else {
                            _offers.postValue(Resource.Failure(false, "Empty response"))
                        }
                    } else {
                        Log.e(TAG, "HTTP error: ${it.code}")
                        _offers.postValue(Resource.Failure(true, "Network error: ${it.code}"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching offers", e)
                val isNetworkError = e.message?.contains("network", ignoreCase = true) == true ||
                                   e.message?.contains("timeout", ignoreCase = true) == true ||
                                   e.message?.contains("connection", ignoreCase = true) == true
                _offers.postValue(Resource.Failure(isNetworkError, "Error: ${e.message}"))
            }
        }
    }

    fun getAmountForCountry(offer: CpaOffer, countryCode: String?): Double {
        return if (countryCode != null && offer.payoutsPerCountry?.containsKey(countryCode) == true) {
            offer.payoutsPerCountry!![countryCode] ?: offer.amount
        } else {
            offer.amount
        }
    }
}
