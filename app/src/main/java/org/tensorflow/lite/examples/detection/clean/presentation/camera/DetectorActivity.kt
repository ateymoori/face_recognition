package org.tensorflow.lite.examples.detection.clean.presentation.camera

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCharacteristics
import android.media.AudioManager
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.detection.R
import org.tensorflow.lite.examples.detection.clean.data.models.MemberModel
import org.tensorflow.lite.examples.detection.clean.data.utils.log
import org.tensorflow.lite.examples.detection.customview.OverlayView
import org.tensorflow.lite.examples.detection.env.BorderedText
import org.tensorflow.lite.examples.detection.env.ImageUtils
import org.tensorflow.lite.examples.detection.env.Logger
import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier
import org.tensorflow.lite.examples.detection.tflite.SimilarityClassifier.Recognition
import org.tensorflow.lite.examples.detection.tflite.TFLiteObjectDetectionAPIModel
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker
import java.io.IOException
import java.io.OutputStream
import java.util.*

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@AndroidEntryPoint
open class DetectorActivity : CameraActivity(), OnImageAvailableListener, RecognitionListener {

    private lateinit var detectedUser: MemberModel
    val viewModel: CameraViewModel by viewModels()

    var trackingOverlay: OverlayView? = null
    private var sensorOrientation: Int? = null
    private var detector: SimilarityClassifier? = null
    private var lastProcessingTimeMs: Long = 0
    private var rgbFrameBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var cropCopyBitmap: Bitmap? = null
    private var computingDetection = false
    private var addPending = false

    //private boolean adding = false;
    private var timestamp: Long = 0
    private var frameToCropTransform: Matrix? = null
    private var cropToFrameTransform: Matrix? = null

    //private Matrix cropToPortraitTransform;
    private var tracker: MultiBoxTracker? = null
    private var borderedText: BorderedText? = null

    // Face detector
    private var faceDetector: FaceDetector? = null

    // here the preview image is drawn in portrait way
    private var portraitBmp: Bitmap? = null

    // here the face is cropped and drawn
    private var faceBmp: Bitmap? = null

//    private var detectedFaceVU: AppCompatImageView? = null
//    private var dataVU: TextView? = null

    //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.fab_add).setOnClickListener(View.OnClickListener { onAddClick() })
        findViewById<View>(R.id.fab_clear).setOnClickListener(View.OnClickListener { detector?.clearData() })

        views.fabVoice.setOnClickListener {
            viewModel.conversation("Who is Sweden prime minister?")
            //   startActivity(Intent(this, VoiceActivity::class.java))
        }

//        detectedFaceVU = findViewById<View>(R.id.detectedFaceVU) as AppCompatImageView
//        dataVU = findViewById<View>(R.id.dataVU) as TextView

        // Real-time contour detection of multiple faces
        val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).build()
        val detector = FaceDetection.getClient(options)
        faceDetector = detector

        init()
        //checkWritePermission();

        lifecycleScope.launch {
            viewModel._member.collect { it ->
                "${it?.user_name.toString()}".log("debug_face detectro activity")
                it?.let {
                    detectedUser = it
                    views.userAvatar.setImageBitmap(it.face)
                    views.userName.setText(it.user_name)
                }
            }

        }

        lifecycleScope.launch {
            viewModel._conversation.collect {
                views.resultsTv.text = it?.second_answer
                speak(it?.second_answer)
            }
        }

        initConnection()

    }

    private fun onAddClick() {
        addPending = true
        //Toast.makeText(this, "click", Toast.LENGTH_LONG ).show();
    }

    public override fun onPreviewSizeChosen(size: Size?, rotation: Int) {
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
        )
        borderedText = BorderedText(textSizePx)
        borderedText!!.setTypeface(Typeface.MONOSPACE)
        tracker = MultiBoxTracker(this)
        try {
            detector = TFLiteObjectDetectionAPIModel.create(
                assets, TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE, TF_OD_API_IS_QUANTIZED
            )
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (e: IOException) {
            e.printStackTrace()
            LOGGER.e(e, "Exception initializing classifier!")
            val toast = Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
        previewWidth = size!!.width
        previewHeight = size.height
        sensorOrientation = rotation - screenOrientation
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation)
        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight)
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
        val targetW: Int
        val targetH: Int
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth
            targetW = previewHeight
        } else {
            targetW = previewWidth
            targetH = previewHeight
        }
        val cropW = (targetW / 2.0).toInt()
        val cropH = (targetH / 2.0).toInt()
        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
        portraitBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888)
        frameToCropTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight, cropW, cropH, sensorOrientation!!, MAINTAIN_ASPECT
        )

//    frameToCropTransform =
//            ImageUtils.getTransformationMatrix(
//                    previewWidth, previewHeight,
//                    previewWidth, previewHeight,
//                    sensorOrientation, MAINTAIN_ASPECT);
        cropToFrameTransform = Matrix()
        frameToCropTransform!!.invert(cropToFrameTransform)
        val frameToPortraitTransform = ImageUtils.getTransformationMatrix(
            previewWidth, previewHeight, targetW, targetH, sensorOrientation!!, MAINTAIN_ASPECT
        )
        trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
        trackingOverlay!!.addCallback { canvas ->
            tracker!!.draw(canvas)
            if (isDebug) {
                tracker!!.drawDebug(canvas)
            }
        }
        tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation!!)
    }

    override fun processImage() {
        ++timestamp
        val currTimestamp = timestamp
        trackingOverlay!!.postInvalidate()

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage()
            return
        }
        computingDetection = true
        LOGGER.i("Preparing image $currTimestamp for detection in bg thread.")
        rgbFrameBitmap!!.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight)
        readyForNextImage()
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap)
        }
        val image = InputImage.fromBitmap(croppedBitmap!!, 0)
        faceDetector?.process(image)?.addOnSuccessListener(OnSuccessListener { faces ->
            if (faces.size == 0) {
                updateResults(currTimestamp, LinkedList())
                return@OnSuccessListener
            }
            runInBackground {
                onFacesDetected(currTimestamp, faces, addPending)
                addPending = false
            }
        })
    }

    override val layoutId: Int
        get() = R.layout.tfe_od_camera_connection_fragment_tracking
    override val desiredPreviewFrameSize: Size
        get() = Size(640, 480)

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum class DetectorMode {
        TF_OD_API
    }

    override fun setUseNNAPI(isChecked: Boolean) {
        runInBackground { detector!!.setUseNNAPI(isChecked) }
    }

    override fun setNumThreads(numThreads: Int) {
        runInBackground { detector!!.setNumThreads(numThreads) }
    }

    // Face Processing
    private fun createTransform(
        srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int, applyRotation: Int
    ): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation)
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)

            // Rotate around origin.
            matrix.postRotate(applyRotation.toFloat())
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;
        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }

    private fun showAddFaceDialog(rec: Recognition) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.image_edit_dialog, null)
        val ivFace = dialogLayout.findViewById<ImageView>(R.id.dlg_image)
        val tvTitle = dialogLayout.findViewById<TextView>(R.id.dlg_title)
        val etName = dialogLayout.findViewById<EditText>(R.id.dlg_input)
        tvTitle.text = "Add Face"
        ivFace.setImageBitmap(rec.crop)
        etName.hint = "Input name"
        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dlg, i ->
            val name = etName.text.toString()
            if (name.isEmpty()) {
                return@OnClickListener
            }

            viewModel.syncUser(
                MemberModel(
                    id = rec.id?.toInt() ?: 0,
                    face = rec.crop,
                    user_name = name,
                    last_mood = "",
                    last_conversation = "",
                    uuid = UUID.randomUUID().toString()
                )
            )
            detector!!.register(name, rec)
            dlg.dismiss()
        })
        builder.setView(dialogLayout)
        builder.show()
    }

    private fun updateResults(currTimestamp: Long, mappedRecognitions: List<Recognition>) {
        tracker!!.trackResults(mappedRecognitions, currTimestamp)
        trackingOverlay!!.postInvalidate()
        computingDetection = false
        //adding = false;
        if (mappedRecognitions.size > 0) {
            LOGGER.i("Adding results")
            val rec = mappedRecognitions[0]
            if (rec.extra != null) {
                showAddFaceDialog(rec)
            }
        }
    }

    private fun onFacesDetected(currTimestamp: Long, faces: List<Face>, add: Boolean) {
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap!!)
        // val canvas = Canvas(cropCopyBitmap)
        val paint = Paint()
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.0f
        var minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API
        minimumConfidence = when (MODE) {
            DetectorMode.TF_OD_API -> MINIMUM_CONFIDENCE_TF_OD_API
        }
        val mappedRecognitions: MutableList<Recognition> = LinkedList()

        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        val sourceW = rgbFrameBitmap!!.width
        val sourceH = rgbFrameBitmap!!.height
        val targetW = portraitBmp!!.width
        val targetH = portraitBmp!!.height
        val transform = createTransform(
            sourceW, sourceH, targetW, targetH, sensorOrientation!!
        )
        val cv = Canvas(portraitBmp!!)

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap!!, transform, null)
        val cvFace = Canvas(faceBmp!!)
        val saved = false
        for (face in faces) {
            LOGGER.i("FACE$face")
            LOGGER.i("Running detection on face $currTimestamp")
            //results = detector.recognizeImage(croppedBitmap);
            val boundingBox = RectF(face.boundingBox)

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
            val goodConfidence = true //face.get;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                cropToFrameTransform!!.mapRect(boundingBox)

                // maps original coordinates to portrait coordinates
                val faceBB = RectF(boundingBox)
                transform.mapRect(faceBB)

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                val sx = TF_OD_API_INPUT_SIZE.toFloat() / faceBB.width()
                val sy = TF_OD_API_INPUT_SIZE.toFloat() / faceBB.height()
                val matrix = Matrix()
                matrix.postTranslate(-faceBB.left, -faceBB.top)
                matrix.postScale(sx, sy)
                cvFace.drawBitmap(portraitBmp!!, matrix, null)

                //canvas.drawRect(faceBB, paint);
                var label = ""
                var confidence = -1f
                var color = Color.BLUE
                var extra: Any? = null
                var crop: Bitmap? = null
                if (add) {
                    crop = Bitmap.createBitmap(
                        portraitBmp!!,
                        faceBB.left.toInt(),
                        faceBB.top.toInt(),
                        faceBB.width().toInt(),
                        faceBB.height().toInt()
                    )
                }

                "left: ${faceBB.left}: ,,, right:  ${faceBB.right}".log("facebb")

                if (faceBB.left.toInt() > 0 && movingType == MovingType.FRONT) {
                    val leftRight = when {
                        faceBB.left.toInt() in 100..200 -> {
                            front()
                            "Middle"
                        }
                        faceBB.left > 200 -> {
                            right()
                            "Left"
                        }
                        else -> {
                            left()
                            "Right"
                        }
                    }
                    runOnUiThread {
                        views.tvDirection.text = leftRight
                    }
                }
//                val leftRight = if (faceBB.left > 100) {
//                    "Left"
//                } else {
//                    "right"
//                }

                //left center : L = 190  R : 380
                //right center : L = 70  R: 250 to right : R ddecreasing
                //Center = L between 180 to 40

                val startTime = SystemClock.uptimeMillis()
                val resultsAux = detector!!.recognizeImage(faceBmp, add)
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime
                if (resultsAux!!.size > 0) {
                    val result = resultsAux[0]
                    extra = result!!.extra
                    //          Object extra = result.getExtra();
//          if (extra != null) {
//            LOGGER.i("embeeding retrieved " + extra.toString());
//          }
                    val conf = result.distance!!
                    if (conf < 1.0f) {
                        confidence = conf
                        label = result.title ?: "titlee"

                        label.log("face_detected 1")
                        viewModel.faceDetected(
                            label, faceBmp
                        )

                        color = if (result.id == "0") {
                            Color.GREEN
                        } else {
                            Color.RED
                        }
                    }
                }
                if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    val flip = Matrix()
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1f, -1f, previewWidth / 2.0f, previewHeight / 2.0f)
                    } else {
                        flip.postScale(-1f, 1f, previewWidth / 2.0f, previewHeight / 2.0f)
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox)
                }
                val result = Recognition(
                    "0", label, confidence, boundingBox
                )
                result.color = color
                result.setLocation(boundingBox)
                result.extra = extra
                result.crop = crop
                mappedRecognitions.add(result)

//                mappedRecognitions.forEach {
//                    it.
//                    it.toString().log("extra__")
//                }
//                runOnUiThread {
//                    detectedFaceVU?.setImageBitmap(faceBmp)
//                    dataVU?.text = label
//
//                    //    face.smilingProbability
//                    //      result.id.toString().toString().log("extra__")
//
//                }

            }
        }

        //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }
        updateResults(currTimestamp, mappedRecognitions)
    }

    companion object {
        private val LOGGER = Logger()

        // FaceNet
        //  private static final int TF_OD_API_INPUT_SIZE = 160;
        //  private static final boolean TF_OD_API_IS_QUANTIZED = false;
        //  private static final String TF_OD_API_MODEL_FILE = "facenet.tflite";
        //  //private static final String TF_OD_API_MODEL_FILE = "facenet_hiroki.tflite";
        // MobileFaceNet
        private const val TF_OD_API_INPUT_SIZE = 112
        private const val TF_OD_API_IS_QUANTIZED = false
        private const val TF_OD_API_MODEL_FILE = "mobile_face_net.tflite"
        private const val TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt"
        private val MODE = DetectorMode.TF_OD_API

        // Minimum detection confidence to track a detection.
        private const val MINIMUM_CONFIDENCE_TF_OD_API = 0.5f
        private const val MAINTAIN_ASPECT = false
        protected val desiredPreviewFrameSize = Size(640, 480)
        //   protected get() = {  }

        //private static final int CROP_SIZE = 320;
        //private static final Size CROP_SIZE = new Size(320, 320);
        private const val SAVE_PREVIEW_BITMAP = false
        private const val TEXT_SIZE_DIP = 10f
    }

    ///////////VOICE
    private lateinit var recognizerIntent: Intent
    private lateinit var speechRecognizer: SpeechRecognizer
    private var lastDetectedVoice = ""
    private var lastPlayedSound: String? = ""

    fun init() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.RECORD_AUDIO
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

    private val textToSpeechEngine: TextToSpeech by lazy {
        TextToSpeech(
            this
        ) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine.language = Locale.UK
            }
        }
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

    enum class MovingType {
        BACK, LEFT, RIGHT, FRONT, STOP
    }

    var movingType: MovingType = MovingType.STOP
    fun detectAndAction(text: String) {

        if (text.isEmpty()) return

        var command = text.lowercase()
        command.log("blueeeeee")
        when {
            command.contains("hey") -> {
                lastDetectedVoice = ""
                userWantsToAskSomething = true
                if (::detectedUser.isInitialized) {
                    speak("Hey ${detectedUser.user_name}, What can I do for you?")
                }
            }
            command.contains("follow") -> {
                movingType = MovingType.FRONT
                front()
                speak("Ok ${detectedUser.user_name}, I am coming")
            }
            command.contains("fr") -> {
                movingType = MovingType.FRONT
                front()
            }
            command.contains("ba") -> {
                movingType = MovingType.BACK
                back()
            }
            command.contains("le") -> {
                movingType = MovingType.LEFT
                left()
            }
            command.contains("st") -> {
                movingType = MovingType.STOP
                stop()
            }
            command.contains("ri") -> {
                movingType = MovingType.RIGHT
                right()
            }
            else -> {
                if (userWantsToAskSomething && (lastPlayedSound?.lowercase()?.contains(
                        lastDetectedVoice.lowercase()
                    ) == false)
                ) {
                    views.questionTv.text = lastDetectedVoice
                    viewModel.conversation(lastDetectedVoice)
                    userWantsToAskSomething = false
                    speak("Ok ${detectedUser.user_name}, I am moving")
//                speak("I dont know")
                }
            }
        }

//        if (text.lowercase().contains("hey kitty")) {
//            lastDetectedVoice = ""
//            userWantsToAskSomething = true
//            if (::detectedUser.isInitialized) {
//                speak("Hey ${detectedUser.user_name}, What can I do for you?")
//            }
//
//        } else {
//            //handle the question/order
//            "lastPlayedSound : ${lastPlayedSound?.lowercase()}".log("monitor_")
//            "lastDetectedVoice : ${lastDetectedVoice.lowercase()}".log("monitor_")
//            if (userWantsToAskSomething && (lastPlayedSound?.lowercase()?.contains(
//                    lastDetectedVoice.lowercase()
//                ) == false)
//            ) {
//                views.questionTv.text = lastDetectedVoice
//                viewModel.conversation(lastDetectedVoice)
//                userWantsToAskSomething = false
//                speak("Ok ${detectedUser.user_name}, I am moving")
////                speak("I dont know")
//            }
//
//        }

        views.resultsTv.text = text

    }

    var lastWord: String? = ""
    fun speak(text: String?) {
        if (lastWord == text)
            return
        textToSpeechEngine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
        lastWord = text

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

    fun sendCommandToGoogleAssistant(command: String) {
        val command = "navigate home by public transport"
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.setClassName(
            "com.google.android.googlequicksearchbox",
            "com.google.android.googlequicksearchbox.SearchActivity"
        )
        intent.putExtra(command, command)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK //necessary if launching from Service

        startActivity(intent)
    }

    private val TAG = "BluetoothExample"
    private val DEVICE_NAME = "HC-05"
    private val DEVICE_ADDRESS = "98:D3:61:F6:A6:69"
    private val PIN_CODE = "1234"
    private val BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream

    fun front() {
        sendBluetoothData("F")
    }

    fun back() {
        sendBluetoothData("B")
    }

    fun left() {
        sendBluetoothData("L")
    }

    fun right() {
        sendBluetoothData("R")
    }

    fun stop() {
        sendBluetoothData("S")
    }

    fun initConnection() {

//        searchDevices()

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.d(TAG, "Device doesn't support Bluetooth")
            return
        }

        val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS)

        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID)
            bluetoothSocket.connect()

            // Provide the PIN code
            device.setPin(PIN_CODE.toByteArray())
            device.createBond()

            outputStream = bluetoothSocket.outputStream
            Log.d(TAG, "Bluetooth connection established.")

            // Send your desired text commands here
//            sendBluetoothData("F")

        } catch (e: IOException) {
            Log.d(TAG, "Error occurred during Bluetooth connection", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyConnection()
    }

    fun destroyConnection() {
        try {
            outputStream.close()
            bluetoothSocket.close()
        } catch (e: IOException) {
            Log.e("blueeeeee", "Error occurred while closing Bluetooth connection", e)
        }
    }

    private fun sendBluetoothData(data: String) {
        if (::outputStream.isInitialized)
            try {
                outputStream.write(data.toByteArray())
                Log.d("blueeeeee", "Data sent: $data")
            } catch (e: IOException) {
                Log.d("blueeeeee", "Error occurred while sending data", e)
            }
    }

    fun searchDevices() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            Log.d(TAG, "Device doesn't support Bluetooth")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is not enabled. Please enable Bluetooth and try again.")
            return
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
                val deviceName = device.name
                val deviceAddress = device.address
                val deviceUuids = device.uuids

                Log.d(TAG, "Device Name: $deviceName")
                Log.d(TAG, "MAC Address: $deviceAddress")

                if (deviceUuids != null) {
                    for (uuid in deviceUuids) {
                        Log.d(TAG, "UUID: $uuid")
                    }
                }
                Log.d(TAG, "---------------------------")
            }
        } else {
            Log.d(TAG, "No paired devices found.")
        }
    }
}