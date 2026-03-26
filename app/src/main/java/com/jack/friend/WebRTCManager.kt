package com.jack.friend

import android.content.Context
import android.util.Log
import com.google.firebase.database.*
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

class WebRTCManager(
    private val context: Context,
    private val roomId: String,
    private val isCaller: Boolean,
    private val isVideo: Boolean,
    private val eglBaseContext: EglBase.Context,
    private val localVideoView: SurfaceViewRenderer? = null,
    private val remoteVideoView: SurfaceViewRenderer? = null,
    private val onLocalStream: () -> Unit,
    private val onRemoteStream: () -> Unit
) {
    private val database = FirebaseDatabase.getInstance().reference.child("calls").child(roomId)
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var audioDeviceModule: JavaAudioDeviceModule? = null

    private val localCandidatesPath = if (isCaller) "callerCandidates" else "receiverCandidates"
    private val remoteCandidatesPath = if (isCaller) "receiverCandidates" else "callerCandidates"
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    companion object {
        private const val TAG = "WebRTCManager"
        fun initialize(context: Context) {
            try {
                val options = PeerConnectionFactory.InitializationOptions.builder(context)
                    .createInitializationOptions()
                PeerConnectionFactory.initialize(options)
                Log.d(TAG, "WebRTC initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebRTC", e)
            }
        }
    }

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        initialize(context)

        audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
            .createAudioDeviceModule()

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun startCall() {
        Log.d(TAG, "Starting call...")
        // Limpa dados antigos da sala antes de começar
        database.removeValue().addOnCompleteListener {
            createPeerConnection()
            setupLocalStream()
            
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
            }
            
            peerConnection?.createOffer(object : SimpleSdpObserver() {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    Log.d(TAG, "Offer created successfully")
                    val modifiedSdp = sdp?.let { SessionDescription(it.type, modifySdp(it.description)) }
                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            Log.d(TAG, "Local description (offer) set")
                            database.child("offer").setValue(mapOf(
                                "type" to modifiedSdp?.type?.canonicalForm(), 
                                "sdp" to modifiedSdp?.description
                            ))
                        }
                    }, modifiedSdp)
                }
            }, constraints)
            
            listenForAnswer()
            listenForIceCandidates()
        }
    }

    fun answerCall() {
        Log.d(TAG, "Answering call...")
        createPeerConnection()
        setupLocalStream()
        
        // Usa um ValueEventListener para esperar a offer se ela ainda não estiver lá
        database.child("offer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                
                val type = snapshot.child("type").getValue(String::class.java)
                val sdp = snapshot.child("sdp").getValue(String::class.java)
                
                if (type != null && sdp != null && peerConnection?.remoteDescription == null) {
                    Log.d(TAG, "Offer received, setting remote description")
                    val sessionDescription = SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp)
                    peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description (offer) set, creating answer")
                            drainIceCandidates()
                            
                            val constraints = MediaConstraints().apply {
                                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideo) "true" else "false"))
                            }
                            
                            peerConnection?.createAnswer(object : SimpleSdpObserver() {
                                override fun onCreateSuccess(answerDescription: SessionDescription?) {
                                    val modifiedSdp = answerDescription?.let { SessionDescription(it.type, modifySdp(it.description)) }
                                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                                        override fun onSetSuccess() {
                                            Log.d(TAG, "Local description (answer) set")
                                            database.child("answer").setValue(mapOf(
                                                "type" to modifiedSdp?.type?.canonicalForm(), 
                                                "sdp" to modifiedSdp?.description
                                            ))
                                            database.child("status").setValue("CONNECTED")
                                        }
                                    }, modifiedSdp)
                                }
                            }, constraints)
                        }
                    }, sessionDescription)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
        listenForIceCandidates()
    }

    private fun setupLocalStream() {
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack)

        if (isVideo && localVideoView != null) {
            videoCapturer = createVideoCapturer()
            val videoSource = peerConnectionFactory?.createVideoSource(false)
            val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            videoCapturer?.startCapture(1280, 720, 30)

            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
            localVideoTrack?.setEnabled(true)
            localVideoTrack?.addSink(localVideoView)
            peerConnection?.addTrack(localVideoTrack)
        }
        
        onLocalStream()
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        val deviceNames = enumerator.deviceNames
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val capturer = enumerator.createCapturer(deviceName, null)
                if (capturer != null) return capturer
            }
        }
        return null
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "onIceCandidate: sending candidate to Firebase")
                    database.child(localCandidatesPath).push().setValue(mapOf(
                        "sdpMid" to it.sdpMid,
                        "sdpMLineIndex" to it.sdpMLineIndex,
                        "candidate" to it.sdp
                    ))
                }
            }
            
            override fun onTrack(transceiver: RtpTransceiver?) {
                val track = transceiver?.receiver?.track()
                Log.d(TAG, "onTrack received: ${track?.kind()}")
                if (track?.kind() == "audio") {
                    onRemoteStream()
                } else if (track?.kind() == "video" && remoteVideoView != null) {
                    (track as VideoTrack).addSink(remoteVideoView)
                    onRemoteStream()
                }
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "onIceConnectionChange: $state")
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    database.child("status").setValue("CONNECTED")
                }
            }
            
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
            override fun onAddStream(p0: MediaStream?) {}
            override fun onRemoveStream(p0: MediaStream?) {}
            override fun onDataChannel(p0: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun listenForAnswer() {
        database.child("answer").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return
                val type = snapshot.child("type").getValue(String::class.java)
                val sdp = snapshot.child("sdp").getValue(String::class.java)
                if (type != null && sdp != null && peerConnection?.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                    Log.d(TAG, "Answer received, setting remote description")
                    peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description (answer) set")
                            drainIceCandidates()
                        }
                    }, SessionDescription(SessionDescription.Type.fromCanonicalForm(type), sdp))
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForIceCandidates() {
        database.child(remoteCandidatesPath).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(s: DataSnapshot, p: String?) {
                val candidate = s.child("candidate").getValue(String::class.java)
                val mid = s.child("sdpMid").getValue(String::class.java)
                val idx = s.child("sdpMLineIndex").getValue(Int::class.java) ?: 0
                if (candidate != null && mid != null) {
                    val iceCandidate = IceCandidate(mid, idx, candidate)
                    if (peerConnection?.remoteDescription != null) {
                        Log.d(TAG, "Adding ICE candidate to peer connection")
                        peerConnection?.addIceCandidate(iceCandidate)
                    } else {
                        Log.d(TAG, "Remote description not set yet, buffering ICE candidate")
                        pendingIceCandidates.add(iceCandidate)
                    }
                }
            }
            override fun onChildChanged(s: DataSnapshot, p: String?) {}
            override fun onChildRemoved(s: DataSnapshot) {}
            override fun onChildMoved(s: DataSnapshot, p: String?) {}
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun drainIceCandidates() {
        Log.d(TAG, "Draining ${pendingIceCandidates.size} pending ICE candidates")
        pendingIceCandidates.forEach { peerConnection?.addIceCandidate(it) }
        pendingIceCandidates.clear()
    }

    private fun modifySdp(sdp: String): String {
        var modified = sdp
        modified = modified.replace("useinbandfec=1", "useinbandfec=1;stereo=1;maxaveragebitrate=128000")
        
        if (isVideo) {
            val lines = modified.split("\r\n").toMutableList()
            var videoLineIndex = -1
            for (i in lines.indices) {
                if (lines[i].startsWith("m=video")) {
                    videoLineIndex = i
                    break
                }
            }
            if (videoLineIndex != -1) {
                lines.add(videoLineIndex + 1, "b=AS:2500")
            }
            modified = lines.joinToString("\r\n")
        }
        return modified
    }

    fun toggleMute(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun toggleVideo(isCameraOff: Boolean) {
        localVideoTrack?.setEnabled(!isCameraOff)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun onDestroy() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
        } catch (e: Exception) {}
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        audioDeviceModule?.release()
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}
