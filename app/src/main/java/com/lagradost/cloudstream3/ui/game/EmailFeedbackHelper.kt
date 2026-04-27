package com.lagradost.cloudstream3.ui.game

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

object EmailFeedbackHelper {
    
    private const val EMAIL_ADDRESS = "am.abdulmueed3@gmail.com"
    
    /**
     * Show dialog to choose between Feature Request or Bug Report
     */
    fun showFeedbackDialog(context: Context) {
        val options = arrayOf("📝 Request Feature / Suggestion", "🐛 Report a Bug / Issue")
        
        android.app.AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Get in Touch")
            .setMessage("What would you like to do?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openFeatureRequestEmail(context)
                    1 -> openBugReportEmail(context)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Open email app with Feature Request template
     */
    private fun openFeatureRequestEmail(context: Context) {
        val subject = "PluginStream - Feature Request & Suggestion"
        val body = """
            New Feature Suggestion:
            [Type your feature idea here]
            
            Request New Game:
            - Game Name: 
            - Category: 
            - Extra Information: 
            
            ---
            Thank you for improving PluginStream!
        """.trimIndent()
        
        sendEmail(context, subject, body)
        Toast.makeText(context, "Opening Email App...", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Open email app with Bug Report template + Device Info
     */
    private fun openBugReportEmail(context: Context) {
        val deviceInfo = getDeviceInfo(context)
        
        val subject = "PluginStream - Bug Report"
        val body = """
            Issue Description:
            [Describe the bug/issue you're facing]
            
            Steps to Reproduce:
            1. 
            2. 
            3. 
            
            Expected Behavior:
            [What should have happened]
            
            Actual Behavior:
            [What actually happened]
            
            --- Device Info ---
            $deviceInfo
            
            ---
            Thank you for reporting!
        """.trimIndent()
        
        sendEmail(context, subject, body)
        Toast.makeText(context, "Opening Email App...", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Send email intent
     */
    private fun sendEmail(context: Context, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$EMAIL_ADDRESS")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found on device", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Get device information
     */
    private fun getDeviceInfo(context: Context): String {
        val packageName = context.packageName
        val packageInfo = try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
            null
        }
        
        val appVersion = packageInfo?.versionName ?: "Unknown"
        val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode?.toString() ?: "Unknown"
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toString() ?: "Unknown"
        }
        
        val deviceModel = Build.MODEL ?: "Unknown"
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val sdkVersion = Build.VERSION.SDK_INT.toString()
        
        val screenDensity = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels
        val screenHeight = context.resources.displayMetrics.heightPixels
        
        val language = java.util.Locale.getDefault().language
        val timezone = java.util.TimeZone.getDefault().id
        
        return """
            - App Version: $appVersion ($appVersionCode)
            - Device: $manufacturer $deviceModel
            - Android: $androidVersion (API $sdkVersion)
            - Screen: ${screenWidth}x$screenHeight (${screenDensity}x)
            - Language: $language
            - Timezone: $timezone
        """.trimIndent()
    }
}
