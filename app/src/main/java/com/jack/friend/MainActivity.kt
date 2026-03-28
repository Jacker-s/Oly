package com.jack.friend

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.jack.friend.ui.chat.ChatScreen
import com.jack.friend.ui.chat.SecurityWrapper
import com.jack.friend.ui.theme.FriendTheme
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {
    private lateinit var mainViewModel: ChatViewModel
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.FLEXIBLE
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WebRTCManager.initialize(applicationContext)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        billingManager = BillingManager(this)
        checkForAppUpdate()

        setContent {
            FriendTheme {
                mainViewModel = viewModel()
                val isUserLoggedIn by mainViewModel.isUserLoggedIn.collectAsStateWithLifecycle()
                val isProfileChecked by mainViewModel.isProfileChecked.collectAsStateWithLifecycle()
                val myUsername by mainViewModel.myUsername.collectAsStateWithLifecycle()
                val isPremium by billingManager.isPremiumPurchased.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    MobileAds.initialize(this@MainActivity)
                }

                // Carregar e mostrar anúncio premiado após 2 minutos de uso se não for premium
                LaunchedEffect(isPremium) {
                    if (!isPremium) {
                        delay(120_000) // 2 minutos
                        val adRequest = AdRequest.Builder().build()
                        RewardedInterstitialAd.load(this@MainActivity, "ca-app-pub-7931782163570852/1428917414", adRequest, object : RewardedInterstitialAdLoadCallback() {
                            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                                ad.show(this@MainActivity) { rewardItem ->
                                    // Recompensa processada (se houver lógica específica futura)
                                }
                            }
                        })
                    }
                }

                var showAdNoticeDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val adNoticeShown = prefs.getBoolean("ad_notice_shown", false)
                    if (!adNoticeShown && !isPremium) {
                        showAdNoticeDialog = true
                    }
                }

                if (showAdNoticeDialog) {
                    AlertDialog(
                        onDismissRequest = { /* Não permite fechar fora */ },
                        title = { Text("Aviso de Anúncios") },
                        text = { Text("Para manter o Oly gratuito e com servidores ativos, poderemos exibir alguns anúncios durante o uso do chat. Você pode remover os anúncios comprando o pacote Premium!") },
                        confirmButton = {
                            Button(onClick = {
                                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                                prefs.edit().putBoolean("ad_notice_shown", true).apply()
                                showAdNoticeDialog = false
                            }) {
                                Text("Entendi")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                billingManager.launchPurchaseFlow(this@MainActivity)
                                showAdNoticeDialog = false
                            }) {
                                Text("Seja Premium")
                            }
                        }
                    )
                }

                val permissionsToRequest = remember {
                    val list = mutableListOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                        list.add(Manifest.permission.READ_MEDIA_IMAGES)
                        list.add(Manifest.permission.READ_MEDIA_VIDEO)
                        list.add(Manifest.permission.READ_MEDIA_AUDIO)
                    } else {
                        list.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        list.add(Manifest.permission.BLUETOOTH_CONNECT)
                    }

                    list.toTypedArray()
                }

                val multiplePermissionResultLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                    onResult = { perms ->
                        perms.forEach { (perm, isGranted) ->
                            Log.d("MainActivity", "Permission $perm granted: $isGranted")
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    val allPermissionsGranted = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (!allPermissionsGranted) {
                        multiplePermissionResultLauncher.launch(permissionsToRequest)
                    } else {
                        requestLocation()
                    }
                }

                if (!isUserLoggedIn || (isProfileChecked && myUsername.isEmpty())) {
                    LaunchedEffect(isUserLoggedIn, isProfileChecked, myUsername) {
                        if (!isUserLoggedIn || (isProfileChecked && myUsername.isEmpty())) {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                            finish()
                        }
                    }
                } else if (!isProfileChecked) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    SecurityWrapper(
                        isUserLoggedIn = true, 
                        viewModel = mainViewModel, 
                        billingManager = billingManager,
                        activity = this@MainActivity
                    )
                }

                var showOverlayDialog by remember { mutableStateOf(false) }
                var showBatteryOptimizationDialog by remember { mutableStateOf(false) }

                val overlayLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    // Verificação após retorno
                }

                LaunchedEffect(Unit) {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        showOverlayDialog = true
                    } else {
                        val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
                        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                            showBatteryOptimizationDialog = true
                        }
                    }
                }

                if (showBatteryOptimizationDialog) {
                    AlertDialog(
                        onDismissRequest = { showBatteryOptimizationDialog = false },
                        title = { Text("Mensagens em Segundo Plano") },
                        text = { Text("Para garantir que você receba mensagens mesmo quando o app estiver fechado, o Oly precisa ser removido das 'Otimizações de Bateria'. Deseja configurar?") },
                        confirmButton = {
                            Button(onClick = {
                                showBatteryOptimizationDialog = false
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                    startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback para as configurações gerais se falhar
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    startActivity(intent)
                                }
                            }) {
                                Text("Remover Otimização")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBatteryOptimizationDialog = false }) {
                                Text("Agora não")
                            }
                        }
                    )
                }

                LaunchedEffect(intent) {
                    handleIntent(intent)
                }
            }
        }
    }

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(updateType)) {
                try {
                    appUpdateManager.startUpdateFlowForResult(info, updateType, this, 123)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        val listener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) appUpdateManager.completeUpdate()
        }
        appUpdateManager.registerListener(listener)
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) appUpdateManager.completeUpdate()
        }
        if (::billingManager.isInitialized) {
            billingManager.queryPurchases()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                mainViewModel.setPendingShare(sharedText, emptyList())
            } else if (type.startsWith("image/") || type.startsWith("video/")) {
                val mediaUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (mediaUri != null) {
                    mainViewModel.setPendingShare(null, listOf(mediaUri))
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            if (type.startsWith("image/") || type.startsWith("video/")) {
                val mediaUris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (mediaUris != null) {
                    mainViewModel.setPendingShare(null, mediaUris)
                }
            }
        } else {
            val targetId = intent.getStringExtra("targetId")
            if (!targetId.isNullOrEmpty()) {
                mainViewModel.setTargetId(targetId)
            }
            
            val openFeed = intent.getBooleanExtra("openFeed", false)
            val postId = intent.getStringExtra("postId")
            if (openFeed) {
                mainViewModel.setOpenFeed(true)
                if (!postId.isNullOrEmpty()) {
                    mainViewModel.setOpenPostId(postId)
                }
            }
        }
    }

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    if (::mainViewModel.isInitialized) {
                        mainViewModel.updateUserLocation(it.latitude, it.longitude)
                    }
                }
            }
        }
    }
}
