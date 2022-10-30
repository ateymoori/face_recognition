package org.tensorflow.lite.examples.detection.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.tensorflow.lite.examples.detection.databinding.ActivityVoiceBinding
import org.tensorflow.lite.examples.detection.log
import java.util.*


class VoiceActivity : AppCompatActivity(), RecognitionListener {
    private lateinit var recognizerIntent: Intent
    private lateinit var speechRecognizer: SpeechRecognizer
    private var lastDetectedVoice = ""
    private var lastPlayedSound: String? = ""

    lateinit var views: ActivityVoiceBinding
    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(
            this
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine.language = Locale.UK
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityVoiceBinding.inflate(layoutInflater)
        setContentView(views.root)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            checkPermission()
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        //init the voice
        speak("")

        startListening()

    }

    private fun checkPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1000)
    }

    fun startListening() {
        // if (!::recognizerIntent.isInitialized) {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        speechRecognizer.setRecognitionListener(this)
        //  }
        speechRecognizer.startListening(recognizerIntent)

    }

    fun stopListening() {
        speechRecognizer.stopListening()
    }

    override fun onResults(results: Bundle?) {
        startListening()
        "".log(" test_voice onResults")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null) {
            val text = matches.get(0)
            lastDetectedVoice = text
            text.log("test_voice partialResults")
        }

    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        "onEvent $eventType".log("test_voice ")
    }

    override fun onReadyForSpeech(params: Bundle?) {
    }

    override fun onBeginningOfSpeech() {
    }

    override fun onRmsChanged(rmsdB: Float) {
    }

    override fun onBufferReceived(buffer: ByteArray?) {
    }

    override fun onEndOfSpeech() {
        "onEndOfSpeech".log("test_voice")
        detectAndAction(lastDetectedVoice)
    }

    override fun onError(error: Int) {
        "onError".log("test_voice")
        startListening()
    }

    fun muteAudio() {
        val amanager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
        amanager.setStreamMute(AudioManager.STREAM_ALARM, true)
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, true)
        amanager.setStreamMute(AudioManager.STREAM_RING, true)
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
    }

    fun unMuteAudio() {
        val amanager = getSystemService(AUDIO_SERVICE) as AudioManager
        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
        amanager.setStreamMute(AudioManager.STREAM_ALARM, false)
        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false)
        amanager.setStreamMute(AudioManager.STREAM_RING, false)
        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
    }

    var userWantsToAskSomething = false
    fun detectAndAction(text: String) {

        if (text.isEmpty()) return

        if (text.toLowerCase().contains("hey kitty")) {
            lastDetectedVoice = ""
            userWantsToAskSomething = true
            speak("Hey Amir, What can I do for you?")

        } else {
            //handle the question/order
            "lastPlayedSound : ${lastPlayedSound?.toLowerCase()}".log("monitor_")
            "lastDetectedVoice : ${lastDetectedVoice?.toLowerCase()}".log("monitor_")
            if (userWantsToAskSomething && (lastPlayedSound?.toLowerCase()?.contains(
                    lastDetectedVoice.toLowerCase()
                ) == false)
            ) {
                views.questionTv.text = lastDetectedVoice
                userWantsToAskSomething = false
                speak("I dont know")
            }


        }

        views.resultsTv.text = text

    }

    fun speak(text: String?) {
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")

        textToSpeechEngine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                lastPlayedSound = text
                "onStart $text  ".log("speak_")
                unMuteAudio()
                stopListening()
            }

            override fun onDone(utteranceId: String?) {
                "onDone".log("speak_")
                muteAudio()
                startListening()
            }

            override fun onError(utteranceId: String?) {
                "onError".log("speak_")
                muteAudio()
                startListening()
            }
        })
    }
}