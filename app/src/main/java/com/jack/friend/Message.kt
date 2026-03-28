package com.jack.friend

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
data class LinkPreview(
    var url: String = "",
    var title: String? = null,
    var description: String? = null,
    var imageUrl: String? = null
) : Serializable

@IgnoreExtraProperties
data class Message(

    // =========================
    // Identificação básica
    // =========================
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var timestamp: Long = 0L,

    // =========================
    // Conteúdo principal
    // =========================
    var text: String = "",
    var imageUrl: String? = null,
    var videoUrl: String? = null,
    var videoThumbnailUrl: String? = null,
    var audioUrl: String? = null,
    var stickerUrl: String? = null,
    var audioDurationSeconds: Long? = null,
    
    // =========================
    // Arquivos (Documentos)
    // =========================
    var fileUrl: String? = null,
    var fileName: String? = null,
    var fileSize: Long? = null,
    var fileExtension: String? = null,

    // =========================
    // Localização
    // =========================
    var latitude: Double? = null,
    var longitude: Double? = null,
    var locationName: String? = null,

    // Campo genérico de mídia (fallback futuro)
    var isMedia: Boolean = false,
    var mediaUrl: String? = null,

    // Link Preview
    var linkPreview: LinkPreview? = null,

    // Anúncio
    var isAd: Boolean = false,
    var adLink: String? = null,
    var adButtonText: String? = null,

    // =========================
    // Estados da mensagem
    // =========================
    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    var isDeleted: Boolean = false,
    var isEdited: Boolean = false,
    var isForwarded: Boolean = false,
    var isViewOnce: Boolean = false,

    @get:PropertyName("audioPlayed")
    @set:PropertyName("audioPlayed")
    var audioPlayed: Boolean = false,

    @get:PropertyName("isStarred")
    @set:PropertyName("isStarred")
    var isStarred: Boolean = false,

    // =========================
    // Reply (resposta)
    // =========================
    var replyToId: String? = null,
    var replyToText: String? = null,
    var replyToName: String? = null,
    var replyToImageUrl: String? = null,

    // =========================
    // Reações
    // =========================
    var reactions: Map<String, String>? = null, // userId -> emoji

    var senderName: String? = null,
    var senderPhotoUrl: String? = null,
    var isSticker: Boolean = false,

    // =========================
    // Chamadas
    // =========================
    var isCall: Boolean = false,
    var callRoomId: String? = null,
    var callType: String? = null,   // AUDIO | VIDEO
    var callStatus: String? = null, // STARTING | RINGING | ACCEPTED | ENDED

    // =========================
    // Expiração (mensagem temporária)
    // =========================
    var expiryTime: Long? = null,
    var tempDurationMillis: Long? = null,

    // =========================
    // Local-only (não vai para o Firebase)
    // =========================
    @get:Exclude
    @set:Exclude
    var localAudioPath: String? = null,

    @get:Exclude
    @set:Exclude
    var isUploading: Boolean = false,

    @get:Exclude
    @set:Exclude
    var uploadProgress: Float = 0f,

    @get:Exclude
    @set:Exclude
    var isDownloading: Boolean = false,

    @get:Exclude
    @set:Exclude
    var localUri: String? = null

) : Serializable {

    // ============================================================
    // 🔥 Helpers Profissionais (não quebram Firebase)
    // ============================================================

    @get:Exclude
    val isImage: Boolean
        get() = !imageUrl.isNullOrEmpty() || (isUploading && localUri != null && !isVideo && !isAudio && !isFile)

    @get:Exclude
    val isVideo: Boolean
        get() = !videoUrl.isNullOrEmpty() || (isUploading && localUri != null && localUri?.contains("video") == true)

    @get:Exclude
    val isAudio: Boolean
        get() = !audioUrl.isNullOrEmpty() || (isUploading && localUri != null && localUri?.contains("audio") == true)

    @get:Exclude
    val isText: Boolean
        get() = text.isNotBlank()

    @get:Exclude
    val isFile: Boolean
        get() = !fileUrl.isNullOrEmpty()

    @get:Exclude
    val isLocation: Boolean
        get() = latitude != null && longitude != null

    @get:Exclude
    val hasMedia: Boolean
        get() = isImage || isVideo || isAudio || !mediaUrl.isNullOrEmpty() || isSticker || isFile || isLocation

    @get:Exclude
    val isExpired: Boolean
        get() = expiryTime != null && System.currentTimeMillis() > expiryTime!!

    @get:Exclude
    val safeText: String
        get() = when {
            isDeleted -> "Mensagem apagada"
            isAd -> "📢 Anúncio"
            isImage -> "📷 Imagem"
            isVideo -> "📹 Vídeo"
            isAudio -> "🎤 Áudio"
            isSticker -> "Sticker"
            isCall -> if (callType == "VIDEO") "📹 Chamada de vídeo" else "📞 Chamada de áudio"
            isFile -> "📄 Arquivo: $fileName"
            isLocation -> "📍 Localização"
            else -> text
        }

    @get:Exclude
    val reactionCount: Int
        get() = reactions?.size ?: 0
}
