# Ad Dialog Configuration Guide

## Overview

This document is a complete guide for the app’s ad dialog system. In this system, you can fetch JSON config from GitHub and display dynamic ads.

## File Locations

| File          | Path                                                                | Purpose                                       |
| ------------- | ------------------------------------------------------------------- | --------------------------------------------- |
| Main Activity | `app/src/main/java/com/lagradost/cloudstream3/MainActivity.kt:1509` | `showDialogAd()` function - complete ad logic |
| Dialog Layout | `app/src/main/res/layout/dialog_ad.xml`                             | Ad dialog UI layout                           |
| JSON Config   | `https://cdn.jsdelivr.net/gh/am-abdulmueed/ads-json@main/ads.json`  | Online config file                            |

## JSON Configuration

### Complete JSON Structure

```json
{
  "dialog_ad": {
    "enabled": true,
    "badge": "Hot Offer",
    "title": "🔥 Special Limited Time Offer!",
    "message": "## 🎁 Exclusive Benefits\n\n- **Premium Access** to all features\n- **Ad-Free Experience**\n\n*Don't miss out!*",
    "button_text": "Get Premium Now",
    "images": [
      "https://i.ibb.co/kgt1Sj4Y/600x600.webp",
      "https://i.ibb.co/abc123/example1.webp"
    ],
    "click_url": "https://members.ogads.com/register?r=401055",
    "show_after_seconds": 3,
    "auto_close_seconds": 20,
    "start_date": "2026-05-01",
    "end_date": "2026-12-31",
    "max_daily_views": 3,
    "interval_hours": 7,
    "show_indicator": true
  }
}
```

### Field Descriptions

| Field                | Type    | Required | Description                                                                    |
| -------------------- | ------- | -------- | ------------------------------------------------------------------------------ |
| `enabled`            | Boolean | Yes      | Enable/disable the ad                                                          |
| `badge`              | String  | No       | Badge shown in the top-left corner (e.g., "Sponsored", "Hot Offer")            |
| `title`              | String  | Yes      | Ad title                                                                       |
| `message`            | String  | Yes      | Ad description (Supports Markdown)                                             |
| `button_text`        | String  | Yes      | Text displayed on the button                                                   |
| `images`             | Array   | Yes      | List of image URLs for the swipeable carousel. Falls back to `image_url` if empty |
| `image_url`          | String  | No       | Single image URL (legacy support)                                              |
| `show_indicator`     | Boolean | No       | Show/hide the dots indicator for multiple images (Default: true)               |
| `click_url`          | String  | Yes      | URL opened when the button is clicked                                          |
| `show_after_seconds` | Integer | Yes      | How many seconds after app launch the ad will show                             |
| `auto_close_seconds` | Integer | Yes      | After how many seconds the countdown ends and the close button appears         |
| `start_date`         | String  | Yes      | When the ad starts showing (Format: YYYY-MM-DD)                                |
| `end_date`           | String  | Yes      | When the ad stops showing (Format: YYYY-MM-DD)                                 |
| `max_daily_views`    | Integer/String | Yes      | Maximum number of times the ad can show per day. Use `"no limit"` for unlimited impressions. |
| `interval_hours`     | Integer | Yes      | Minimum gap in hours between two ad displays                                   |

## Dialog UI Flow

1. **Dialog Appears**
   - Countdown timer shows in the top-right corner
   - Badge (if provided) shows in the top-left corner
   - Order: Badge → Title → Image Carousel → Dots Indicator → Description → Button
   - **Responsive Design:** The dialog is fully responsive and scrollable (`NestedScrollView`). It won't exceed 85% of the screen height, ensuring it never gets cut off.
2. **Multi-Image Carousel**
   - If multiple `images` are provided, they become swipeable.
   - **Dots Indicator:** Shows current position if `show_indicator` is true.
   - **Legacy Support:** Still works if only a single `image_url` is provided.
3. **Markdown Support**
   - The `message` field supports full **Markdown** rendering.
   - **Headers:** `# Header 1`, `## Header 2`, etc.
   - **Lists:** Bullet points (`-`) and Numbered lists (`1.`).
   - **Styling:** **Bold**, *Italic*, `Inline Code`, and Code Blocks.
   - **Interactive Links:** All markdown links (e.g., `[Google](https://google.com)`) are **clickable** and will open in the device's browser.
   - **Alignment:** All text (Title & Message) is **start-aligned** (left) to ensure lists and bullets look professional.
3. **Interactive Carousel**
   - Every image in the carousel is **clickable**.
   - Clicking an image opens that specific image in a **Fullscreen View**.
   - **Smooth Zoom:** In fullscreen view, you can **Pinch-to-Zoom** the current image.
   - A close button (X) is provided to return to the ad dialog.
4. **After Countdown Ends**
   - Timer disappears
   - Close (X) icon appears
   - User can dismiss the dialog by clicking the close icon

## Shared Preferences Keys

These keys are used to store data:

| Key                        | Purpose                                      |
| -------------------------- | -------------------------------------------- |
| `LAST_DIALOG_AD_SHOW_TIME` | Last time the ad was shown (milliseconds)    |
| `DIALOG_AD_VIEWS_TODAY`    | Number of times the ad has shown today       |
| `DIALOG_AD_LAST_VIEW_DATE` | Last date the ad was shown                   |

## Automatic Badge Color System

Badge color is automatically set based on the text with **Premium Gradients**!

| Badge Text     | Background Gradient | Text Color | Meaning                 |
| -------------- | ------------------- | ---------- | ----------------------- |
| `Sponsored`    | Blue Gradient       | White      | Trust & Professionalism |
| `Promotion`    | Green Gradient      | White      | Growth & Deals          |
| `Hot Offer`    | Orange Gradient     | White      | Urgency & Limited Time  |
| `Hot`          | Orange Gradient     | White      | Urgency & Limited Time  |
| `Exclusive`    | Purple Gradient     | White      | Premium & Luxury        |
| `Premium`      | Gold Gradient       | Black      | High-Value Offers       |
| `VIP`          | Gold Gradient       | Black      | High-Value Offers       |
| Any other text | Red Gradient        | White      | Default fallback        |

### Premium UI Features

- **Dark Theme:** Dialog uses a premium dark background (`#1A1A1A`).
- **Rounded Corners:** 24dp corner radius for a modern look.
- **Glassmorphism Border:** Subtle white border for depth.
- **Full Width Button:** Call-to-action button is now more prominent.

### Example JSON Configs

**Sponsored Ad:**
```json
{
  "badge": "Sponsored",
  "title": "Special Offer!"
}
```

**Hot Offer:**
```json
{
  "badge": "Hot Offer",
  "title": "Limited Time Deal!"
}
```

**Promotion:**
```json
{
  "badge": "Promotion",
  "title": "Special Promotion!"
}
```

**Exclusive Deal:**
```json
{
  "badge": "Exclusive",
  "title": "Exclusive Offer!"
}
```

## Customization Tips

### Available Badge Drawables

If you want to manually change badge backgrounds, these drawables are available:

| Drawable File         | Color  |
| --------------------- | ------ |
| `badge_bg_blue.xml`   | Blue   |
| `badge_bg_gray.xml`   | Gray   |
| `badge_bg_green.xml`  | Green  |
| `badge_bg_orange.xml` | Orange |
| `badge_bg_red.xml`    | Red    |
| `badge_bg_purple.xml` | Purple |
| `badge_bg_gold.xml`   | Gold   |

### Change Image Radius

Now we use **ShapeableImageView** (from Material Components) which rounds the image without cropping. To change the radius, edit the `RoundedImage` style in `app/src/main/res/values/styles.xml`:

```xml
<style name="RoundedImage">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">24dp</item>  <!-- Change this value -->
</style>
```

**Note:** The benefit of ShapeableImageView is that the image doesn't get cropped - only the corners are rounded! The full image remains visible.

### Change Button Style

The app already has a `WhiteButton` style defined. It’s already applied in the layout.

## Example Badge Ideas

| Badge Text  | Color            | Use Case             |
| ----------- | ---------------- | -------------------- |
| `Sponsored` | Blue (#1E88E5)   | Normal sponsored ads |
| `Hot Offer` | Orange (#FB8C00) | Limited time offers  |
| `Promotion` | Green (#43A047)  | Special promotions   |
| `Exclusive` | Purple (#8E24AA) | Exclusive deals      |
| `Premium`   | Gold (#FFD700)   | Premium offers       |

## Important Notes

1. **Required JSON Fields**: `title`, `message`, `button_text`, `click_url`, and at least one image (`images` array or `image_url`) must not be empty.
2. **Carousel Behavior**: If multiple images are provided, the share button and fullscreen preview will always target the **currently visible image** in the carousel.
3. **Date Format**: Always use `YYYY-MM-DD` for `start_date` and `end_date`  
4. **Time Calculation**: All time calculations are based on the local device time  
5. **No Auto-Dismiss**: After countdown ends, the dialog won’t auto-dismiss. Only the close button will appear  
6. **Improved Share Functionality**: Shares the **current carousel image** and formatted text together.
6. **Secure Image Sharing**: Uses `FileProvider` to safely share the ad image with other apps.
7. **Cross-App Compatibility**: Optimized to work perfectly with WhatsApp, Telegram, and other social platforms.
8. **Markdown Support**: Shared text includes professional formatting with bold titles and emojis.
9. **Fallback Mechanism**: If the image isn't loaded, it gracefully falls back to sharing text only.  

## Markdown Example File

For a complete example with markdown formatting, see: **[custom_ads_markdown_example.json](custom_ads_markdown_example.json)**

This example shows:
- Rich text formatting with headers
- Bold and italic text
- Lists and bullet points
- Strikethrough pricing
- Emoji integration
- Professional markdown layout

---
**Licensed under the MIT License © 2026 Abdul Mueed**
