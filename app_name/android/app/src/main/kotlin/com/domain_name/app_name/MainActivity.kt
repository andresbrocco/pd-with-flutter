package com.domain_name.app_name

import android.os.Bundle
import android.util.Log
import android.content.pm.PackageManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import androidx.core.content.ContextCompat

import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

import org.puredata.android.io.AudioParameters
import org.puredata.android.io.PdAudio
import org.puredata.core.PdBase
import org.puredata.core.PdReceiver
import org.puredata.core.utils.IoUtils

import java.io.File
import java.io.IOException
import java.util.Arrays
import java.util.HashMap

class MainActivity : FlutterActivity() {

    private val LOG_TAG = "SIMPLE_LOOPER"
    private val METHOD_CHANNEL = "com.domain_name.app_name"
    private val EVENT_CHANNEL = "com.domain_name.app_name/vumeter"
    private val MIC_PERMISSION_REQUEST_CODE = 123 // You can use any code

    private val receiver = object : PdReceiver { // Receives messages from pd
        override fun print(s: String) {
            Log.v(LOG_TAG, "received: $s")
        }

        override fun receiveBang(source: String) {
            print("bang")
        }

        override fun receiveFloat(source: String, x: Float) {
            print("$source - float: $x")
            when (source) {
                "vuDrum" -> onVUMeter("drum", x)
                "vuBass" -> onVUMeter("bass", x)
            }
        }

        override fun receiveList(source: String, vararg args: Any) {
            print("list: " + Arrays.toString(args))
        }

        override fun receiveMessage(source: String, symbol: String, vararg args: Any) {
            print("message: " + Arrays.toString(args))
        }

        override fun receiveSymbol(source: String, symbol: String) {
            print("symbol: $symbol")
        }
    }

    private fun requestMicrophonePermission() {
        val microphonePermission = android.Manifest.permission.RECORD_AUDIO
        val permissionStatus = ContextCompat.checkSelfPermission(this, microphonePermission)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(microphonePermission), MIC_PERMISSION_REQUEST_CODE)
        } else {
            initAudio(true)
        }
    }

    private fun initAudio(micPermissionGranted: Boolean) {
        AudioParameters.init(this)

        val sampleRate = AudioParameters.suggestSampleRate()

        try {
            PdAudio.initAudio(sampleRate, if(micPermissionGranted) 1 else 0, 2, 8, true)
            Log.v(LOG_TAG, "audio started")
        } catch (e: IOException) {
            Log.v(LOG_TAG, "something went wrong attempting start audio")
        }
    }

    private fun loadPdPatch() {
        PdBase.setReceiver(receiver) // The pd engine gets started here, because it's the first call to a PdBase method.
        PdBase.subscribe("vuDrum")
        PdBase.subscribe("vuBass")

        try {
            val dir: File = filesDir
            Log.v(LOG_TAG, "filesDir: $dir")
            IoUtils.extractZipResource(resources.openRawResource(R.raw.pd_files), dir, true) // This is the zip file containing the puredata patch and resources (audio files, etc.). Attention! The compression must be performed by selecting the files, and not a folder containing the files!
            val patchFile = File(dir, "looper.pd")
            PdBase.openPatch(patchFile.absolutePath)
        } catch (e: IOException) {
            Log.v(LOG_TAG, "failed to load pd patch")
        }
    }

    private fun onVUMeter(track: String, v: Float) {
        val intent = Intent("onVuMeter")
        val result: HashMap<String, Any> = HashMap()
        result["track"] = track
        result["value"] = v
        intent.putExtra("vuMeter", result)
        sendBroadcast(intent)
    }

    private fun initMethodHandlers() {
        MethodChannel(flutterEngine!!.dartExecutor.binaryMessenger, METHOD_CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "dspToggle" -> {
                        val dspToggle = call.argument<Double>("toggle")
                        PdBase.sendFloat("dspToggle", dspToggle?.toFloat() ?: 0.0f)
                        PdBase.sendFloat("vuToggle", 0.0f)
                    }
                    "bangStart" -> {
                        PdBase.sendFloat("vuToggle", 1.0f)
                        PdBase.sendBang("bangStart")
                    }
                    "bangStop" -> {
                        PdBase.sendFloat("vuToggle", 0.0f)
                        PdBase.sendBang("bangStop")
                    }
                    "looperSliderSet" -> {
                        val slider = call.argument<String>("source") + "Volume"
                        val value = call.argument<Double>("value")
                        PdBase.sendFloat(slider, value?.toFloat() ?: 0.0f)
                    }
                }
            }
    }

    private fun initEventHandlers() {
        EventChannel(flutterEngine!!.dartExecutor.binaryMessenger, EVENT_CHANNEL)
            .setStreamHandler(object : EventChannel.StreamHandler {
                private var vuMeterBroadcastReceiver: BroadcastReceiver? = null

                override fun onListen(args: Any?, events: EventChannel.EventSink) {
                    Log.w(LOG_TAG, "adding listener")
                    vuMeterBroadcastReceiver = createVuMeterReceiver(events)
                    registerReceiver(vuMeterBroadcastReceiver, IntentFilter("onVuMeter"))
                }

                override fun onCancel(args: Any?) {
                    Log.w(LOG_TAG, "cancelling listener")
                }
            })
    }

    private fun createVuMeterReceiver(events: EventChannel.EventSink): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val intentResult = intent.getSerializableExtra("vuMeter") as HashMap<*, *>
                events.success(intentResult)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == MIC_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.e(LOG_TAG, "Microphone permission accepted")
            initAudio(true)
        } else {
            Log.e(LOG_TAG, "Microphone permission denied")
            initAudio(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initMethodHandlers() // communication from flutter app
        initEventHandlers() // communication to flutter app
        loadPdPatch() // loads, but does not turn on the dsp (audio processing)
        requestMicrophonePermission() // asynchronus: when approved, turns on the dsp
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
