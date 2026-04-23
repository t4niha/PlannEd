package com.planned

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun VoiceMicButton(
    db: AppDatabase,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var phase             by remember { mutableStateOf(VoicePhase.IDLE) }
    var lastResult        by remember { mutableStateOf<VoiceResult?>(null) }
    var pendingAction     by remember { mutableStateOf<VoicePendingAction?>(null) }
    var showResultDialog  by remember { mutableStateOf(false) }
    var showPendingDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        VoiceCommandManager.initTts(context)
        VoiceCommandManager.onPhaseChange = { newPhase -> phase = newPhase }
        VoiceCommandManager.onResult = { result ->
            lastResult = result
            showResultDialog = true
        }
        VoiceCommandManager.onPendingAction = { action ->
            pendingAction = action
            showPendingDialog = true
        }
        onDispose { VoiceCommandManager.releaseTts() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) VoiceCommandManager.startListening(context, db)
        else phase = VoicePhase.ERROR
    }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.30f,
        animationSpec = infiniteRepeatable(animation = tween(550, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )

    val buttonColor = when (phase) {
        VoicePhase.IDLE      -> Color(0xFF9E9E9E)
        VoicePhase.LISTENING -> PrimaryColor
        VoicePhase.THINKING  -> Color(0xFF9E9E9E)
        VoicePhase.SPEAKING  -> Color(0xFF9E9E9E)
        VoicePhase.ERROR     -> Color.LightGray
    }

    val isActive     = phase != VoicePhase.IDLE && phase != VoicePhase.ERROR
    val appliedScale = if (phase == VoicePhase.LISTENING) pulseScale else 1f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isActive) {
            Box(modifier = Modifier.size(52.dp).scale(appliedScale).clip(CircleShape).background(buttonColor.copy(alpha = 0.18f)))
        }
        Box(
            modifier = Modifier
                .size(40.dp).clip(CircleShape).background(buttonColor)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = ripple(bounded = true)) {
                    when (phase) {
                        VoicePhase.IDLE, VoicePhase.ERROR -> {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) VoiceCommandManager.startListening(context, db)
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        VoicePhase.LISTENING -> {
                            VoiceCommandManager.stopListening()
                            phase = VoicePhase.THINKING
                        }
                        VoicePhase.SPEAKING  -> VoiceCommandManager.cancelSpeech()
                        VoicePhase.THINKING  -> { /* Can't interrupt */ }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (phase == VoicePhase.ERROR) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = when (phase) {
                    VoicePhase.IDLE      -> "Start voice command"
                    VoicePhase.LISTENING -> "Stop recording"
                    VoicePhase.THINKING  -> "Thinking…"
                    VoicePhase.SPEAKING  -> "Stop speaking"
                    VoicePhase.ERROR     -> "Voice unavailable"
                },
                tint = Color.White, modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showPendingDialog) {
        pendingAction?.let { action ->
            VoiceConfirmationDialog(
                action    = action,
                onConfirm = {
                    showPendingDialog = false
                    pendingAction = null
                    scope.launch {
                        action.execute()
                        lastResult = VoiceResult(userText = "", replyText = action.replyText, actionTaken = actionLabel(action))
                        showResultDialog = true
                        VoiceCommandManager.speakOut(action.replyText)
                    }
                },
                onCancel = {
                    showPendingDialog = false
                    pendingAction = null
                }
            )
        }
    }

    if (showResultDialog) {
        lastResult?.let { result ->
            VoiceResultDialog(result = result, onDismiss = { showResultDialog = false })
        }
    }
}

private fun actionLabel(action: VoicePendingAction): String {
    val prefix = when {
        action.actionType.startsWith("CREATE") -> "Created"
        action.actionType.startsWith("EDIT")   -> "Updated"
        action.actionType.startsWith("DELETE") -> "Deleted"
        action.actionType == "CHANGE_SETTING"  -> "Setting updated"
        else -> "Done"
    }
    return "$prefix ${action.entityLabel}"
}

@Composable
fun VoiceConfirmationDialog(
    action:    VoicePendingAction,
    onConfirm: () -> Unit,
    onCancel:  () -> Unit
) {
    val isEdit = action.actionType.startsWith("EDIT")

    val titleText = when {
        action.actionType.startsWith("CREATE") -> "Create ${action.entityLabel}"
        isEdit                                 -> "Update ${action.entityLabel}"
        action.actionType.startsWith("DELETE") -> "Delete ${action.entityLabel}"
        action.actionType == "CHANGE_SETTING"  -> "Change Setting"
        else                                   -> action.entityLabel
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(BackgroundColor.value)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier  = Modifier.fillMaxWidth(0.92f).padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text       = titleText,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = PrimaryColor
                )

                HorizontalDivider(color = Color.LightGray, thickness = 0.8.dp)

                VoiceInfoCard(
                    fields        = action.summaryFields,
                    changedFields = action.changedFields
                )

                if (isEdit && action.changedFields.isEmpty()) {
                    Text(text = "No changes detected.", fontSize = 14.sp, color = Color.Gray)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick        = onCancel,
                        modifier       = Modifier.weight(1f),
                        colors         = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        contentPadding = PaddingValues(14.dp)
                    ) { Text("Cancel", fontSize = 15.sp, color = Color.White) }

                    Button(
                        onClick        = onConfirm,
                        modifier       = Modifier.weight(1f),
                        colors         = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                        contentPadding = PaddingValues(14.dp)
                    ) { Text("Confirm", fontSize = 15.sp, color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun VoiceInfoCard(
    fields:        List<Pair<String, String>>,
    changedFields: Set<String> = emptySet()
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(CardColor), RoundedCornerShape(12.dp))
    ) {
        fields.forEachIndexed { index, (label, value) ->
            val isChanged = label in changedFields
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text       = label,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color.Gray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text       = value,
                    fontSize   = 16.sp,
                    color      = if (isChanged) PrimaryColor else Color.Unspecified,
                    fontWeight = if (isChanged) FontWeight.Medium else FontWeight.Normal
                )
            }
            if (index < fields.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
        }
    }
}

@Composable
fun VoiceResultDialog(
    result:    VoiceResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(BackgroundColor.value)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(text = "PlannEd Assistant", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryColor)

                HorizontalDivider(color = Color.LightGray, thickness = 0.8.dp)

                if (result.userText.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "You said", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(Color(CardColor)).padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = "\u201C${result.userText}\u201D", fontSize = 13.sp, color = Color.Gray, fontStyle = FontStyle.Italic)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "PlannEd", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(text = result.replyText, fontSize = 14.sp, lineHeight = 20.sp)
                }

                result.actionTaken?.let { actionTaken ->
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(PrimaryColor.copy(alpha = 0.10f))
                            .border(1.dp, PrimaryColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = "\u2713 $actionTaken",
                            fontSize   = 12.sp,
                            color      = PrimaryColor,
                            fontWeight = FontWeight.SemiBold,
                            textAlign  = TextAlign.Center,
                            modifier   = Modifier.fillMaxWidth()
                        )
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Done", color = PrimaryColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}