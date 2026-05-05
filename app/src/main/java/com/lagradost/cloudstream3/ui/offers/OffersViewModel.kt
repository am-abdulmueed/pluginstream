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
import com.lagradost.cloudstream3.ui.offers.model.DynamicOffersConfig
import com.lagradost.cloudstream3.ui.offers.model.OffersResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import org.json.JSONObject

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
        private const val GITHUB_CONFIG_URL = "https://cdn.jsdelivr.net/gh/am-abdulmueed/offers@main/offers.json"
        private const val CONFIG_FILE = "config.json"
        
        // OGADS API Constants
        private const val OGADS_ENDPOINT = "https://authenticateapp.store/api/v2"
        private const val OGADS_API_KEY = "43864|9grBUuVSGId3VU938pcNViQobbwj5VFJtOSc89zFbf8d191f"
    }
    
    private fun loadConfig(context: Context): JSONObject? {
        return try {
            val configString = context.assets.open(CONFIG_FILE).bufferedReader().use { it.readText() }
            JSONObject(configString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config.json", e)
            null
        }
    }

    fun fetchOffers(context: Context) {
        viewModelScope.launch {
            _offers.postValue(Resource.Loading())

            try {
                val allOffers = mutableListOf<CpaOffer>()
                
                // 1. Try CPALEAD (GitHub config)
                try {
                    val cpaleadOffers = fetchCpaLeadOffers()
                    if (cpaleadOffers.isNotEmpty()) {
                        allOffers.addAll(cpaleadOffers)
                        Log.d(TAG, "CPALEAD: Fetched ${cpaleadOffers.size} offers")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "CPALEAD failed, trying OGADS", e)
                }

                // 2. Try OGADS (if CPALEAD failed or for additional offers)
                try {
                    val ogadsOffers = fetchOgadsOffers(context)
                    if (ogadsOffers.isNotEmpty()) {
                        allOffers.addAll(ogadsOffers)
                        Log.d(TAG, "OGADS: Fetched ${ogadsOffers.size} offers")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "OGADS failed", e)
                }

                // 3. If no offers from any network
                if (allOffers.isEmpty()) {
                    _offers.postValue(Resource.Failure(false, "No offers available"))
                    return@launch
                }

                // 4. Sort all offers by rank and remove duplicates
                val sortedOffers = allOffers
                    .filter { offer -> offer.offerRank != null }
                    .sortedBy { offer -> offer.offerRank }
                    .distinctBy { offer -> offer.offerUrl } // Remove duplicate URLs

                Log.d(TAG, "Total merged offers: ${sortedOffers.size}")
                _offers.postValue(Resource.Success(sortedOffers))

            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching offers", e)
                val isNetworkError = e.message?.contains("network", ignoreCase = true) == true ||
                                   e.message?.contains("timeout", ignoreCase = true) == true ||
                                   e.message?.contains("connection", ignoreCase = true) == true
                _offers.postValue(Resource.Failure(isNetworkError, "Error: ${e.message}"))
            }
        }
    }

    private suspend fun fetchCpaLeadOffers(): List<CpaOffer> {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "Fetching CPALEAD config from: $GITHUB_CONFIG_URL")
            val configRequest = Request.Builder()
                .url(GITHUB_CONFIG_URL)
                .get()
                .build()

            val configResponse = client.newCall(configRequest).execute()
            val dynamicUrl = configResponse.use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Failed to fetch CPALEAD config: ${response.code}")
                }
                val body = response.body?.string() ?: throw Exception("Empty config response")
                val config = mapper.readValue(body, DynamicOffersConfig::class.java)
                config.offerUrl
            }

            Log.d(TAG, "CPALEAD dynamic URL: $dynamicUrl")

            val request = Request.Builder()
                .url(dynamicUrl)
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body?.string()
                    if (responseBody != null) {
                        val offersResponse = mapper.readValue(responseBody, OffersResponse::class.java)
                        if (offersResponse.status == "success") {
                            return@withContext offersResponse.offers
                        } else {
                            throw Exception("CPALEAD API error: ${offersResponse.status}")
                        }
                    }
                }
                throw Exception("CPALEAD HTTP error: ${it.code}")
            }
        }
    }

    private suspend fun fetchOgadsOffers(context: Context): List<CpaOffer> {
        return withContext(Dispatchers.IO) {
            // Load config for API key
            val config = loadConfig(context)
            val apiKey = config?.optJSONObject("api_sources")?.optJSONObject("ogads")?.optString("api_key") 
                ?: OGADS_API_KEY
            val endpoint = config?.optJSONObject("api_sources")?.optJSONObject("ogads")?.optString("endpoint") 
                ?: OGADS_ENDPOINT

            Log.d(TAG, "Fetching OGADS from: $endpoint")

            val request = Request.Builder()
                .url(endpoint)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use {
                if (it.isSuccessful) {
                    val responseBody = it.body?.string()
                    if (responseBody != null) {
                        // Parse OGADS response and convert to CpaOffer format
                        return@withContext parseOgadsResponse(responseBody)
                    }
                }
                throw Exception("OGADS HTTP error: ${it.code}")
            }
        }
    }

    private fun parseOgadsResponse(responseBody: String): List<CpaOffer> {
        return try {
            val jsonObject = JSONObject(responseBody)
            val offersArray = jsonObject.optJSONArray("offers") ?: return emptyList()
            
            val offers = mutableListOf<CpaOffer>()
            for (i in 0 until offersArray.length()) {
                val offerObj = offersArray.getJSONObject(i)
                val cpaOffer = CpaOffer(
                    offerUrl = offerObj.optString("offer_url", ""),
                    offerName = offerObj.optString("offer_name", "Unknown Offer"),
                    offerDescription = offerObj.optString("description", ""),
                    amount = offerObj.optDouble("payout", 0.0),
                    offerRank = offerObj.optInt("rank", 999),
                    imageUrl = offerObj.optString("image_url", ""),
                    category = offerObj.optString("category", "general"),
                    countries = listOf("*"), // OGADS might not provide this
                    payoutsPerCountry = null
                )
                if (cpaOffer.offerUrl.isNotEmpty()) {
                    offers.add(cpaOffer)
                }
            }
            offers
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing OGADS response", e)
            emptyList()
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
