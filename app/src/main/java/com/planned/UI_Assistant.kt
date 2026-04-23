package com.planned

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Assistant(db: AppDatabase) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var inputText         by remember { mutableStateOf("") }
    var userBubbleText    by remember { mutableStateOf<String?>(null) }
    var replyBubbleText   by remember { mutableStateOf<String?>(null) }
    var isThinking        by remember { mutableStateOf(false) }
    var hasInteracted     by remember { mutableStateOf(false) }
    var phase             by remember { mutableStateOf(VoicePhase.IDLE) }
    var pendingAction     by remember { mutableStateOf<VoicePendingAction?>(null) }
    var showPendingDialog by remember { mutableStateOf(false) }

    // VoiceCommandManager
    DisposableEffect(Unit) {
        VoiceCommandManager.initTts(context)
        VoiceCommandManager.onPhaseChange = { newPhase ->
            phase = newPhase
            if (newPhase == VoicePhase.THINKING) isThinking = true
            if (newPhase == VoicePhase.IDLE || newPhase == VoicePhase.ERROR) isThinking = false
        }
        VoiceCommandManager.onResult = { result ->
            isThinking = false
            if (result.userText.isNotBlank()) userBubbleText = result.userText
            replyBubbleText = result.replyText
        }
        VoiceCommandManager.onPendingAction = { action ->
            isThinking        = false
            pendingAction     = action
            showPendingDialog = true
        }
        VoiceCommandManager.onSpokenText = { spoken ->
            userBubbleText  = spoken
            replyBubbleText = null
            hasInteracted   = true
        }
        onDispose { VoiceCommandManager.releaseTts() }
    }

    // Send handler
    fun sendText(text: String) {
        if (text.isBlank()) return
        keyboardController?.hide()
        hasInteracted   = true
        userBubbleText  = text
        replyBubbleText = null
        isThinking      = true
        inputText       = ""
        scope.launch {
            VoiceCommandManager.handleSpokenCommand(context, db, text)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) VoiceCommandManager.startListening(context, db)
        else phase = VoicePhase.ERROR
    }

    // Mic pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation  = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val micColor = when (phase) {
        VoicePhase.IDLE      -> PrimaryColor
        VoicePhase.LISTENING -> PrimaryColor
        VoicePhase.THINKING  -> Color.LightGray
        VoicePhase.SPEAKING  -> Color.LightGray
        VoicePhase.ERROR     -> Color(0xFF9E9E9E)
    }
    val isActive     = phase != VoicePhase.IDLE && phase != VoicePhase.ERROR
    val appliedScale = if (phase == VoicePhase.LISTENING) pulseScale else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        // Title bar
        Column(modifier = Modifier.background(BackgroundColor)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text       = "Assistant",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Chat area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(userBubbleText, replyBubbleText, isThinking) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)
            ) {
                // User bubble
                userBubbleText?.let { text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text       = "You",
                                fontSize   = 11.sp,
                                color      = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                modifier   = Modifier.padding(end = 4.dp, bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart    = 16.dp, topEnd    = 4.dp,
                                            bottomStart = 16.dp, bottomEnd = 16.dp
                                        )
                                    )
                                    .background(PrimaryColor)
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(text = text, fontSize = 15.sp, color = Color.White, lineHeight = 21.sp)
                            }
                        }
                    }
                }

                // Assistant bubble
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text       = "PlannEd",
                            fontSize   = 11.sp,
                            color      = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            modifier   = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart    = 4.dp,  topEnd    = 16.dp,
                                        bottomStart = 16.dp, bottomEnd = 16.dp
                                    )
                                )
                                .background(Color.LightGray)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .widthIn(max = 260.dp)
                        ) {
                            when {
                                isThinking              -> ThinkingDots()
                                replyBubbleText != null -> Text(
                                    text       = replyBubbleText!!,
                                    fontSize   = 15.sp,
                                    color      = Color.Black,
                                    lineHeight = 21.sp
                                )
                                !hasInteracted          -> Text(
                                    text       = "How can I help you?",
                                    fontSize   = 15.sp,
                                    color      = Color.Black,
                                    lineHeight = 21.sp
                                )
                                else                    -> {}
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Mic button
            Box(contentAlignment = Alignment.Center) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(appliedScale)
                            .clip(CircleShape)
                            .background(micColor.copy(alpha = 0.18f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(micColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = ripple(bounded = true)
                        ) {
                            when (phase) {
                                VoicePhase.IDLE, VoicePhase.ERROR -> {
                                    hasInteracted   = true
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) VoiceCommandManager.startListening(context, db)
                                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                VoicePhase.LISTENING -> {
                                    VoiceCommandManager.stopListening()
                                    phase      = VoicePhase.THINKING
                                    isThinking = true
                                }
                                VoicePhase.SPEAKING -> VoiceCommandManager.cancelSpeech()
                                VoicePhase.THINKING -> { /* Can't interrupt */ }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (phase == VoicePhase.ERROR) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Voice input",
                        tint               = Color.White,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }

            // Text field
            OutlinedTextField(
                value         = inputText,
                onValueChange = { inputText = it },
                placeholder   = { Text("Message PlannEd…", fontSize = 14.sp, color = Color.Gray) },
                modifier      = Modifier.weight(1f),
                shape         = RoundedCornerShape(24.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor      = PrimaryColor,
                    unfocusedBorderColor    = Color.LightGray,
                    focusedContainerColor   = BackgroundColor,
                    unfocusedContainerColor = BackgroundColor
                ),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendText(inputText) }),
                textStyle       = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            // Send button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isBlank()) Color.LightGray else PrimaryColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = ripple(bounded = true),
                        enabled           = inputText.isNotBlank()
                    ) { sendText(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint               = Color.White,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }

    // Confirmation dialog
    if (showPendingDialog) {
        pendingAction?.let { action ->
            VoiceConfirmationDialog(
                action    = action,
                onConfirm = {
                    showPendingDialog = false
                    pendingAction     = null
                    scope.launch {
                        action.execute()
                        replyBubbleText = action.replyText
                        VoiceCommandManager.speakOut(action.replyText)
                    }
                },
                onCancel = {
                    showPendingDialog = false
                    pendingAction     = null
                    replyBubbleText   = "Cancelled."
                    isThinking        = false
                }
            )
        }
    }
}

// Animated thinking dots
@Composable
fun ThinkingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    @Composable
    fun dot(delayMs: Int): Float {
        val alpha by infiniteTransition.animateFloat(
            initialValue  = 0.3f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(600, easing = FastOutSlowInEasing, delayMillis = delayMs),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot$delayMs"
        )
        return alpha
    }

    val a1 = dot(0)
    val a2 = dot(200)
    val a3 = dot(400)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.height(20.dp)
    ) {
        listOf(a1, a2, a3).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray.copy(alpha = alpha))
            )
        }
    }
}