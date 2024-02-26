package com.domain_name.app_name

import android.os.Bundle
import android.util.Log
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import org.puredata.android.io.AudioParameters
import org.puredata.android.io.PdAudio
import org.puredata.android.service.PdPreferences
import org.puredata.android.service.PdService
import org.puredata.android.utils.PdUiDispatcher
import org.puredata.core.PdBase
import org.puredata.core.utils.IoUtils
import java.io.File
import java.io.IOException

class MainActivity : FlutterActivity() {

    private val LOG_TAG = "PdTest"
    private val CHANNEL = "com.domain_name.app_name"
    private val MIC_PERMISSION_REQUEST_CODE = 123 // You can use any code

    private var pdService: PdService? = null
    private lateinit var dispatcher: PdUiDispatcher

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call: MethodCall, result: MethodChannel.Result ->
            when {
                call.method == "startTone" -> PdBase.sendFloat("onOff", 1.0f)
                call.method == "stopTone" -> PdBase.sendFloat("onOff", 0.0f)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestMicrophonePermission()
    }

    private fun requestMicrophonePermission() {
        val microphonePermission = android.Manifest.permission.RECORD_AUDIO
        val permissionStatus = ContextCompat.checkSelfPermission(this, microphonePermission)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(microphonePermission), MIC_PERMISSION_REQUEST_CODE)
        } else {
            initializeAudioEngine()
        }
    }


    private fun initializeAudioEngine() {
        AudioParameters.init(this)
        PdPreferences.initPreferences(applicationContext)

        initPd()
    }

    private fun initPd() {
        dispatcher = PdUiDispatcher()
        PdBase.setReceiver(dispatcher)

        val sampleRate = AudioParameters.suggestSampleRate()

        try {
            PdAudio.initAudio(sampleRate, 1, 2, 8, true)
            loadPdPatch()
            startAudio()
        } catch (e: IOException) {
            Log.v(LOG_TAG, "failed to init pd audio")
        }
    }

    private fun startAudio() {
        try {
            pdService?.initAudio(-1, -1, -1, -1f)
        } catch (e: IOException) {
            Log.v(LOG_TAG, "something went wrong attempting start audio")
        }
    }

    private fun stopAudio() {
        pdService?.stopAudio()
    }

    private fun loadPdPatch() {
        try {
            val dir: File = filesDir
            IoUtils.extractZipResource(resources.openRawResource(R.raw.soundtest), dir, true) // This zip file contains the pd patches and any other files needed
            val patchFile = File(dir, "soundtest.pd")
            PdBase.openPatch(patchFile.absolutePath)
        } catch (e: IOException) {
            Log.v(LOG_TAG, "failed to load pd patch")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == MIC_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            initializeAudioEngine()
        } else {
            // Handle permission denied
            // You might want to show a message to the user or close the app
            Log.e(LOG_TAG, "Microphone permission denied")
        }
    }

    override fun onPause() {
        super.onPause()
        PdAudio.stopAudio()
    }

    override fun onResume() {
        super.onResume()
        PdAudio.startAudio(this)
    }
}
