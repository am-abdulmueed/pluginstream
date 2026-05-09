# Offer Tab Configuration Guide

## Overview

This document is a complete guide for the app's offer tab system. In this system, you can fetch CPA (Cost Per Action) offers from multiple APIs and display them to users for completion and earning rewards.

## File Locations

| File            | Path                                                                            | Purpose                         |
| --------------- | ------------------------------------------------------------------------------- | ------------------------------- |
| Main Fragment   | `app/src/main/java/com/lagradost/cloudstream3/ui/offers/OffersFragment.kt`      | Main offers list UI and logic   |
| Detail Fragment | `app/src/main/java/com/lagradost/cloudstream3/ui/offers/OfferDetailFragment.kt` | Individual offer detail view    |
| ViewModel       | `app/src/main/java/com/lagradost/cloudstream3/ui/offers/OffersViewModel.kt`     | API calls and data management   |
| Adapter         | `app/src/main/java/com/lagradost/cloudstream3/ui/offers/OffersAdapter.kt`       | RecyclerView adapter for offers |
| Data Models     | `app/src/main/java/com/lagradost/cloudstream3/ui/offers/model/CpaOffer.kt`      | Offer data classes              |
| Main Layout     | `app/src/main/res/layout/fragment_offers.xml`                                   | Main offers screen layout       |
| Detail Layout   | `app/src/main/res/layout/fragment_offer_detail.xml`                             | Offer detail screen layout      |
| Item Layout     | `app/src/main/res/layout/item_offer.xml`                                        | Individual offer item layout    |
| GitHub Config   | `https://cdn.jsdelivr.net/gh/am-abdulmueed/offers@main/offers.json`             | Dynamic API configuration       |

## API Integration

### Multiple API Sources

The offer tab fetches offers from two different APIs:

1. **CPALead API** - Primary offer source
   - Dynamic URL fetched from GitHub config
   - Requires no authentication
   - Returns ranked offers
2. **OGAds API** - Secondary offer source
   - Fixed endpoint: `https://authenticateapp.online/api/v2`
   - Requires API key authentication
   - Uses IP-based targeting

### API Configuration

#### GitHub Config Structure

```json
{
  "offer": "https://cpalead.com/dashboard/reports/campaigns/list.json?api_key=YOUR_API_KEY"
}
```

#### OGAds API Parameters

- **Endpoint**: `https://authenticateapp.online/api/v2`
- **Method**: GET
- **Headers**: `Authorization: Bearer 43897|vGaDKh19mgaEz7YfSFe1nynTv5gIiez9fF6U4MA05ed58814`
- **Query Parameters**:
  - `ip`: User's public IP address
  - `user_agent`: Device user agent
  - `max`: Maximum offers to return (default: 10)

## Data Models

### CpaOffer Model

```kotlin
data class CpaOffer(
    val id: Int,                          // Unique offer identifier
    val title: String,                    // Offer title/name
    val description: String?,              // Offer description
    val conversion: String?,               // Conversion requirements
    val device: String?,                  // Supported device type
    val dailyCap: Int?,                   // Daily conversion limit
    val isFastPay: Boolean?,              // Fast payment eligibility
    val link: String,                     // Offer completion link
    val previewLink: String?,             // Preview link
    val amount: Double,                   // Payout amount
    val payoutCurrency: String?,          // Currency code (USD, EUR, etc.)
    val payoutType: String?,              // Payout type (CPI, CPE, CPR)
    val countries: List<String>?,          // Supported countries
    val epc: Double?,                     // Earnings per click
    val creatives: OfferCreatives?,       // Image assets
    val offerRank: Int?,                  // Display ranking
    val payoutsPerCountry: Map<String, Double>?  // Country-specific payouts
)
```

### OfferCreatives Model

```kotlin
data class OfferCreatives(
    val url: String?                      // Image URL for offer
)
```

## UI Components

### Main Offers Screen (`OffersFragment`)

**Features:**

- RecyclerView with offer cards
- Swipe-to-refresh functionality
- Loading, empty, and offline states
- **Debug Panel:** Live logs for troubleshooting.
    - **Visibility:** The debug toggle icon is only visible in **Debug builds**. It is automatically hidden in **Release builds** for security and a cleaner UI.
    - **Note:** Debug logs icon will only be enabled in debug mode, not in release.
- Automatic caching (30 minutes)

**Layout Structure:**

1. **Debug Panel** (toggleable)
   - Live API logs
   - Copy to clipboard functionality
2. **Loading State**
   - Progress indicator
3. **Empty State**
   - Gift icon with "No offers available" message
4. **Offers List**
   - RecyclerView with offer cards
   - SwipeRefreshLayout for refresh
5. **Offline Screen**
   - Beautiful offline UI with shimmer effect
   - Retry button with loading animation

### Offer Detail Screen (`OfferDetailFragment`)

**Features:**

- Offer image display
- Detailed offer information
- Country and device compatibility
- Share functionality
- Install button (opens offer link)

**Information Displayed:**

- Offer title and image
- Payout amount and currency
- Supported countries with flags
- Device compatibility with icons
- Conversion requirements
- Description

### Offer Card Item (`OffersAdapter`)

**Card Contents:**

- Offer image (from creatives.url)
- Offer title
- Payout amount (formatted to 2 decimals)
- Currency symbol
- Payout type (CPI→Install, CPE→Action, CPR→Registration)

## API Flow

### 1. Initial Load

```
OffersFragment.onCreate() → 
ViewModel.fetchOffers() → 
Check cache → 
If fresh: Return cached data
If expired: Fetch from APIs
```

### 2. API Fetching Process

```
fetchOffers() → 
Parallel execution:
├── fetchPublicIP() → fetchOffersFromNewAPI() (OGAds)
└── fetchOffersFromExistingAPI() (CPALead)
→ Combine results → Cache → Update UI
```

### 3. CPALead API Flow

```
GitHub Config → Get Dynamic URL → 
API Request → Parse JSON → 
Filter by offerRank → Sort by rank → 
Return offers
```

### 4. OGAds API Flow

```
Get Public IP → Encode parameters → 
API Request with Auth → Parse JSON → 
Map to CpaOffer model → Return offers
```

## Caching System

### Cache Configuration

- **Duration**: 30 minutes
- **Storage**: In-memory (ViewModel)
- **Cache Key**: Last fetch time + offers list

### Cache Logic

```kotlin
if (!forceRefresh && cachedOffers != null && 
    (currentTime - lastFetchTime) < CACHE_DURATION_MS) {
    // Return cached offers
    return cachedOffers
}
```

## Debug Features

### Debug Panel

- **Toggle Button**: Floating action button (bug icon)
- **Live Logs**: Real-time API call logs
- **Copy Function**: Copy all logs to clipboard
- **Timestamp**: Each log entry has timestamp

### Log Categories

- `[CPALead]` - CPALead API operations
- `[OGAds]` - OGAds API operations
- General operations (cache, IP fetch, etc.)

### Sample Log Output

```
[14:30:15] Starting offers fetch...
[14:30:15] User Agent: Mozilla/5.0 (Linux; Android 10; SM-G973F)
[14:30:16] Fetching public IP from https://api.ipify.org/?format=json
[14:30:16] Got public IP: 192.168.1.1
[14:30:16] [CPALead] Fetching offers...
[14:30:17] [CPALead] Using URL: https://cpalead.com/...
[14:30:18] [CPALead] Success! Got 15 offers
[14:30:18] [OGAds] Fetching offers from: https://authenticateapp.online/api/v2
[14:30:19] [OGAds] Success! Got 8 offers
[14:30:19] Total offers: CPALead(15) + OGAds(8) = 23
```

## Error Handling

### Network Errors

- **Detection**: Checks for "network", "timeout", "connection" in error messages
- **UI Response**: Shows offline screen with retry option
- **User Feedback**: Offline screen with shimmer effect

### API Errors

- **CPALead**: Checks response.status == "success"
- **OGAds**: Checks response.success == true
- **Fallback**: If one API fails, continues with the other

### Empty States

- **No Offers**: Shows gift icon with message
- **Network Error**: Shows offline screen
- **Loading**: Shows progress indicator

## Customization

### Payout Type Mapping

```kotlin
when (offer.payoutType?.uppercase()) {
    "CPI" -> "Install"      // Cost Per Install
    "CPE" -> "Action"       // Cost Per Engagement  
    "CPR" -> "Registration" // Cost Per Registration
    else -> offer.payoutType ?: "Offer"
}
```

### Device Icons

- **Android**: `ic_android`
- **iOS**: `ic_ios`
- **Desktop**: `ic_desktop`
- **Mobile**: `ic_mobile`
- **Default**: `ic_device`

### Country Support

- **Flag Generation**: Unicode flag emojis from country codes
- **Country Names**: Mapping for common countries
- **Supported Countries**: US, CA, GB, AU, NZ, DE, FR, IN, BR, MX

## Share Functionality

### Share Message Format

```
🎁 *Offer Title*

🤖 *Supported Device:* Android
🇺🇸 *Available in:* United States

📝 *Description:*
Complete this offer to earn reward.

🔗 *Get this offer:*
https://offer-link.com

──────────────
📲 Download from PluginStream Max
🌐 https://pluginstream.pages.dev
```

### Share Implementation

- **Intent**: ACTION\_SEND with text/plain
- **Subject**: "Check out this offer: \[Title]"
- **Chooser**: "Share Offer via"

## Performance Optimizations

### Parallel API Calls

- Uses `async` for parallel execution
- Combines results from both APIs
- Reduces total fetch time

### Image Loading

- Uses ImageLoader utility
- Placeholder image for missing creatives
- Efficient memory management

### RecyclerView Optimization

- DiffUtil for efficient updates
- ViewHolder pattern
- Proper item recycling

## Security Considerations

### API Keys

- OGAds API key stored in code
- Consider moving to secure storage
- CPALead uses dynamic URL from GitHub

### URL Validation

- All offer links opened with ACTION\_VIEW
- Proper URL parsing in Intent
- Exception handling for malformed URLs

## Important Notes

1. **Cache Duration**: Offers are cached for 30 minutes to reduce API calls
2. **IP Detection**: Public IP is fetched for OGAds targeting
3. **Ranking**: CPALead offers are sorted by offerRank
4. **Device Targeting**: Some offers are device-specific
5. **Country Restrictions**: Offers may be geo-restricted
6. **Debug Mode**: Can be toggled via FAB button
7. **Offline Support**: Graceful handling of network issues

## API Response Examples

### CPALead Response

```json
{
  "status": "success",
  "number_offers": 15,
  "country": "US",
  "devices": "android",
  "offers": [
    {
      "id": 1234,
      "title": "Game App Install",
      "description": "Install and play this game",
      "amount": 1.50,
      "payout_currency": "USD",
      "payout_type": "CPI",
      "offer_rank": 1,
      "countries": ["US", "CA"],
      "device": "android",
      "creatives": {
        "url": "https://example.com/image.jpg"
      }
    }
  ]
}
```

### OGAds Response

```json
{
  "success": true,
  "offers": [
    {
      "offerid": 5678,
      "name_short": "Survey App",
      "name": "Complete Survey App",
      "description": "Complete surveys to earn",
      "payout": "2.00",
      "link": "https://offer-link.com",
      "picture": "https://image-url.com",
      "country": "US,CA,GB",
      "device": "mobile"
    }
  ]
}
```

***

**Licensed under the MIT License © 2026 Abdul Mueed**
