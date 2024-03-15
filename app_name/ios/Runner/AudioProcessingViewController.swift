import UIKit
import Flutter
import AVFoundation

class AudioProcessingViewController: FlutterViewController {
    let pdAudioController = PdAudioController()
    var registrar : FlutterPluginRegistrar?

    let LOG_TAG = "SIMPLE_LOOPER"
    let METHOD_CHANNEL = "com.domain_name.app_name/method"
    let EVENT_CHANNEL = "com.domain_name.app_name/vumeter"

    override func viewDidLoad() {
        super.viewDidLoad()
        requestMicrophonePermission()
        initMethodHandlers()
        initEventHandlers()
        loadPdPatch()
    }

    func requestMicrophonePermission() {
        switch AVAudioSession.sharedInstance().recordPermission {
        case .undetermined:
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                DispatchQueue.main.async {
                    if granted {
                        self.initAudio(micPermissionGranted: true)
                    } else {
                        self.initAudio(micPermissionGranted: false)
                    }
                }
            }
        case .denied:
            self.initAudio(micPermissionGranted: false)
        case .granted:
            self.initAudio(micPermissionGranted: true)
        @unknown default:
            fatalError("Unknown record permission state")
        }
    }

    func initAudio(micPermissionGranted: Bool) {
        let sampleRate = 44100 // Commonly used sample rate, you might want to adjust
        let inputChannels: Int = micPermissionGranted ? 1 : 0 // Enable input if permission granted
        let outputChannels: Int = 2 // Stereo output
        
        do {
            try AVAudioSession.sharedInstance().setCategory(micPermissionGranted ? .playAndRecord : .playback, mode: .default)
            try AVAudioSession.sharedInstance().setActive(true)
            
            let status = pdAudioController?.configurePlayback(withSampleRate: Int32(sampleRate),
                                                            inputChannels: Int32(inputChannels),
                                                            outputChannels: Int32(outputChannels),
                                                            inputEnabled: micPermissionGranted)
            
            if status != PdAudioOK {
                print("\(LOG_TAG): Audio configured successfully with sample rate \(sampleRate)")
                // Proceed with further libpd initialization if necessary
            } else {
                print("\(LOG_TAG): Failed to configure audio with libpd")
            }
        } catch {
            print("\(LOG_TAG): Something went wrong attempting to start audio session")
        }
    }

    private func initMethodHandlers() {
        let methodChannel = FlutterMethodChannel(name: METHOD_CHANNEL, binaryMessenger: registrar.messenger())
        methodChannel.setMethodCallHandler { [weak self] (call, result) in
            // Handle method calls here, similar to Android
        }
    }

    private func initEventHandlers() {
        let eventChannel = FlutterEventChannel(name: EVENT_CHANNEL, binaryMessenger: binaryMessenger!)
        eventChannel.setStreamHandler(self)
    }

    private func loadPdPatch() {
        let patch = Bundle.main.url(forResource: "simple_looper", withExtension: "pd")
        let patchHandle = PdBase.openFile(patch?.path, path: patch?.deletingLastPathComponent().path)
        if patchHandle != 0 {
            print("\(LOG_TAG): Patch loaded successfully")
        } else {
            print("\(LOG_TAG): Failed to load patch")
        }
    }
}

// MARK: - FlutterStreamHandler
extension AudioProcessingViewController: FlutterStreamHandler {
    func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        // Start listening to events, e.g., from NotificationCenter and forward them to Flutter
        return nil
    }

    func onCancel(withArguments arguments: Any?) -> FlutterError? {
        // Stop listening to events
        return nil
    }
}
