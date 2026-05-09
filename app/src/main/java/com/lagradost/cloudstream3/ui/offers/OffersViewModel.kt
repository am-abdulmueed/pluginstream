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
import com.lagradost.cloudstream3.ui.offers.model.OfferCreatives
import com.lagradost.cloudstream3.ui.offers.model.OffersResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OffersViewModel : ViewModel() {

    private val _offers = MutableLiveData<Resource<List<CpaOffer>>>()
    val offers: LiveData<Resource<List<CpaOffer>>> = _offers

    private val _debugLogs = MutableLiveData<String>()
    val debugLogs: LiveData<String> = _debugLogs

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    private val logEntries = mutableListOf<String>()
    
    private var cachedOffers: List<CpaOffer>? = null
    private var lastFetchTime: Long = 0L
    private val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes

    companion object {
        private const val TAG = "OffersViewModel"
        private const val GITHUB_CONFIG_URL = "https://cdn.jsdelivr.net/gh/am-abdulmueed/offers@main/offers.json"
        private const val AUTHENTICATEAPP_API_URL = "https://authenticateapp.online/api/v2"
        private const val IPIFY_URL = "https://api.ipify.org/?format=json"
        private const val API_KEY = "43897|vGaDKh19mgaEz7YfSFe1nynTv5gIiez9fF6U4MA05ed58814"
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logEntry = "[$timestamp] $message"
        logEntries.add(logEntry)
        _debugLogs.postValue(logEntries.joinToString("\n"))
        Log.d(TAG, message)
    }

    private suspend fun fetchPublicIP(): String? {
        return withContext(Dispatchers.IO) {
            try {
                addLog("Fetching public IP from $IPIFY_URL")
                val request = Request.Builder()
                    .url(IPIFY_URL)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string() ?: return@use null
                        val json = JSONObject(body)
                        val ip = json.getString("ip")
                        addLog("Got public IP: $ip")
                        ip
                    } else {
                        addLog("Failed to fetch IP: HTTP ${it.code}")
                        null
                    }
                }
            } catch (e: Exception) {
                addLog("Error fetching IP: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchOffersFromExistingAPI(): List<CpaOffer>? {
        return withContext(Dispatchers.IO) {
            try {
                addLog("[CPALead] Fetching offers...")
                val configRequest = Request.Builder()
                    .url(GITHUB_CONFIG_URL)
                    .get()
                    .build()

                val configResponse = client.newCall(configRequest).execute()
                val dynamicUrl = configResponse.use { response ->
                    if (!response.isSuccessful) {
                        addLog("[CPALead] Failed to fetch config: HTTP ${response.code}")
                        return@use null
                    }
                    val body = response.body?.string() ?: return@use null
                    val config = mapper.readValue(body, DynamicOffersConfig::class.java)
                    config.offerUrl
                } ?: return@withContext null

                addLog("[CPALead] Using URL: $dynamicUrl")
                val request = Request.Builder()
                    .url(dynamicUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string() ?: return@use null
                        val offersResponse = mapper.readValue(responseBody, OffersResponse::class.java)
                        if (offersResponse.status == "success") {
                            val sortedOffers = offersResponse.offers
                                .filter { offer -> offer.offerRank != null }
                                .sortedBy { offer -> offer.offerRank }
                            addLog("[CPALead] Success! Got ${sortedOffers.size} offers")
                            sortedOffers
                        } else {
                            addLog("[CPALead] Failed! Error status: ${offersResponse.status}")
                            null
                        }
                    } else {
                        val errorBody = it.body?.string()
                        addLog("[CPALead] Failed! HTTP ${it.code} - ${errorBody ?: "No error body"}")
                        null
                    }
                }
            } catch (e: Exception) {
                addLog("[CPALead] Exception: ${e.message}")
                null
            }
        }
    }

    private suspend fun fetchOffersFromNewAPI(ip: String, userAgent: String): List<CpaOffer>? {
        return withContext(Dispatchers.IO) {
            try {
                addLog("[OGAds] Fetching offers from: $AUTHENTICATEAPP_API_URL")
                
                val encodedIP = java.net.URLEncoder.encode(ip, "UTF-8")
                val encodedUA = java.net.URLEncoder.encode(userAgent, "UTF-8")
                val finalUrl = "$AUTHENTICATEAPP_API_URL?ip=$encodedIP&user_agent=$encodedUA&max=10"
                
                addLog("[OGAds] Request URL: $finalUrl")

                val request = Request.Builder()
                    .url(finalUrl)
                    .addHeader("Authorization", "Bearer $API_KEY")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                response.use {
                    if (it.isSuccessful) {
                        val body = it.body?.string() ?: return@use null
                        addLog("[OGAds] Success! Response: $body")
                        
                        val jsonResponse = JSONObject(body)
                        
                        if (!jsonResponse.optBoolean("success", false)) {
                            addLog("[OGAds] API returned success: false")
                            return@use null
                        }
                        
                        val offersArray = jsonResponse.optJSONArray("offers") ?: return@use null
                        
                        val offers = mutableListOf<CpaOffer>()
                        for (i in 0 until offersArray.length()) {
                            val offerObj = offersArray.getJSONObject(i)
                            
                            val id = offerObj.optInt("offerid", i + 1000)
                            val title = offerObj.optString("name_short", offerObj.optString("name", "Offer"))
                            val description = offerObj.optString("description", "")
                            val link = offerObj.optString("link", "")
                            val payoutStr = offerObj.optString("payout", "0")
                            val payout = try { payoutStr.toDouble() } catch (e: Exception) { 0.0 }
                            val picture = offerObj.optString("picture", "")
                            val countryStr = offerObj.optString("country", "")
                            val countries = if (countryStr.isNotEmpty()) {
                                countryStr.split(",").map { it.trim() }
                            } else {
                                null
                            }
                            val device = offerObj.optString("device", null)
                            
                            val creatives = if (picture.isNotEmpty()) {
                                OfferCreatives(url = picture)
                            } else {
                                null
                            }
                            
                            offers.add(
                                CpaOffer(
                                    id = id,
                                    title = title,
                                    description = description,
                                    conversion = null,
                                    device = device,
                                    dailyCap = null,
                                    isFastPay = null,
                                    link = link,
                                    previewLink = null,
                                    amount = payout,
                                    payoutCurrency = "USD",
                                    payoutType = null,
                                    countries = countries,
                                    epc = null,
                                    creatives = creatives,
                                    offerRank = i,
                                    payoutsPerCountry = null
                                )
                            )
                        }
                        
                        addLog("[OGAds] Got ${offers.size} offers")
                        offers
                    } else {
                        val errorBody = it.body?.string()
                        addLog("[OGAds] Failed! HTTP ${it.code} - ${errorBody ?: "No error body"}")
                        null
                    }
                }
            } catch (e: Exception) {
                addLog("[OGAds] Exception: ${e.message}")
                null
            }
        }
    }

    fun fetchOffers(context: Context, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            
            if (!forceRefresh && cachedOffers != null && (currentTime - lastFetchTime) < CACHE_DURATION_MS) {
                addLog("Using cached offers (${cachedOffers!!.size} offers)")
                _offers.postValue(Resource.Success(cachedOffers!!))
                return@launch
            }

            logEntries.clear()
            addLog("Starting offers fetch...")
            _offers.postValue(Resource.Loading())

            try {
                val userAgent = System.getProperty("http.agent") ?: "Mozilla/5.0"
                addLog("User Agent: $userAgent")
                
                val publicIP = fetchPublicIP()

                val existingOffersDeferred = async { fetchOffersFromExistingAPI() }
                val newOffersDeferred = async { 
                    if (publicIP != null) {
                        fetchOffersFromNewAPI(publicIP, userAgent)
                    } else {
                        addLog("[OGAds] Skipping - no IP available")
                        null
                    }
                }

                val existingOffers = existingOffersDeferred.await()
                val newOffers = newOffersDeferred.await()

                val allOffers = mutableListOf<CpaOffer>()
                existingOffers?.let { allOffers.addAll(it) }
                newOffers?.let { allOffers.addAll(it) }

                val cpaleadCount = existingOffers?.size ?: 0
                val ogadsCount = newOffers?.size ?: 0
                
                if (allOffers.isNotEmpty()) {
                    addLog("Total offers: CPALead($cpaleadCount) + OGAds($ogadsCount) = ${allOffers.size}")
                    cachedOffers = allOffers
                    lastFetchTime = currentTime
                    _offers.postValue(Resource.Success(allOffers))
                } else {
                    addLog("No offers found from CPALead or OGAds")
                    _offers.postValue(Resource.Failure(false, "No offers available"))
                }
            } catch (e: Exception) {
                addLog("Exception: ${e.message}")
                val isNetworkError = e.message?.contains("network", ignoreCase = true) == true ||
                                   e.message?.contains("timeout", ignoreCase = true) == true ||
                                   e.message?.contains("connection", ignoreCase = true) == true
                _offers.postValue(Resource.Failure(isNetworkError, "Error: ${e.message}"))
            }
        }
    }

    fun getDebugLogs(): String {
        return logEntries.joinToString("\n")
    }

    fun getAmountForCountry(offer: CpaOffer, countryCode: String?): Double {
        return if (countryCode != null && offer.payoutsPerCountry?.containsKey(countryCode) == true) {
            offer.payoutsPerCountry!![countryCode] ?: offer.amount
        } else {
            offer.amount
        }
    }
}
