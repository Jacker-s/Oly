package com.jack.friend

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import com.google.android.gms.ads.MobileAds
import com.google.firebase.database.FirebaseDatabase
import com.cloudinary.android.MediaManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FriendApplication : Application(), Application.ActivityLifecycleCallbacks, ImageLoaderFactory {

    private var activityCount = 0
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground

    companion object {
        lateinit var instance: FriendApplication
            private set
        var isAppInForeground: Boolean = false
        var currentOpenedChatId: String? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerActivityLifecycleCallbacks(this)

        // Inicializar AdMob
        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            MobileAds.initialize(this@FriendApplication) {}
        }

        // Inicializar Cloudinary
        try {
            val config = mapOf(
                "cloud_name" to CloudinaryConfig.CLOUD_NAME,
                "api_key" to CloudinaryConfig.API_KEY,
                "api_secret" to CloudinaryConfig.API_SECRET
            )
            MediaManager.init(this, config)
        } catch (e: Exception) {
            // Se já foi inicializado
        }

        // Ativa persistência offline do Firebase
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        } catch (e: Exception) {
            // Pode falhar se já tiver sido chamado
        }

        // INICIAR O SERVIÇO DE MENSAGENS/CHAMADAS COMO BACKGROUND SERVICE
        // Removemos o startForegroundService para não exibir a notificação fixa
        startMessagingService()
    }

    private fun startMessagingService() {
        val intent = Intent(this, MessagingService::class.java)
        startService(intent)
    }

    fun clearAppData() {
        try {
            // Limpa SharedPreferences conhecidas
            val prefs = listOf("friend_prefs", "security_prefs", "ui_prefs", "recent_emojis_prefs", "chat_cache_prefs")
            prefs.forEach { name ->
                getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
            }

            // Limpa Cache (arquivos de áudio, imagens temporárias)
            cacheDir.deleteRecursively()

            // Limpa arquivos internos se houver
            filesDir.deleteRecursively()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            isAppInForeground = true
            _isForeground.value = true
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            isAppInForeground = false
            _isForeground.value = false
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
