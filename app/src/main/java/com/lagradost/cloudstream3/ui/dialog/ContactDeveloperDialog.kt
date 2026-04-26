package com.lagradost.cloudstream3.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.lagradost.cloudstream3.BuildConfig

const val EMAIL_ADDRESS = "am.abdulmueed3@gmail.com"

fun sendEmailIntent(context: Context, subject: String, body: String, onFail: () -> Unit) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:") // Sirf email apps open hongi
        putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        onFail()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDeveloperDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    scope: CoroutineScope
) {
    val context = LocalContext.current

    if (showDialog) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
            dragHandle = {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Get in Touch", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("Quick actions to reach us", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Streaming Support
                ElevatedButton(
                    onClick = {
                        val subject = "[PluginStream-Stream] Support/Request"
                        val body = """
                            Issue/Request Type: [Streaming Bug / New Provider Request]

                            Details:
                            [Describe which link or provider is not working...]

                            Device Info: ${Build.MODEL}, Android ${Build.VERSION.RELEASE}
                        """.trimIndent()
                        sendEmailIntent(context, subject, body) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Streaming Support", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Games Hub Support
                ElevatedButton(
                    onClick = {
                        val subject = "[PluginStream-Games] New Game/Bug Report"
                        val body = """
                            Action: [Add New Game / Game Not Loading]

                            Game Name:
                            Genre:
                            Additional Info: [Provide link or description here]

                            Note: Thanks for making the game hub better!
                        """.trimIndent()
                        sendEmailIntent(context, subject, body) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Gamepad, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Games Hub Support", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // ProTube Support
                ElevatedButton(
                    onClick = {
                        val subject = "[PluginStream-ProTube] Feedback/Issue"
                        val body = """
                            Feedback Type: [Video Quality / Search Issue / Feature Request]

                            Describe the Problem:
                            [Describe what can be improved in ProTube...]

                            System Logs: (Auto-filled context)
                        """.trimIndent()
                        sendEmailIntent(context, subject, body) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tv, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ProTube Support", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // General Bug Report
                FilledTonalButton(
                    onClick = {
                        val subject = "⚠️ Bug Report - PluginStream v${BuildConfig.VERSION_NAME}"
                        val body = """
                            --- Device Information (Auto-Generated) ---
                            Device: ${Build.MANUFACTURER}
                            Model: ${Build.MODEL}
                            Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
                            App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
                            ---
                            Problem Description:
                            [Describe the problem you are facing...]
                            ---
                            Latest Logs:
                            [Last few app activities or errors will appear here automatically]

                            Please fix this as soon as possible. Thanks!
                        """.trimIndent()
                        sendEmailIntent(context, subject, body) {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("General Bug Report", fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Open Email App (Generic)
                OutlinedButton(
                    onClick = {
                        sendEmailIntent(context, "", "") {
                            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                        }
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open Email App", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
