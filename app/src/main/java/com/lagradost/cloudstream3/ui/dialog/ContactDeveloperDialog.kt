package com.lagradost.cloudstream3.ui.dialog

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.lagradost.cloudstream3.BuildConfig
import com.lagradost.cloudstream3.utils.AppDiagnostics

const val EMAIL_ADDRESS = "am.abdulmueed3@gmail.com"

fun sendEmailIntent(context: Context, subject: String, body: String, onFail: () -> Unit) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
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
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = CircleShape
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Get in Touch",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "How can we help you today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ContactOptionItem(
                        title = "Streaming Support",
                        subtitle = "Bugs or provider requests",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            val logs = AppDiagnostics.readLogs(50)
                            val subject = "[PluginStream-Stream] Support/Request"
                            val body = "Issue/Request Type: [Streaming Bug / New Provider Request]\n\nDetails:\n[Describe which link or provider is not working...]\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
                            sendEmailIntent(context, subject, body) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )

                    ContactOptionItem(
                        title = "Games Hub Support",
                        subtitle = "New games or loading issues",
                        icon = Icons.Default.Gamepad,
                        onClick = {
                            val logs = AppDiagnostics.readLogs(50)
                            val subject = "[PluginStream-Games] New Game/Bug Report"
                            val body = "Action: [Add New Game / Game Not Loading]\n\nGame Name:\nGenre:\nAdditional Info: [Provide link or description here]\n\nNote: Thanks for making the game hub better!\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
                            sendEmailIntent(context, subject, body) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )

                    ContactOptionItem(
                        title = "ProTube Support",
                        subtitle = "Feedback or video issues",
                        icon = Icons.Default.Tv,
                        onClick = {
                            val logs = AppDiagnostics.readLogs(50)
                            val subject = "[PluginStream-ProTube] Feedback/Issue"
                            val body = "Feedback Type: [Video Quality / Search Issue / Feature Request]\n\nDescribe the Problem:\n[Describe what can be improved in ProTube...]\n\n${AppDiagnostics.getDeviceInfo()}\n\n--- Recent Logs ---\n$logs"
                            sendEmailIntent(context, subject, body) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )

                    ContactOptionItem(
                        title = "General Bug Report",
                        subtitle = "Report technical problems",
                        icon = Icons.Default.BugReport,
                        isHighlight = true,
                        onClick = {
                            val logs = AppDiagnostics.readLogs(100)
                            val subject = "⚠️ Bug Report - PluginStream v${BuildConfig.VERSION_NAME}"
                            val body = "${AppDiagnostics.getDeviceInfo()}\n---\nProblem Description:\n[Describe the problem you are facing...]\n---\nLatest Logs:\n$logs\n\nPlease fix this as soon as possible. Thanks!"
                            sendEmailIntent(context, subject, body) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ContactOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isHighlight) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val contentColor = if (isHighlight) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isHighlight) 0.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
