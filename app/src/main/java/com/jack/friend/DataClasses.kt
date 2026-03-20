package com.jack.friend

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

// =========================
// Firebase Models (SEGUROS)
// =========================

@IgnoreExtraProperties
data class StatusOverlay(
    var text: String = "",
    var x: Float = 0.5f, // Posição relativa (0.0 a 1.0)
    var y: Float = 0.5f,
    var color: Long = 0xFFFFFFFF,
    var fontSize: Float = 24f,
    var fontStyle: String = "Default",
    var stickerUrl: String? = null, // ✅ Novo: URL para sticker ou emoji animado
    var isAnimated: Boolean = false  // ✅ Novo: Define se é animado (Lottie/GIF)
)

// ✅ Novo: Classe para gerenciar múltiplos status em rascunho
data class StatusDraft(
    val uri: Uri,
    var caption: String = "",
    val overlays: SnapshotStateList<StatusOverlay> = mutableStateListOf()
)

@IgnoreExtraProperties
data class ChatSummary(
    var friendId: String = "",
    var lastMessage: String = "",
    var timestamp: Long = 0L,
    var hasUnread: Boolean = false,
    var lastSenderId: String = "",
    var lastMessageRead: Boolean = false,
    var friendName: String? = null,
    var friendPhotoUrl: String? = null,

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    var lastActive: Long = 0L,

    @get:PropertyName("isPinned")
    @set:PropertyName("isPinned")
    var isPinned: Boolean = false,

    @get:PropertyName("isMuted")
    @set:PropertyName("isMuted")
    var isMuted: Boolean = false,

    @get:PropertyName("isTyping")
    @set:PropertyName("isTyping")
    var isTyping: Boolean = false,

    @get:PropertyName("isEphemeral")
    @set:PropertyName("isEphemeral")
    var isEphemeral: Boolean = false,

    var tempDuration: Long = 0L, // ✅ Duração das mensagens temporárias

    @get:PropertyName("isScreenshotDisabled")
    @set:PropertyName("isScreenshotDisabled")
    var isScreenshotDisabled: Boolean = false, // ✅ Nova configuração por chat

    @get:PropertyName("isGroup")
    @set:PropertyName("isGroup")
    var isGroup: Boolean = false,

    @get:PropertyName("isAccepted")
    @set:PropertyName("isAccepted")
    var isAccepted: Boolean = true,

    var presenceStatus: String = "Online"
)

@IgnoreExtraProperties
data class UserStatus(
    var userId: String = "",

    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var isOnline: Boolean = false,

    var lastActive: Long = 0L,
    var username: String = "",
    var imageUrl: String = "",
    var videoUrl: String? = null,
    var isVideo: Boolean = false,
    var timestamp: Long = 0L,
    var userPhotoUrl: String? = null,
    var id: String = "",
    var viewers: Map<String, Long> = emptyMap(), // userId -> timestamp
    var caption: String = "", // ✅ Adicionado campo para descrição
    var overlays: List<StatusOverlay> = emptyList() // ✅ Novo: Textos flutuantes sobre a imagem
)

// =========================
// Helpers (Extensions) - NÃO QUEBRAM FIREBASE
// =========================

// ChatSummary helpers
val ChatSummary.displayName: String
    get() = friendName?.takeIf { it.isNotBlank() } ?: friendId

val ChatSummary.hasPhoto: Boolean
    get() = !friendPhotoUrl.isNullOrBlank()

val ChatSummary.isVisibleOnline: Boolean
    get() = isOnline && presenceStatus != "Invisível"

// UserStatus helpers
val UserStatus.viewerCount: Int
    get() = viewers.size

fun UserStatus.hasViewed(userId: String): Boolean = viewers.containsKey(userId)

val UserStatus.isExpired: Boolean
    get() = System.currentTimeMillis() - timestamp > 86_400_000L // 24h

@IgnoreExtraProperties
data class FeedPost(
    var id: String = "",
    var authorId: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String? = null,
    var text: String = "",
    var timestamp: Long = 0L,
    var photoUrl: String? = null,
    var photoUrls: List<String> = emptyList(), // ✅ Novo: Lista de fotos para posts múltiplos
    var mediaType: String? = null,
    var likes: Map<String, Boolean> = emptyMap(),
    var isPublic: Boolean = true,
    var comments: Map<String, FeedComment> = emptyMap(),
    var reactions: Map<String, String> = emptyMap(), // userId -> emoji
    var animatedEmoji: String? = null,
    var mentions: List<String> = emptyList(), // ✅ Novo: Lista de usernames mencionados (@User)
    var isAd: Boolean = false,
    var adLink: String? = null
)

@IgnoreExtraProperties
data class FeedComment(
    var id: String = "",
    var authorId: String = "",
    var authorName: String = "",
    var authorPhotoUrl: String? = null,
    var text: String = "",
    var timestamp: Long = 0L
)

@IgnoreExtraProperties
data class FeedNotification(
    var id: String = "",
    var type: String = "LIKE", // LIKE, COMMENT, REACTION, MENTION
    var fromId: String = "",
    var fromName: String = "",
    var fromPhotoUrl: String? = null,
    var postId: String = "",
    var postPreviewText: String = "",
    var timestamp: Long = 0L,
    var isRead: Boolean = false,
    var reactionEmoji: String? = null
)
