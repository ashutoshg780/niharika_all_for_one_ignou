package com.example.niharika_all_for_one

import android.Manifest
import android.app.Activity
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
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
//import loadJobImages
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.get

class Edit_Job_Activity : AppCompatActivity() {

    // Firebase
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // UI Elements (lateinit bindings)
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var tvJOBID: TextView
    private lateinit var etCustomerName: EditText
    private lateinit var etContractNo: EditText
    private lateinit var btnPurchaseDate: Button
    private lateinit var dataMake: EditText
    private lateinit var dataSLNo: EditText
    private lateinit var taIssueDescription: EditText
    private lateinit var editComplaint: EditText
    private lateinit var tvEngineerName: TextView
    private lateinit var btnUpdateJob: Button

    // Radio Groups
    private lateinit var radioWarrantyStatus: RadioGroup
    private lateinit var radioComplaintMethod: GridLayout
    private lateinit var radioComplaintType: RadioGroup
    private lateinit var radioCustomerType: RadioGroup

    // Radio Buttons
    private lateinit var radioOnline: RadioButton
    private lateinit var radioByMobileCall: RadioButton
    private lateinit var radioWalkIn: RadioButton
    private lateinit var radioOther: RadioButton
    private lateinit var rbField: RadioButton
    private lateinit var rbWorkshop: RadioButton
    private lateinit var rbInWarranty: RadioButton
    private lateinit var rbOutOfWarranty: RadioButton
    private lateinit var rbRegular: RadioButton
    private lateinit var rbVIP: RadioButton


    // CheckBoxes
    private lateinit var cbTurningOn: CheckBox
    private lateinit var cbWaterNotPumping: CheckBox
    private lateinit var cbLowPressure: CheckBox
    private lateinit var cbHighPressure: CheckBox
    private lateinit var cbAutoTurnOff: CheckBox
    private lateinit var cbStartTakesTime: CheckBox

    // Global variables
    private var jobId: String = ""
    private var selectedEngineer: String = ""
    private var currentUserRole: String = ""
    private var currentUserName: String = ""

    // Constants
    private val REQUEST_IMAGE_CAPTURE = 1001
    private val REQUEST_CAMERA_PERMISSION = 2001
    private val REQUEST_IMAGE_TEXT_RECOGNITION = 2002
    private val REQUEST_VOICE_COMPLAINT = 3001
    private val REQUEST_VOICE_ISSUE = 3002
    private val REQUEST_RECORD_AUDIO = 3003
    private val REQUEST_RECORD_AUDIO_PERMISSION = 5001


    // Camera & Voice
    private var currentImageCardId = -1
    private lateinit var photoUri: Uri
    private val localImageMap = mutableMapOf<Int, File>()
    private val uploadedImagePaths = mutableListOf<String>()

    private var mediaRecorder: MediaRecorder? = null
    private lateinit var audioFilePath: String
    private var currentVoiceTargetField: EditText? = null
    private var recordingField = ""
    private val uploadedAudioPaths = mutableListOf<String>()

    private var currentOcrTargetField: EditText? = null

    private lateinit var engJobUpdatesLayout: LinearLayout
    private lateinit var layoutInflater: LayoutInflater

    private val imageCardIds = listOf(
        R.id.cardImg1, R.id.cardImg2, R.id.cardImg3,
        R.id.cardImg4, R.id.cardImg5, R.id.cardImg6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_job)

        // Get passed jobId from intent
        jobId = intent.getStringExtra("jobId") ?: ""
        if (jobId.isBlank()) {
            Toast.makeText(this, "Invalid Job ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // ==================== Bind All Views ====================
        // Header
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // Top Fields
        tvJOBID = findViewById(R.id.tvJOBID)
        etCustomerName = findViewById(R.id.etCustomerName)
        etContractNo = findViewById(R.id.etContractNo)
        btnPurchaseDate = findViewById(R.id.btnPurchaseDate)
        radioWarrantyStatus = findViewById(R.id.radioWarrantyStatus)
        rbInWarranty = findViewById(R.id.rbInWarranty)
        rbOutOfWarranty = findViewById(R.id.rbOutOfWarranty)

        // Complaint Method
        radioComplaintMethod = findViewById(R.id.radioComplaintMethod)
        radioOnline = findViewById(R.id.radioOnline)
        radioByMobileCall = findViewById(R.id.radioByMobileCall)
        radioWalkIn = findViewById(R.id.radioWalkIn)
        radioOther = findViewById(R.id.radioOther)
        editComplaint = findViewById(R.id.editComplaint)

        // Complaint Type
        radioComplaintType = findViewById(R.id.radioComplaintType)
        rbField = findViewById(R.id.rbField)
        rbWorkshop = findViewById(R.id.rbWorkshop)
        tvEngineerName = findViewById(R.id.tvEngineerName)

        // Customer Type
        radioCustomerType = findViewById(R.id.radioCustomerType)
        rbRegular = findViewById(R.id.rbRegular)
        rbVIP = findViewById(R.id.rbVIP)

        engJobUpdatesLayout = findViewById(R.id.engjobupdates)
        layoutInflater = LayoutInflater.from(this)

        // Image Cards
        imageCardIds.forEachIndexed { index, id ->
            findViewById<CardView>(id).setOnClickListener {
                currentImageCardId = index + 1
                checkCameraPermissionAndOpen()
            }
        }

        // Make + SL No
        dataMake = findViewById(R.id.dataMake)
        dataSLNo = findViewById(R.id.dataSLNo)

        // OCR camera icons
        dataMake.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataMake.right - dataMake.compoundDrawables[2].bounds.width()) {
                currentOcrTargetField = dataMake
                openCameraForTextRecognition()
                true
            } else false
        }

        dataSLNo.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_UP &&
                e.rawX >= dataSLNo.right - dataSLNo.compoundDrawables[2].bounds.width()) {
                currentOcrTargetField = dataSLNo
                openCameraForTextRecognition()
                true
            } else false
        }

        // Issue checkboxes
        cbTurningOn = findViewById(R.id.checkbox_turning_on)
        cbWaterNotPumping = findViewById(R.id.checkbox_water_not_pumping)
        cbLowPressure = findViewById(R.id.checkbox_low_pressure)
        cbHighPressure = findViewById(R.id.checkbox_high_pressure)
        cbAutoTurnOff = findViewById(R.id.checkbox_auto_turn_off)
        cbStartTakesTime = findViewById(R.id.checkbox_start_takes_time)
        taIssueDescription = findViewById(R.id.taIssueDescription)

        // Voice input mics
        taIssueDescription.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= taIssueDescription.right - taIssueDescription.compoundDrawables[2].bounds.width()) {
                startVoiceInput(taIssueDescription, "issue")
                true
            } else false
        }

        editComplaint.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP &&
                event.rawX >= editComplaint.right - editComplaint.compoundDrawables[2].bounds.width()) {
                startVoiceInput(editComplaint, "complaint")
                true
            } else false
        }

        // Complaint method → enable/disable editComplaint
        val complaintRadios = listOf(radioOnline, radioByMobileCall, radioWalkIn, radioOther)
        complaintRadios.forEach { rb ->
            rb.setOnClickListener {
                complaintRadios.forEach { it.isChecked = false }
                rb.isChecked = true
                editComplaint.isEnabled = rb == radioOther
                if (!editComplaint.isEnabled) editComplaint.text.clear()
            }
        }

        // Complaint Type (select engineer)
        rbField.setOnClickListener { showEngineerDialog("Engineer (Field)") }
        rbWorkshop.setOnClickListener { showEngineerDialog("Engineer (Workshop)") }

        // Role + Name
        checkUser()

        // Logout and Back
//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Purchase Date Picker
        btnPurchaseDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                btnPurchaseDate.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(cal.time)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // ======= Get jobId from Intent and load job data =======
        jobId = intent.getStringExtra("jobId") ?: ""
        if (jobId.isNotEmpty()) {
            tvJOBID.text = "Job ID: $jobId"
            prefillData(jobId)
        }

        // ======= Submit Button Logic =======
        btnUpdateJob = findViewById(R.id.btnUpdateJob)

        btnUpdateJob.setOnClickListener {
            uploadPendingImagesAndThen {
                uploadAudioIfAny {
                    updateJobToFirestore(jobId)
                }
            }
        }
    }

    // ✅ Get role and name from shared preferences
//    private fun checkUser() {
//        val phone = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null) ?: return
//        FirebaseFirestore.getInstance().collection("Users").document(phone).get()
//            .addOnSuccessListener {
//                currentUserName = it.getString("fullName") ?: ""
//                currentUserRole = it.getString("role") ?: ""
//                RoleTitle.text = "$currentUserRole: $currentUserName"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phone = prefs.getPhone() ?: return

        FirebaseFirestore.getInstance().collection("Users").document(phone).get()
            .addOnSuccessListener {
                currentUserName = it.getString("fullName") ?: ""
                currentUserRole = it.getString("role") ?: ""
                RoleTitle.text = "$currentUserRole: $currentUserName"
            }
    }

    // ✅ Show dialog to choose engineer based on selected complaint type
    private fun showEngineerDialog(role: String) {
        FirebaseFirestore.getInstance().collection("Users")
            .whereEqualTo("role", role)
            .get()
            .addOnSuccessListener { snapshot ->
                val names = snapshot.documents.mapNotNull { it.getString("fullName") }
                AlertDialog.Builder(this)
                    .setTitle("Select $role")
                    .setItems(names.toTypedArray()) { _, i ->
                        selectedEngineer = names[i]
                        tvEngineerName.text = "Assigned to: $selectedEngineer"
                    }
                    .show()
            }
    }

    // ✅ Voice typing + recording
    private fun startVoiceInput(targetField: EditText, fieldType: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        try {
            recordingField = fieldType
            currentVoiceTargetField = targetField
            startRecordingAudio()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            val reqCode = if (fieldType == "complaint") REQUEST_VOICE_COMPLAINT else REQUEST_VOICE_ISSUE
            startActivityForResult(intent, reqCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ✅ Start recording audio
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

    // ✅ Ask permission before opening camera
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

    // ✅ Open default camera intent
    private fun openCamera() {
        val imageFile = File.createTempFile("cardImg$currentImageCardId", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    // ✅ Open camera for OCR
    private fun openCameraForTextRecognition() {
        val imageFile = File.createTempFile("ocr_temp", ".jpg", cacheDir)
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_TEXT_RECOGNITION)
    }

    // ✅ Show image in CardView
    private fun showPreviewInCard(uri: Uri) {
        val cardView = findViewById<CardView>(imageCardIds[currentImageCardId - 1])
        val imgView = (cardView.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView
        Glide.with(this).load(uri).centerCrop().into(imgView)
    }

    // ✅ Handle result from camera, voice, and OCR
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, photoUri))
                    } else {
                        MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
                    }

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

            REQUEST_VOICE_COMPLAINT, REQUEST_VOICE_ISSUE -> {
                val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
                currentVoiceTargetField?.setText(spoken)
                // Do NOT upload here! Upload will be done on final submit.
            }
        }
    }

    // 📸 Upload all images to Firebase before updating Firestore
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
                    uploadedImagePaths.add(path)
                    remaining--
                    if (remaining == 0) onComplete()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Image upload failed: card $index", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // 🎤 Upload audio if any
    private fun uploadAudioIfAny(onComplete: () -> Unit) {
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
                            uploadedAudioPaths.add(path)
                            Toast.makeText(this, "Audio uploaded: $fileName", Toast.LENGTH_SHORT).show()
                            onComplete()
                        }.addOnFailureListener {
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
            Toast.makeText(this, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
            onComplete()
        }
    }

    // ✅ Final Firestore update
    private fun updateJobToFirestore(jobId: String) {
        val db = FirebaseFirestore.getInstance()

        val updatedDateTime = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date())
        val timestamp = System.currentTimeMillis()

        val issues = mutableListOf<String>().apply {
            if (cbTurningOn.isChecked) add("Turning on")
            if (cbWaterNotPumping.isChecked) add("Water not pumping")
            if (cbLowPressure.isChecked) add("Low pressure")
            if (cbHighPressure.isChecked) add("High pressure")
            if (cbAutoTurnOff.isChecked) add("Auto turn off")
            if (cbStartTakesTime.isChecked) add("Start takes time")
        }

        val updateData = mapOf(
            "customerName" to etCustomerName.text.toString(),
            "contractNo" to etContractNo.text.toString(),
            "purchaseDate" to btnPurchaseDate.text.toString(),
            "warrantyStatus" to findViewById<RadioButton>(radioWarrantyStatus.checkedRadioButtonId)?.text.toString(),
            "complaintMethod" to when {
                radioOnline.isChecked -> "Online"
                radioByMobileCall.isChecked -> "By Mobile Call"
                radioWalkIn.isChecked -> "Walk-in"
                radioOther.isChecked -> "Other"
                else -> ""
            },
            "complaintText" to editComplaint.text.toString(),
            "complaintType" to findViewById<RadioButton>(radioComplaintType.checkedRadioButtonId)?.text.toString(),
            "customerType" to findViewById<RadioButton>(radioCustomerType.checkedRadioButtonId)?.text.toString(),
            "assignedEngineer" to selectedEngineer,
            "make" to dataMake.text.toString(),
            "serialNumber" to dataSLNo.text.toString(),
            "description" to taIssueDescription.text.toString(),
            "issues" to issues,
            "lastUpdatedBy" to "$currentUserRole: $currentUserName",
            "lastUpdatedAt" to updatedDateTime,
            "lastUpdatedTimestamp" to timestamp
        )

        db.collection("Jobs").document("job.$jobId")
            .update(updateData)
            .addOnSuccessListener {
                Toast.makeText(this, "Job updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    // 🔄 Prefill data from Firestore
    private fun prefillData(jobId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("Jobs").document("job.$jobId").get()
            .addOnSuccessListener { doc ->
                etCustomerName.setText(doc.getString("customerName") ?: "")
                etContractNo.setText(doc.getString("contractNo") ?: "")
                btnPurchaseDate.text = doc.getString("purchaseDate") ?: "01-01-1974"
                dataMake.setText(doc.getString("make") ?: "")
                dataSLNo.setText(doc.getString("serialNumber") ?: "")
                taIssueDescription.setText(doc.getString("description") ?: "")

                val issues = doc.get("issues") as? List<*> ?: emptyList<String>()
                cbTurningOn.isChecked = "Turning on" in issues
                cbWaterNotPumping.isChecked = "Water not pumping" in issues
                cbLowPressure.isChecked = "Low pressure" in issues
                cbHighPressure.isChecked = "High pressure" in issues
                cbAutoTurnOff.isChecked = "Auto turn off" in issues
                cbStartTakesTime.isChecked = "Start takes time" in issues

                when (doc.getString("warrantyStatus")) {
                    "In Warranty" -> radioWarrantyStatus.check(R.id.rbInWarranty)
                    "Out of Warranty" -> radioWarrantyStatus.check(R.id.rbOutOfWarranty)
                }

                when (doc.getString("complaintMethod")) {
                    "Online" -> radioOnline.isChecked = true
                    "By Mobile Call" -> radioByMobileCall.isChecked = true
                    "Walk-in" -> radioWalkIn.isChecked = true
                    "Other" -> radioOther.isChecked = true
                }

                editComplaint.setText(doc.getString("complaintText") ?: "")

                when (doc.getString("complaintType")) {
                    "Field" -> rbField.isChecked = true
                    "Workshop" -> rbWorkshop.isChecked = true
                }

                when (doc.getString("customerType")) {
                    "Regular" -> radioCustomerType.check(R.id.rbRegular)
                    "VIP" -> radioCustomerType.check(R.id.rbVIP)
                }

                selectedEngineer = doc.getString("assignedEngineer") ?: ""
                tvEngineerName.text = "Assigned to: $selectedEngineer"

                // Load images for cardImg1 to cardImg6 if uploaded by allowed roles
                loadJobImages(jobId)

                loadEngineerUpdates(jobId)

            }
    }

    private fun loadJobImages(jobId: String) {
        val allowedRoles = listOf("Manager", "Admin", "Super Admin", "Owner")
        val storage = FirebaseStorage.getInstance().reference.child("Jobs/job.$jobId/images")

        storage.listAll().addOnSuccessListener { result ->
            result.items.forEach { ref ->
                val name = ref.name // e.g., Admin:.Ashutosh.23-05-2025.01-33 PM.cardImg1.jpg
                if (allowedRoles.any { role -> name.startsWith(role) }) {
                    val cardId = name.substringAfterLast(".cardImg").substringBefore(".jpg").toIntOrNull() ?: return@forEach
                    if (cardId in 1..6) {
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            val card = findViewById<CardView>(imageCardIds[cardId - 1])
                            val img = (card.getChildAt(0) as LinearLayout).getChildAt(0) as ImageView
                            Glide.with(this).load(uri).centerCrop().into(img)
                        }
                    }
                }
            }
        }
    }

    // Engineer's Updates
    //========================================================================================================================

    // 🔄 Load engineer updates dynamically and display them
    private fun loadEngineerUpdates(jobId: String) {
        val db = FirebaseFirestore.getInstance()
        val updatesRef = db.collection("Jobs").document("job.$jobId").collection("updates")

        engJobUpdatesLayout.removeAllViews() // Clear existing updates if any

        updatesRef.get().addOnSuccessListener { snapshots ->
            for (doc in snapshots.documents) {
                val data = doc.data ?: continue // Skip if data is missing

                // Check if update is from Field or Workshop engineer based on Firestore document ID
                if (doc.id.contains("Field", ignoreCase = true)) {
                    val view = layoutInflater.inflate(R.layout.update_job_engineer_field, engJobUpdatesLayout, false)
                    fillFieldUpdateView(view, data) // Fill the Field update view
                    engJobUpdatesLayout.addView(view) // Add to parent layout

                } else if (doc.id.contains("Workshop", ignoreCase = true)) {
                    val view = layoutInflater.inflate(R.layout.update_job_engineer_workshop, engJobUpdatesLayout, false)
                    fillWorkshopUpdateView(view, data) // Fill the Workshop update view
                    engJobUpdatesLayout.addView(view) // Add to parent layout
                }
            }

            // Show a message if there are no updates available
            if (engJobUpdatesLayout.childCount == 0) {
                Toast.makeText(this, "No engineer updates available.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load engineer updates: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 👷 Fill the Field update view with engineer data
    private fun fillFieldUpdateView(view: View, data: Map<String, Any>) {
        // ✅ Parse role & name from "roleName"
        val roleName = data["roleName"]?.toString() ?: "-"
        val parts = roleName.split(":.")
        val role = parts.getOrNull(0)?.replace(".", " ")?.trim() ?: "-"
        val name = parts.getOrNull(1)?.trim() ?: "-"

        view.findViewById<TextView>(R.id.tvEngineerRoledsp_field).text = role
        view.findViewById<TextView>(R.id.tvEngineerNamedsp_field).text = name

        // ✅ Parse receivedDate & receivedTime from "timestamp"
        val timestamp = data["timestamp"]?.toString() ?: "-"
        val firstSpaceIndex = timestamp.indexOf(" ")
        val date = if (firstSpaceIndex != -1) timestamp.substring(0, firstSpaceIndex).trim() else "-"
        val time = if (firstSpaceIndex != -1) timestamp.substring(firstSpaceIndex + 1).trim() else "-"

        view.findViewById<TextView>(R.id.receiveDate_field).text = date
        view.findViewById<TextView>(R.id.receiveTime_field).text = time

        // ✅ For this example, we'll assume handoverDate & handoverTime are same as received
        view.findViewById<TextView>(R.id.handoverDate_field).text = date
        view.findViewById<TextView>(R.id.handoverTime_field).text = time

        // ✅ Warranty
        val warrantyStatus = data["warrantyStatus"]?.toString() ?: ""
        view.findViewById<RadioButton>(R.id.rbInWarranty_field).isChecked = warrantyStatus.equals("In Warranty", true)
        view.findViewById<RadioButton>(R.id.rbOutOfWarranty_field).isChecked = warrantyStatus.equals("Out of Warranty", true)

        // ✅ Pump Issues Checkboxes
        val issues = data["issues"] as? List<*> ?: emptyList<String>()
        view.findViewById<CheckBox>(R.id.checkbox_turning_on_field).isChecked = "Turning on" in issues
        view.findViewById<CheckBox>(R.id.checkbox_water_not_pumping_field).isChecked = "Water not pumping" in issues
        view.findViewById<CheckBox>(R.id.checkbox_low_pressure_field).isChecked = "Low pressure" in issues
        view.findViewById<CheckBox>(R.id.checkbox_high_pressure_field).isChecked = "High pressure" in issues
        view.findViewById<CheckBox>(R.id.checkbox_auto_turn_off_field).isChecked = "Auto turn off" in issues
        view.findViewById<CheckBox>(R.id.checkbox_start_takes_time_field).isChecked = "Start takes time" in issues

        // ✅ Description
        view.findViewById<EditText>(R.id.taIssueDescription_field).setText(data["description"]?.toString() ?: "")

        // ✅ Next-Day Repair, Another Engineer, Workshop Repair statuses
        view.findViewById<RadioButton>(R.id.rbNeeded_field).isChecked = data["nextDayRepair"]?.toString() == "Needed"
        view.findViewById<RadioButton>(R.id.rbNotNeeded_field).isChecked = data["nextDayRepair"]?.toString() == "Not-Needed"

        view.findViewById<RadioButton>(R.id.rbEngNeeded_field).isChecked = data["anotherEngineer"]?.toString() == "Needed"
        view.findViewById<RadioButton>(R.id.rbEngNotNeeded_field).isChecked = data["anotherEngineer"]?.toString() == "Not-Needed"

        view.findViewById<RadioButton>(R.id.rbWorkshopNeeded_field).isChecked = data["workshopRepair"]?.toString() == "Needed"
        view.findViewById<RadioButton>(R.id.rbWorkshopNotNeeded_field).isChecked = data["workshopRepair"]?.toString() == "Not-Needed"

        // ✅ Make & SL No
        view.findViewById<EditText>(R.id.dataMake_field).setText(data["make"]?.toString() ?: "")
        view.findViewById<EditText>(R.id.dataSLNo_field).setText(data["serialNumber"]?.toString() ?: "")
    }

    // 🏭 Fill the Workshop update view with engineer data
    private fun fillWorkshopUpdateView(view: View, data: Map<String, Any>) {
        // ✅ Parse role & name from "roleName"
        val roleName = data["roleName"]?.toString() ?: "-"
        val parts = roleName.split(":.")
        val role = parts.getOrNull(0)?.replace(".", " ")?.trim() ?: "-"
        val name = parts.getOrNull(1)?.trim() ?: "-"

        view.findViewById<TextView>(R.id.tvEngineerRoledsp_workshop).text = role
        view.findViewById<TextView>(R.id.tvEngineerNamedsp_workshop).text = name

        // ✅ Parse receivedDate & receivedTime from "timestamp"
        val timestamp = data["timestamp"]?.toString() ?: "-"
        val firstSpaceIndex = timestamp.indexOf(" ")
        val date = if (firstSpaceIndex != -1) timestamp.substring(0, firstSpaceIndex).trim() else "-"
        val time = if (firstSpaceIndex != -1) timestamp.substring(firstSpaceIndex + 1).trim() else "-"

        view.findViewById<TextView>(R.id.receiveDate_workshop).text = date
        view.findViewById<TextView>(R.id.receiveTime_workshop).text = time

        // ✅ For this example, we'll assume handoverDate & handoverTime are same as received
        view.findViewById<TextView>(R.id.handoverDate_workshop).text = date
        view.findViewById<TextView>(R.id.handoverTime_workshop).text = time

        // ✅ Warranty
        val warrantyStatus = data["warrantyStatus"]?.toString() ?: ""
        view.findViewById<RadioButton>(R.id.rbInWarranty_workshop).isChecked = warrantyStatus.equals("In Warranty", true)
        view.findViewById<RadioButton>(R.id.rbOutOfWarranty_workshop).isChecked = warrantyStatus.equals("Out of Warranty", true)

        // ✅ Pump Issues Checkboxes
        val issues = data["issues"] as? List<*> ?: emptyList<String>()
        view.findViewById<CheckBox>(R.id.checkbox_turning_on_workshop).isChecked = "Turning on" in issues
        view.findViewById<CheckBox>(R.id.checkbox_water_not_pumping_workshop).isChecked = "Water not pumping" in issues
        view.findViewById<CheckBox>(R.id.checkbox_low_pressure_workshop).isChecked = "Low pressure" in issues
        view.findViewById<CheckBox>(R.id.checkbox_high_pressure_workshop).isChecked = "High pressure" in issues
        view.findViewById<CheckBox>(R.id.checkbox_auto_turn_off_workshop).isChecked = "Auto turn off" in issues
        view.findViewById<CheckBox>(R.id.checkbox_start_takes_time_workshop).isChecked = "Start takes time" in issues

        // ✅ Description
        view.findViewById<EditText>(R.id.taIssueDescription_workshop).setText(data["description"]?.toString() ?: "")

        // ✅ Next-Day Repair & Another Engineer statuses
        view.findViewById<RadioButton>(R.id.rbNeeded_workshop).isChecked = data["nextDayRepair"]?.toString() == "Needed"
        view.findViewById<RadioButton>(R.id.rbNotNeeded_workshop).isChecked = data["nextDayRepair"]?.toString() == "Not-Needed"

        view.findViewById<RadioButton>(R.id.rbEngNeeded_workshop).isChecked = data["anotherEngineer"]?.toString() == "Needed"
        view.findViewById<RadioButton>(R.id.rbEngNotNeeded_workshop).isChecked = data["anotherEngineer"]?.toString() == "Not-Needed"

        // ✅ Make & SL No
        view.findViewById<EditText>(R.id.dataMake_workshop).setText(data["make"]?.toString() ?: "")
        view.findViewById<EditText>(R.id.dataSLNo_workshop).setText(data["serialNumber"]?.toString() ?: "")
    }
}