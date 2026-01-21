package com.example.niharika_all_for_one

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import android.view.MotionEvent
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Update_Job_Engineer_Workshop_Activity : AppCompatActivity() {

    // Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Constants
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_CAMERA_PERMISSION = 2001
    private val REQUEST_IMAGE_TEXT_RECOGNITION = 2002
    private val REQUEST_VOICE_ISSUE = 4001
    private val REQUEST_VOICE_SPARE = 4002
    private val REQUEST_RECORD_AUDIO_PERMISSION = 3001

    // Photo handling
    private val localImageMap = mutableMapOf<Int, File>()
    private var currentImageCardId = -1
    private lateinit var photoUri: Uri

    // Audio handling
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String
    private var recordingField = ""

    // OCR
    private var currentOcrTargetField: EditText? = null

    // Job ID
    private var jobId: String = ""

    // UI Elements
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var tvCustomerName: TextView
    private lateinit var btnPurchaseDate: Button
    private lateinit var rbInWarranty: RadioButton
    private lateinit var rbOutOfWarranty: RadioButton
    private lateinit var taIssueDescription: EditText
    private lateinit var taSparePartsDescription: EditText
    private lateinit var dataMake: EditText
    private lateinit var dataSLNo: EditText
    private lateinit var btnUpdateJob: Button
    private lateinit var cbTurningOn: CheckBox
    private lateinit var cbWaterNotPumping: CheckBox
    private lateinit var cbLowPressure: CheckBox
    private lateinit var cbHighPressure: CheckBox
    private lateinit var cbAutoTurnOff: CheckBox
    private lateinit var cbStartTakesTime: CheckBox
    private lateinit var rbRepairNeeded: RadioButton
    private lateinit var rbRepairNotNeeded: RadioButton
    private lateinit var rbEngNeeded: RadioButton
    private lateinit var rbEngNotNeeded: RadioButton
    private lateinit var tvJOBID: TextView

    // Image card IDs
    private val imageCardIds = listOf(
        R.id.cardImg1, R.id.cardImg2, R.id.cardImg3,
        R.id.cardImg4, R.id.cardImg5, R.id.cardImg6,
        R.id.cardImg7, R.id.cardImg8, R.id.cardImg9
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update_job_engineer_workshop)

        // UI Binding

        // Header
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // Job Info
        tvCustomerName = findViewById(R.id.tvCustomerName)
        btnPurchaseDate = findViewById(R.id.btnPurchaseDate)
        tvJOBID = findViewById(R.id.tvJOBID)

        // Warranty Info
        rbInWarranty = findViewById(R.id.rbInWarranty)
        rbOutOfWarranty = findViewById(R.id.rbOutOfWarranty)

        // Inputs
        taIssueDescription = findViewById(R.id.taIssueDescription)
        taSparePartsDescription = findViewById(R.id.taSparePartsDescription)
        dataMake = findViewById(R.id.dataMake)
        dataSLNo = findViewById(R.id.dataSLNo)
        btnUpdateJob = findViewById(R.id.btnUpdateJob)

        // Checkboxes and Radios
        cbTurningOn = findViewById(R.id.checkbox_turning_on)
        cbWaterNotPumping = findViewById(R.id.checkbox_water_not_pumping)
        cbLowPressure = findViewById(R.id.checkbox_low_pressure)
        cbHighPressure = findViewById(R.id.checkbox_high_pressure)
        cbAutoTurnOff = findViewById(R.id.checkbox_auto_turn_off)
        cbStartTakesTime = findViewById(R.id.checkbox_start_takes_time)
        rbRepairNeeded = findViewById(R.id.rbNeeded)
        rbRepairNotNeeded = findViewById(R.id.rbNotNeeded)
        rbEngNeeded = findViewById(R.id.rbEngNeeded)
        rbEngNotNeeded = findViewById(R.id.rbEngNotNeeded)

        // Get Role and Name
        // Show Role + Name
        checkUser()

        // Logout action
//        LogOut.setOnClickListener {
//            auth.signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            auth.signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        // Back button
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Get Job ID from intent
        jobId = intent.getStringExtra("jobId") ?: ""
        if (jobId.isNotEmpty()) {
            tvJOBID.text = "Job ID: $jobId"
            fetchAndFillJobData(jobId)
        }

        // OCR listeners
        setupOCRListeners()

        // Voice input mic setup
        setupVoiceListeners()

        // Approval Triggers
        rbRepairNeeded.setOnClickListener { sendNotificationToRoles("Next-Day Repair Needed", jobId) }
        rbEngNeeded.setOnClickListener { sendNotificationToRoles("Another Engineer Needed", jobId) }

        // Photo card listeners
        imageCardIds.forEachIndexed { index, cardId ->
            findViewById<CardView>(cardId).setOnClickListener {
                currentImageCardId = index + 1
                checkCameraPermissionAndOpen()
            }
        }

        // Final Update logic (images → voice → firestore)
        btnUpdateJob.setOnClickListener {
            uploadPendingImagesAndThen {
                uploadAudioIfAny()
                updateJobToFirestore(jobId)
            }
        }


    }

    // Get role and name from shared prefs and Firestore
//    private fun checkUser() {
//        val phone = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null) ?: return
//        FirebaseFirestore.getInstance().collection("Users").document(phone).get()
//            .addOnSuccessListener {
//                RoleTitle.text = "${it.getString("role")}: ${it.getString("fullName")}"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phone = prefs.getPhone() ?: return

        FirebaseFirestore.getInstance().collection("Users").document(phone).get()
            .addOnSuccessListener {
                RoleTitle.text = "${it.getString("role")}: ${it.getString("fullName")}"
            }
    }

    // OCR field camera icon logic
    private fun setupOCRListeners() {
        dataMake.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataMake.right - dataMake.compoundDrawables[2].bounds.width()) {
                currentOcrTargetField = dataMake
                openCameraForTextRecognition()
                return@setOnTouchListener true
            }
            false
        }

        dataSLNo.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataSLNo.right - dataSLNo.compoundDrawables[2].bounds.width()) {
                currentOcrTargetField = dataSLNo
                openCameraForTextRecognition()
                return@setOnTouchListener true
            }
            false
        }
    }

    // Open camera for OCR (Google Lens)
    private fun openCameraForTextRecognition() {
        val file = File.createTempFile("ocr", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_TEXT_RECOGNITION)
    }

    // Ask camera permission → then open camera
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    // Open default Camera intent for photo capture (cardImgX)
    private fun openCamera() {
        val file = File.createTempFile("cardImg$currentImageCardId", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // Voice mic icon logic
    private fun setupVoiceListeners() {
        taIssueDescription.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= taIssueDescription.right - taIssueDescription.compoundDrawables[2].bounds.width()) {
                startVoiceInput(taIssueDescription, "issue")
                return@setOnTouchListener true
            }
            false
        }

        taSparePartsDescription.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= taSparePartsDescription.right - taSparePartsDescription.compoundDrawables[2].bounds.width()) {
                startVoiceInput(taSparePartsDescription, "spare")
                return@setOnTouchListener true
            }
            false
        }
    }

    // Start voice-to-text + audio recording
    private fun startVoiceInput(target: EditText, fieldType: String) {
        // Check mic permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        recordingField = fieldType
        currentOcrTargetField = target
        startRecordingAudio()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        val reqCode = if (fieldType == "issue") REQUEST_VOICE_ISSUE else REQUEST_VOICE_SPARE
        startActivityForResult(intent, reqCode)
    }

    // Start audio recording
    private fun startRecordingAudio() {
        try {
            val file = File(cacheDir, "${recordingField}_${System.currentTimeMillis()}.mp3")
            audioFilePath = file.absolutePath
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Stop audio and upload to Firebase
    private fun uploadAudioIfAny() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            if (::audioFilePath.isInitialized) {
                val file = File(audioFilePath)
                if (file.exists()) {
                    val uri = Uri.fromFile(file)
                    val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                    val time = SimpleDateFormat("hh-mm a", Locale.getDefault()).format(Date())
                    val roleName = RoleTitle.text.toString().replace(" ", ".")
                    val fileName = "$roleName.$date.$time.$recordingField.mp3"
                    val path = "Jobs/job.$jobId/recordings/$fileName"
                    FirebaseStorage.getInstance().reference.child(path).putFile(uri)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Audio uploaded: $fileName", Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Audio upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    audioFilePath = "" // Clear path after upload to avoid reuse
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle Camera, OCR, Voice Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, photoUri))
                    else MediaStore.Images.Media.getBitmap(contentResolver, photoUri)

                    val file = File(cacheDir, "cardImg$currentImageCardId.jpg")
                    FileOutputStream(file).use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
                    }

                    localImageMap[currentImageCardId] = file
                    showPreviewInCard(Uri.fromFile(file))
                } catch (e: Exception) {
                    Toast.makeText(this, "Image error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_IMAGE_TEXT_RECOGNITION -> {
                val image = InputImage.fromFilePath(this, photoUri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener {
                        val resultText = it.text.trim()
                        currentOcrTargetField?.setText(resultText)
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "OCR failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            REQUEST_VOICE_ISSUE, REQUEST_VOICE_SPARE -> {
                val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                currentOcrTargetField?.setText(spoken)
            }
        }
    }

    // Show image preview in respective card
    private fun showPreviewInCard(uri: Uri) {
        val card = findViewById<CardView>(imageCardIds[currentImageCardId - 1])
        val imgView = (card.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView
        Glide.with(this).load(uri).centerCrop().into(imgView)
    }

    // Upload all images and continue
    private fun uploadPendingImagesAndThen(onComplete: () -> Unit) {
        if (localImageMap.isEmpty()) {
            onComplete()
            return
        }

        val roleName = RoleTitle.text.toString().replace(" ", ".")
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("hh-mm a", Locale.getDefault()).format(Date())
        var remaining = localImageMap.size

        localImageMap.forEach { (index, file) ->
            val fileName = "$roleName.$date.$time.cardImg$index.jpg"
            val path = "Jobs/job.$jobId/images/$fileName"
            val ref = FirebaseStorage.getInstance().reference.child(path)
            val uri = Uri.fromFile(file)

            ref.putFile(uri)
                .addOnSuccessListener {
                    remaining--
                    if (remaining == 0) onComplete()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed (card $index)", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Load data and prefill fields
    private fun fetchAndFillJobData(jobId: String) {
        FirebaseFirestore.getInstance().collection("Jobs").document("job.$jobId").get()
            .addOnSuccessListener { doc ->
                tvCustomerName.text = doc.getString("customerName") ?: ""
                btnPurchaseDate.text = doc.getString("purchaseDate") ?: ""

                rbInWarranty.isChecked = doc.getString("warrantyStatus") == "In Warranty"
                rbOutOfWarranty.isChecked = doc.getString("warrantyStatus") == "Out of Warranty"

                taIssueDescription.setText(doc.getString("description") ?: "")
                taSparePartsDescription.setText(doc.getString("sparePartsUsed") ?: "")
                dataMake.setText(doc.getString("make") ?: "")
                dataSLNo.setText(doc.getString("serialNumber") ?: "")

                val issues = doc.get("issues") as? List<*> ?: emptyList<String>()
                cbTurningOn.isChecked = "Turning on" in issues
                cbWaterNotPumping.isChecked = "Water not pumping" in issues
                cbLowPressure.isChecked = "Low pressure" in issues
                cbHighPressure.isChecked = "High pressure" in issues
                cbAutoTurnOff.isChecked = "Auto turn off" in issues
                cbStartTakesTime.isChecked = "Start takes time" in issues

                rbRepairNeeded.isChecked = doc.getString("nextDayRepair") == "Needed"
                rbRepairNotNeeded.isChecked = doc.getString("nextDayRepair") == "Not-Needed"
                rbEngNeeded.isChecked = doc.getString("needAnotherEngineer") == "Needed"
                rbEngNotNeeded.isChecked = doc.getString("needAnotherEngineer") == "Not-Needed"
            }
    }

    // Save job update to Firestore (separate doc)
    private fun updateJobToFirestore(jobId: String) {
        val db = FirebaseFirestore.getInstance()
        val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("hh-mm a", Locale.getDefault()).format(Date())
        val roleName = RoleTitle.text.toString().replace(" ", ".")
        val docName = "$roleName.$date.$time"

        val issues = mutableListOf<String>().apply {
            if (cbTurningOn.isChecked) add("Turning on")
            if (cbWaterNotPumping.isChecked) add("Water not pumping")
            if (cbLowPressure.isChecked) add("Low pressure")
            if (cbHighPressure.isChecked) add("High pressure")
            if (cbAutoTurnOff.isChecked) add("Auto turn off")
            if (cbStartTakesTime.isChecked) add("Start takes time")
        }

        val updateData = hashMapOf(
            "description" to taIssueDescription.text.toString(),
            "make" to dataMake.text.toString(),
            "serialNumber" to dataSLNo.text.toString(),
            "sparePartsUsed" to taSparePartsDescription.text.toString(),
            "nextDayRepair" to if (rbRepairNeeded.isChecked) "Needed" else "Not-Needed",
            "needAnotherEngineer" to if (rbEngNeeded.isChecked) "Needed" else "Not-Needed",
            "issues" to issues,
            "roleName" to roleName,
            "timestamp" to "$date $time"
        )

        db.collection("Jobs")
            .document("job.$jobId")
            .collection("updates")
            .document(docName)
            .set(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Job update saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Notify Managers/Admins when Needed is selected
    private fun sendNotificationToRoles(message: String, jobId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Users")
            .whereIn("role", listOf("Manager", "Admin", "Super Admin", "Owner"))
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    val token = doc.getString("fcmToken") ?: return@forEach
                    sendPushNotification(token, message, jobId)
                }
            }
    }

    // 🔔 Simulated FCM push (print to log)
    private fun sendPushNotification(token: String, message: String, jobId: String) {
        Log.d("FCM", "Mock send to $token: [$message] for job $jobId")
    }
}
