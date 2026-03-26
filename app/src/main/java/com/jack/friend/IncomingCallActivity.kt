package com.jack.friend

import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import android.view.WindowManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.database.*
import androidx.compose.ui.res.stringResource
import com.jack.friend.R
import com.jack.friend.ui.theme.FriendTheme
import kotlin.math.max
import kotlin.math.roundToInt

class IncomingCallActivity : ComponentActivity() {

    companion object {
        private const val TAG = "IncomingCallActivity"
        private const val EXTRA_CALL_MESSAGE = "callMessage"

        // ✅ vamos padronizar: o app todo usa "isVideo"
        private const val EXTRA_IS_VIDEO = "isVideo"

        private const val NOTIF_ID_INCOMING_CALL = 1002

        private const val STATUS_CONNECTED = "CONNECTED"
        private const val STATUS_REJECTED = "REJECTED"
        private const val STATUS_ENDED = "ENDED"

        private const val TIMEOUT_MS = 30_000L
    }

    private var mediaPlayer: MediaPlayer? = null

    private var callRef: DatabaseReference? = null
    private var callStatusListener: ValueEventListener? = null
    private var roomId: String? = null

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val handler by lazy { Handler(mainLooper) }
    private var timeoutRunnable: Runnable? = null

    private var hasFinished = false
    private var isAccepted = false

    // ✅ tipo da call (áudio/vídeo)
    private var isVideoCall: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        showOnLockScreen()
        initVibrator()
        acquireWakeLock()

        val message = getCallMessageOrFinish() ?: return
        roomId = message.callRoomId

        // ✅ 1) tenta pegar do Intent
        isVideoCall = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        observeCallStatusAndType()
        startTimeout()
        startRingtone()
        startVibration()

        setContent {
            FriendTheme {
                IncomingCallScreen(
                    callerName = message.senderName ?: message.senderId,
                    callerPhotoUrl = message.senderPhotoUrl,
                    isVideo = isVideoCall,
                    onAccept = { acceptCall(message) },
                    onReject = { rejectCall() },
                    onRemind = { scheduleReminder(message) },
                    onMessage = { showQuickMessageDialog(message) }
                )
            }
        }
    }

    private fun scheduleReminder(message: Message) {
        val callerName = message.senderName ?: message.senderId
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("callerName", callerName)
            putExtra("callerId", message.senderId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            message.senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Lembrete em 15 minutos
        val triggerTime = SystemClock.elapsedRealtime() + 15 * 60 * 1000L
        
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
            android.widget.Toast.makeText(this, getString(R.string.call_reminder_toast), android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao agendar alarme: ${e.message}")
        }
        
        rejectCall()
    }

    private fun showQuickMessageDialog(message: Message) {
        // Para simplificar no IncomingCallActivity (que é ComponentActivity),
        // vamos lidar com o estado do BottomSheet via Compose
        isQuickMessageVisible.value = true
    }

    val isQuickMessageVisible = mutableStateOf(false)

    fun sendQuickMessage(message: Message, text: String) {
        val me = message.receiverId
        val target = message.senderId
        val db = FirebaseDatabase.getInstance().reference
        
        val msgId = db.push().key ?: return
        val msg = Message(
            id = msgId,
            senderId = me,
            receiverId = target,
            text = text,
            timestamp = System.currentTimeMillis(),
            isGroup = false
        )
        
        // Caminho da mensagem (mesmo chatKey usado no ViewModel)
        val a = me.uppercase().trim()
        val b = target.uppercase().trim()
        val chatKey = if (a < b) "${a}_$b" else "${b}_$a"
        
        db.child("messages").child(chatKey).child(msgId).setValue(msg)
        
        isQuickMessageVisible.value = false
        rejectCall()
    }

    fun getCallMessageOrFinish(): Message? {
        val msg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CALL_MESSAGE, Message::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CALL_MESSAGE) as? Message
        }

        if (msg == null) {
            safeFinish()
            return null
        }
        return msg
    }

    private fun showOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * ✅ CORREÇÃO: observa STATUS e também isVideo (fallback Firebase).
     * Assim o incoming reconhece vídeo mesmo que a Message não tenha esse campo.
     */
    private fun observeCallStatusAndType() {
        val id = roomId ?: return
        callRef = FirebaseDatabase.getInstance().reference.child("calls").child(id)

        callStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // ✅ fallback do tipo de call via Firebase
                val firebaseIsVideo = snapshot.child("isVideo").getValue(Boolean::class.java)
                if (firebaseIsVideo != null) {
                    isVideoCall = firebaseIsVideo
                }

                if (hasFinished || isAccepted) return

                val status = snapshot.child("status").getValue(String::class.java)
                if (status == STATUS_ENDED || status == STATUS_REJECTED || status == STATUS_CONNECTED) {
                    cleanupAndFinish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "observeCallStatus cancelled: ${error.message}")
            }
        }

        callRef?.addValueEventListener(callStatusListener!!)
    }

    private fun startTimeout() {
        cancelTimeout()
        timeoutRunnable = Runnable {
            if (!hasFinished && !isAccepted) {
                setStatus(STATUS_ENDED)
                cleanupAndFinish()
            }
        }
        handler.postDelayed(timeoutRunnable!!, TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun initVibrator() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun startVibration() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        val pattern = longArrayOf(0, 350, 200, 350, 800)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try { vibrator?.cancel() } catch (_: Exception) {}
    }

    private fun startRingtone() {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ringtone error: ${e.message}", e)
        }
    }

    private fun stopRingtone() {
        stopVibration()
        try {
            mediaPlayer?.let {
                runCatching { if (it.isPlaying) it.stop() }
                runCatching { it.release() }
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID_INCOMING_CALL)
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:incoming_call")
            wakeLock?.setReferenceCounted(false)
            wakeLock?.acquire(TIMEOUT_MS + 10_000L)
        } catch (e: Exception) {
            Log.e(TAG, "WakeLock error: ${e.message}", e)
        }
    }

    private fun releaseWakeLock() {
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: Exception) {}
        wakeLock = null
    }

    private fun setStatus(status: String) {
        val id = roomId ?: return
        FirebaseDatabase.getInstance().reference
            .child("calls").child(id).child("status")
            .setValue(status)
    }

    private fun acceptCall(message: Message) {
        if (isAccepted || hasFinished) return
        isAccepted = true

        cancelTimeout()
        stopRingtone()
        setStatus(STATUS_CONNECTED)

        // ✅ CORREÇÃO PRINCIPAL: passar isVideo corretamente
        startActivity(
            Intent(this, CallActivity::class.java).apply {
                putExtra("roomId", message.callRoomId)
                putExtra("targetId", message.senderId)
                putExtra("targetPhotoUrl", message.senderPhotoUrl)
                putExtra("isOutgoing", false)
                putExtra("isVideo", isVideoCall) // ✅ agora sempre certo
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )

        safeFinish()
    }

    private fun rejectCall() {
        if (hasFinished) return
        cancelTimeout()
        setStatus(STATUS_REJECTED)
        cleanupAndFinish()
    }

    private fun cleanupAndFinish() {
        if (hasFinished) return
        hasFinished = true

        cancelTimeout()
        stopRingtone()

        callStatusListener?.let { listener ->
            callRef?.removeEventListener(listener)
        }
        callStatusListener = null
        callRef = null

        releaseWakeLock()
        safeFinish()
    }

    private fun safeFinish() {
        if (!isFinishing && !isDestroyed) finish()
    }

    override fun onDestroy() {
        cancelTimeout()
        stopRingtone()

        callStatusListener?.let { listener ->
            callRef?.removeEventListener(listener)
        }
        callStatusListener = null
        callRef = null

        releaseWakeLock()
        super.onDestroy()
    }
}

// ============================
// UI - Premium Redesign
// ============================
@Composable
private fun IncomingCallScreen(
    callerName: String,
    callerPhotoUrl: String?,
    isVideo: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onRemind: () -> Unit,
    onMessage: () -> Unit
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as IncomingCallActivity
    val showQuickMessages by activity.isQuickMessageVisible
    val message = activity.getCallMessageOrFinish()

    if (showQuickMessages && message != null) {
        QuickMessageBottomSheet(
            onDismiss = { activity.isQuickMessageVisible.value = false },
            onSelect = { activity.sendQuickMessage(message, it) }
        )
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Full screen background image with blur
        if (!callerPhotoUrl.isNullOrBlank()) {
            AsyncImage(
                model = callerPhotoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(60.dp).scale(1.4f),
                contentScale = ContentScale.Crop,
                alpha = 0.5f
            )
        }

        // Overlay Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(0.8f))
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp, vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            
            // Header
            Text(
                if (isVideo) stringResource(R.string.call_type_video) else stringResource(R.string.call_type_audio),
                color = Color.White.copy(0.5f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            
            Spacer(Modifier.height(40.dp))

            // Caller Card
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    // Outer Ring
                    Box(
                        Modifier
                            .size(170.dp)
                            .scale(pulse)
                            .border(1.dp, Color.White.copy(0.15f), CircleShape)
                    )
                    
                    Surface(
                        modifier = Modifier.size(140.dp),
                        shape = CircleShape,
                        color = Color.White.copy(0.05f),
                        border = BorderStroke(2.dp, Color.White.copy(0.2f))
                    ) {
                        if (!callerPhotoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = callerPhotoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(70.dp).padding(30.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(30.dp))

                Text(
                    text = callerName,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
                
                Text(
                    text = stringResource(R.string.country_name_default), 
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickActionItem(icon = Icons.Default.Alarm, label = stringResource(R.string.call_action_remind), onClick = onRemind)
                QuickActionItem(icon = Icons.Default.Message, label = stringResource(R.string.call_action_message), onClick = onMessage)
            }

            // Main Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onReject,
                        containerColor = Color(0xFFFF3B30),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(Icons.Rounded.CallEnd, null, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.call_action_reject), color = Color.White, fontSize = 14.sp)
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF34C759),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(if (isVideo) Icons.Default.Videocam else Icons.Rounded.Call, null, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.call_action_accept), color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun QuickMessageBottomSheet(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val messages = listOf(
        stringResource(R.string.call_quick_msg_1),
        stringResource(R.string.call_quick_msg_2),
        stringResource(R.string.call_quick_msg_3),
        stringResource(R.string.call_quick_msg_4)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF1C1C1E),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.call_sheet_header),
                modifier = Modifier.padding(16.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            
            messages.forEach { msg ->
                Text(
                    text = msg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(msg) }
                        .padding(16.dp),
                    fontSize = 18.sp,
                    color = Color.White
                )
                HorizontalDivider(color = Color.White.copy(0.1f))
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Cancelar")
            }
        }
    }
}
