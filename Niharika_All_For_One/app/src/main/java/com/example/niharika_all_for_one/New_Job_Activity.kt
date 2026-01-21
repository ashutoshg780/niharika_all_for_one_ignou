package com.example.niharika_all_for_one

import android.Manifest
import android.app.DatePickerDialog
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
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
import android.app.Activity
import com.example.niharika_all_for_one.network.AppPreferences

class New_Job_Activity : AppCompatActivity() {

    // Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Camera + OCR + Voice Constants
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_CAMERA_PERMISSION = 1002
    private val REQUEST_IMAGE_TEXT_RECOGNITION = 1003
    private val REQUEST_VOICE_COMPLAINT = 1004
    private val REQUEST_VOICE_ISSUE = 1005
    private val REQUEST_RECORD_AUDIO_PERMISSION = 1006

    // Voice Recording
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String
    private var recordingField = "" // complaint / issue
    private var currentVoiceTargetField: EditText? = null

    // Photo Handling
    private var currentImageCardId = -1
    private lateinit var photoUri: Uri
    private val localImageMap = mutableMapOf<Int, File>()
    private val imageCardIds = listOf(
        R.id.cardImg1, R.id.cardImg2, R.id.cardImg3,
        R.id.cardImg4, R.id.cardImg5, R.id.cardImg6
    )

    // OCR
    private var currentOcrTargetField: EditText? = null

    // Engineer Info
    private var selectedEngineer: String = ""
    private var currentUserName = ""
    private var currentUserRole = ""

    // UI Components
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var etCustomerName: EditText
    private lateinit var etContractNo: EditText
    private lateinit var btnPurchaseDate: Button
    private lateinit var dataMake: EditText
    private lateinit var dataSLNo: EditText
    private lateinit var editComplaint: EditText
    private lateinit var taIssueDescription: EditText
    private lateinit var btnCreateJob: Button
    private lateinit var tvEngineerName: TextView

    // RadioGroups
    private lateinit var radioWarrantyStatus: RadioGroup
    private lateinit var radioComplaintMethod: RadioGroup
    private lateinit var radioComplaintType: RadioGroup
    private lateinit var radioCustomerType: RadioGroup

    // RadioButtons
    private lateinit var radioOnline: RadioButton
    private lateinit var radioByMobileCall: RadioButton
    private lateinit var radioWalkIn: RadioButton
    private lateinit var radioOther: RadioButton
    private lateinit var rbField: RadioButton
    private lateinit var rbWorkshop: RadioButton

    // CheckBoxes
    private lateinit var cbTurningOn: CheckBox
    private lateinit var cbWaterNotPumping: CheckBox
    private lateinit var cbLowPressure: CheckBox
    private lateinit var cbHighPressure: CheckBox
    private lateinit var cbAutoTurnOff: CheckBox
    private lateinit var cbStartTakesTime: CheckBox

    // ✅ Add this to your constants section near the top
    private val REQUEST_RECORD_AUDIO = 3005  // 🔊 Used for mic recording permission

    // Voice File Path List (delayed upload)
    private var voiceFilePathMap = mutableMapOf<String, String>() // issue/complaint → local path

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_job)

        // ==== Header ====
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // ==== Input Fields ====
        etCustomerName = findViewById(R.id.etCustomerName)
        etContractNo = findViewById(R.id.etContractNo)
        btnPurchaseDate = findViewById(R.id.btnPurchaseDate)
        dataMake = findViewById(R.id.dataMake)
        dataSLNo = findViewById(R.id.dataSLNo)
        editComplaint = findViewById(R.id.editComplaint)
        taIssueDescription = findViewById(R.id.taIssueDescription)
        btnCreateJob = findViewById(R.id.btnCreateJob)
        tvEngineerName = findViewById(R.id.tvEngineerName)

        // ==== Radio Groups ====
        radioWarrantyStatus = findViewById(R.id.radioWarrantyStatus)
        radioComplaintMethod = findViewById(R.id.radioComplaintMethod)
        radioComplaintType = findViewById(R.id.radioComplaintType)
        radioCustomerType = findViewById(R.id.radioCustomerType)

        // ==== Radio Buttons ====
        radioOnline = findViewById(R.id.radioOnline)
        radioByMobileCall = findViewById(R.id.radioByMobileCall)
        radioWalkIn = findViewById(R.id.radioWalkIn)
        radioOther = findViewById(R.id.radioOther)
        rbField = findViewById(R.id.rbField)
        rbWorkshop = findViewById(R.id.rbWorkshop)

        // ==== CheckBoxes ====
        cbTurningOn = findViewById(R.id.checkbox_turning_on)
        cbWaterNotPumping = findViewById(R.id.checkbox_water_not_pumping)
        cbLowPressure = findViewById(R.id.checkbox_low_pressure)
        cbHighPressure = findViewById(R.id.checkbox_high_pressure)
        cbAutoTurnOff = findViewById(R.id.checkbox_auto_turn_off)
        cbStartTakesTime = findViewById(R.id.checkbox_start_takes_time)

        // ==== Load Current User Role/Name ====
        checkUser()

        // ==== Logout ====
//        LogOut.setOnClickListener {
//            auth.signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)  // Use AppPreferences
            prefs.clearPreferences()
            auth.signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        // ==== Back Button ====
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // ==== Purchase Date Picker ====
        btnPurchaseDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this, { _, year, month, day ->
                    val picked = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    val formatted =
                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(picked.time)
                    btnPurchaseDate.text = formatted
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ==== Complaint Method Radio Selection ====
        val methodRadios = listOf(radioOnline, radioByMobileCall, radioWalkIn, radioOther)
        methodRadios.forEach { radio ->
            radio.setOnClickListener {
                methodRadios.forEach { it.isChecked = false }
                radio.isChecked = true
                editComplaint.isEnabled = (radio == radioOther)
                if (!editComplaint.isEnabled) editComplaint.setText("")
            }
        }

        // ==== Engineer Selection ====
        rbField.setOnClickListener { showEngineerDialog("Engineer (Field)") }
        rbWorkshop.setOnClickListener { showEngineerDialog("Engineer (Workshop)") }

        // ==== Photo Cards Click Listeners ====
        imageCardIds.forEachIndexed { index, cardId ->
            findViewById<CardView>(cardId).setOnClickListener {
                currentImageCardId = index + 1
                checkCameraPermissionAndOpen()
            }
        }

        // ==== OCR Camera Taps for Make / SL No ====
        dataMake.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataMake.right - dataMake.compoundDrawables[2].bounds.width()
            ) {
                currentOcrTargetField = dataMake
                openCameraForTextRecognition()
                true
            } else false
        }

        dataSLNo.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataSLNo.right - dataSLNo.compoundDrawables[2].bounds.width()
            ) {
                currentOcrTargetField = dataSLNo
                openCameraForTextRecognition()
                true
            } else false
        }

        // ==== Voice Input for Complaint and Issue Description ====
        editComplaint.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= editComplaint.right - editComplaint.compoundDrawables[2].bounds.width()
            ) {
                startVoiceInput(editComplaint, "complaint")
                true
            } else false
        }

        taIssueDescription.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= taIssueDescription.right - taIssueDescription.compoundDrawables[2].bounds.width()
            ) {
                startVoiceInput(taIssueDescription, "issue")
                true
            } else false
        }

        // ==== Create Job Button ====
        btnCreateJob.setOnClickListener {
            validateAndCreate()
        }
    }

    // === 🧠 Fetch Role & Name and Display in Header ===
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null) ?: return
//        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
//            .addOnSuccessListener {
//                currentUserName = it.getString("fullName") ?: ""
//                currentUserRole = it.getString("role") ?: ""
//                RoleTitle.text = "$currentUserRole: $currentUserName"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)  // Use AppPreferences
        val phoneNumber = prefs.getPhone() ?: return

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
            .addOnSuccessListener {
                currentUserName = it.getString("fullName") ?: ""
                currentUserRole = it.getString("role") ?: ""
                RoleTitle.text = "$currentUserRole: $currentUserName"
            }
    }



    // === 🧑‍🔧 Show Dialog to Select Engineer by Role (Field / Workshop) ===
    private fun showEngineerDialog(role: String) {
        FirebaseFirestore.getInstance().collection("Users")
            .whereEqualTo("role", role)
            .get()
            .addOnSuccessListener { snapshot ->
                val names = snapshot.documents.mapNotNull { it.getString("fullName") }
                if (names.isEmpty()) {
                    Toast.makeText(this, "No $role found!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                AlertDialog.Builder(this)
                    .setTitle("Select $role")
                    .setItems(names.toTypedArray()) { _, index ->
                        selectedEngineer = names[index]
                        tvEngineerName.text = "Assigned to: $selectedEngineer"
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching engineers: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // === 📷 Ask Camera Permission and then open camera ===
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            openCamera()
        }
    }

    // === 📷 Open Camera to capture photo for image card ===
    private fun openCamera() {
        val file = File.createTempFile("cardImg$currentImageCardId", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // === 🤖 Open Camera for ML Kit OCR (Make / SL No) ===
    private fun openCameraForTextRecognition() {
        val file = File.createTempFile("ocr_temp", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_TEXT_RECOGNITION)
    }

    // === 🖼️ Show captured image preview inside selected CardView ===
    private fun showPreviewInCard(imageUri: Uri) {
        try {
            val cardView = findViewById<CardView>(imageCardIds[currentImageCardId - 1])
            val imageView = (cardView.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView
            Glide.with(this).load(imageUri).centerCrop().into(imageView)
        } catch (e: Exception) {
            Toast.makeText(this, "Preview error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Start voice typing + audio recording
    private fun startVoiceInput(targetField: EditText, fieldType: String) {
        // Check audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
            return
        }

        // Store which field and type
        recordingField = fieldType
        currentVoiceTargetField = targetField
        startRecordingAudio()

        // Launch Android's voice typing intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())

        val requestCode = if (fieldType == "complaint") REQUEST_VOICE_COMPLAINT else REQUEST_VOICE_ISSUE
        startActivityForResult(intent, requestCode)
    }

    // Start audio recording to local file (upload later after Create Job)
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

    // Handle result of camera / OCR / voice input
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
                        currentOcrTargetField?.setText(it.text.trim())
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "OCR failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }

            REQUEST_VOICE_COMPLAINT, REQUEST_VOICE_ISSUE -> {
                val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                currentVoiceTargetField?.setText(spoken)

                try {
                    mediaRecorder?.apply {
                        stop()
                        release()
                    }
                    mediaRecorder = null
                } catch (e: Exception) {
                    Log.e("AudioStop", "Error stopping recording: ${e.message}")
                }
            }
        }
    }

    // Validate all fields before creating the job
    private fun validateAndCreate() {
        val missing = mutableListOf<String>()

        // Basic validation
        if (etCustomerName.text.isBlank()) missing.add("Customer Name")
        if (etContractNo.text.isBlank()) missing.add("Contract No")
        if (dataMake.text.isBlank()) missing.add("Make")
        if (dataSLNo.text.isBlank()) missing.add("SL No")
        if (btnPurchaseDate.text == "01-01-1974") missing.add("Purchase Date")
        if (taIssueDescription.text.isBlank()) missing.add("Issue Description")
        if (radioWarrantyStatus.checkedRadioButtonId == -1) missing.add("Warranty Status")
        if (radioComplaintType.checkedRadioButtonId == -1) missing.add("Complaint Type")
        if (radioCustomerType.checkedRadioButtonId == -1) missing.add("Customer Type")
        if (selectedEngineer.isBlank()) missing.add("Assigned Engineer")

        val complaintRadios = listOf(radioOnline, radioByMobileCall, radioWalkIn, radioOther)
        if (complaintRadios.none { it.isChecked }) missing.add("Complaint Method")

        if (missing.isNotEmpty()) {
            Toast.makeText(this, "Missing: ${missing.joinToString()}", Toast.LENGTH_LONG).show()
            return
        }

        // Generate IDs and upload
        getNextCounter("jobCounter", "JID") { jobNum ->
            getNextCounter("customerCounter", "CID") { custNum ->
                val monthYear = SimpleDateFormat("MMyy", Locale.getDefault()).format(Date())
                val finalJobId = "$jobNum$monthYear"
                val finalCustomerId = "$custNum$monthYear"

                stopAndUploadImages(finalJobId) {
                    stopAndUploadAudio(finalJobId) {
                        createJob(finalJobId, finalCustomerId)
                    }
                }
            }
        }
    }

    // Upload audio (after Create Job is clicked)
    private fun stopAndUploadAudio(jobId: String, onComplete: () -> Unit) {
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

                    val ref = FirebaseStorage.getInstance().reference.child(path)
                    ref.putFile(uri)
                        .addOnSuccessListener {
                            Log.d("AudioUpload", "Uploaded audio: $path")
                            onComplete()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Audio upload failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }
                } else {
                    onComplete()
                }
            } else {
                onComplete()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Audio upload error: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    // Upload all captured images to Firebase
    private fun stopAndUploadImages(jobId: String, onComplete: () -> Unit) {
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
                    Log.d("ImageUpload", "Uploaded image: $path")
                    remaining--
                    if (remaining == 0) onComplete()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed (card $index)", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Get and increment counter from Firestore
    private fun getNextCounter(docId: String, prefix: String, callback: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("Counters").document(docId)

        db.runTransaction { txn ->
            val snapshot = txn.get(ref)
            val current = snapshot.getLong("count") ?: 0
            val next = current + 1
            txn.update(ref, "count", next)
            "$prefix${String.format("%03d", next)}"
        }.addOnSuccessListener { id ->
            callback(id)
        }.addOnFailureListener {
            Toast.makeText(this, "ID Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Create job and customer in Firestore
    private fun createJob(jobId: String, customerId: String) {
        val db = FirebaseFirestore.getInstance()

        val createdDateTime = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date())
        val createdAtTimestamp = System.currentTimeMillis()
        val complaintDate = createdDateTime

        // Estimated Completion Date = 7 days from now
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val completionDate = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(calendar.time)

        val issues = mutableListOf<String>().apply {
            if (cbTurningOn.isChecked) add("Turning on")
            if (cbWaterNotPumping.isChecked) add("Water not pumping")
            if (cbLowPressure.isChecked) add("Low pressure")
            if (cbHighPressure.isChecked) add("High pressure")
            if (cbAutoTurnOff.isChecked) add("Auto turn off")
            if (cbStartTakesTime.isChecked) add("Start takes time")
        }

        // Job data
        val job = hashMapOf(
            "jobId" to jobId,
            "customerId" to customerId,
            "customerName" to etCustomerName.text.toString().trim(),
            "contractNo" to etContractNo.text.toString().trim(),
            "make" to dataMake.text.toString().trim(),
            "serialNumber" to dataSLNo.text.toString().trim(),
            "purchaseDate" to btnPurchaseDate.text.toString().trim(),
            "warrantyStatus" to findViewById<RadioButton>(radioWarrantyStatus.checkedRadioButtonId)?.text.toString(),
            "complaintMethod" to when {
                radioOnline.isChecked -> "Online"
                radioByMobileCall.isChecked -> "By Mobile Call"
                radioWalkIn.isChecked -> "Walk-in"
                radioOther.isChecked -> "Other"
                else -> ""
            },
            "complaintText" to editComplaint.text.toString().trim(),
            "complaintType" to findViewById<RadioButton>(radioComplaintType.checkedRadioButtonId)?.text.toString(),
            "customerType" to findViewById<RadioButton>(radioCustomerType.checkedRadioButtonId)?.text.toString(),
            "assignedEngineer" to selectedEngineer,
            "issues" to issues,
            "description" to taIssueDescription.text.toString().trim(),
            "createdByName" to currentUserName,
            "createdByRole" to currentUserRole,
            "status" to "New",
            "payment" to "0",
            "createdDateTime" to createdDateTime,
            "createdAtTimestamp" to createdAtTimestamp,
            "complaintDate" to complaintDate,
            "completionDate" to completionDate
        )

        // Customer data
        val customer = hashMapOf(
            "customerId" to customerId,
            "name" to etCustomerName.text.toString().trim(),
            "contractNo" to etContractNo.text.toString().trim(),
            "addedBy" to "$currentUserRole: $currentUserName",
            "createdDateTime" to createdDateTime
        )

        // Save job + customer data to Firestore
        db.collection("Jobs").document("job.$jobId").set(job).addOnSuccessListener {
            db.collection("Customers").document("customer.$customerId").set(customer).addOnSuccessListener {
                Toast.makeText(this, "Job Created Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Customer Save Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Job Creation Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
