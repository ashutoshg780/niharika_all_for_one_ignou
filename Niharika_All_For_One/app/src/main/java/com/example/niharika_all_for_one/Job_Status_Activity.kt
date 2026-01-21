package com.example.niharika_all_for_one

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Job_Status_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var jobDetails: TextView

    // Timeline Circles
    private lateinit var circle1: View
    private lateinit var circle2: View
    private lateinit var circle3: View
    private lateinit var circle4: View
    private lateinit var circle5: View

    // Timeline Lines
    private lateinit var line1: View
    private lateinit var line2: View
    private lateinit var line3: View
    private lateinit var line4: View

    // Icons
    private lateinit var editButton: ImageView
    private lateinit var notificationButton: ImageView
    private lateinit var callButton: ImageView

    // Action Buttons
    private lateinit var btnPartsUsed: Button
    private lateinit var btnUpdateRepair: Button
    private lateinit var btnDetailedBill: Button
    private lateinit var btnEndReopen: Button

    // Role Info
    private var userRole: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_job_status)

        auth = FirebaseAuth.getInstance()

        // Header UI
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        jobDetails = findViewById(R.id.jobDetails)

        // Timeline Views
        circle1 = findViewById(R.id.circle1)
        circle2 = findViewById(R.id.circle2)
        circle3 = findViewById(R.id.circle3)
        circle4 = findViewById(R.id.circle4)
        circle5 = findViewById(R.id.circle5)
        line1 = findViewById(R.id.line1)
        line2 = findViewById(R.id.line2)
        line3 = findViewById(R.id.line3)
        line4 = findViewById(R.id.line4)

        // Icons
        editButton = findViewById(R.id.editButton)
        notificationButton = findViewById(R.id.notificationButton)
        callButton = findViewById(R.id.callButton)

        // Action Buttons
        btnPartsUsed = findViewById(R.id.btnPartsUsed)
        btnUpdateRepair = findViewById(R.id.btnUpdateRepair)
        btnDetailedBill = findViewById(R.id.btnDetailedBill)
        btnEndReopen = findViewById(R.id.btnEndReopen)

        // Header actions
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

        // Load user info and continue
        checkUser {
            val jobId = intent.getStringExtra("jobId") ?: ""

            if (jobId.isNotEmpty()) fetchJobDetails(jobId)
            else Toast.makeText(this, "Job ID not found!", Toast.LENGTH_SHORT).show()
        }
    }

    // Get current user's role and name from Firestore
    private fun checkUser(callback: () -> Unit) {
        val prefs = AppPreferences(this)
        val phone = prefs.getPhone() ?: return

        FirebaseFirestore.getInstance().collection("Users").document(phone).get()
            .addOnSuccessListener {
                userRole = it.getString("role") ?: ""
                userName = it.getString("fullName") ?: ""
                RoleTitle.text = "$userRole: $userName"
                callback()
            }
    }


    // Load and show job info using jobId
    private fun fetchJobDetails(jobId: String) {
        FirebaseFirestore.getInstance().collection("Jobs").document("job.$jobId").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val job = doc.toObject(Job::class.java)
                    if (job != null) {
                        setJobDetailsText(doc)
                        updateTimeline(job)
                        handleButtons(doc)
                    }
                } else {
                    Toast.makeText(this, "Job not found!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching job: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Format and display job fields inside jobDetails TextView
    private fun setJobDetailsText(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val details = """
            Complaint Person: ${doc.getString("customerName")}
            Complaint No: ${doc.getString("jobId")}
            Assigned to: ${doc.getString("assignedEngineer")}
            Complaint Date: ${doc.getString("complaintDate")}
            Completion Date: ${doc.getString("completionDate")}
            Warranty Status: ${doc.getString("warrantyStatus")}
            Complaint Method: ${doc.getString("complaintMethod")}
            Complaint Type: ${doc.getString("complaintType")}
            Make: ${doc.getString("make")}
            Pump SL No: ${doc.getString("serialNumber")}
            Description: ${doc.getString("description")}
            Customer Type: ${doc.getString("customerType")}
            Status: ${doc.getString("status")}
        """.trimIndent()
        jobDetails.text = details
    }

    // Dynamic timeline update for circles and lines based on job status
    private fun updateTimeline(job: Job) {
        val status = job.status.lowercase(Locale.getDefault())
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        fun setGreen(v: View) = v.setBackgroundResource(R.drawable.circle_green)
        fun setYellow(v: View) = v.setBackgroundResource(R.drawable.circle_yellow)
        fun setRed(v: View) = v.setBackgroundResource(R.drawable.circle_red)
        fun setGray(v: View) = v.setBackgroundResource(R.drawable.circle_gray)
        fun setLineColor(v: View, hex: String) = v.setBackgroundColor(android.graphics.Color.parseColor(hex))

        when (status) {
            "new" -> {
                setGreen(circle1); setYellow(circle2); setGray(circle3); setGray(circle4); setGray(circle5)
                setLineColor(line1, "#FFC107"); setLineColor(line2, "#BDBDBD")
                setLineColor(line3, "#BDBDBD"); setLineColor(line4, "#BDBDBD")
            }
            "pending" -> {
                setGreen(circle1); setGreen(circle2); setYellow(circle3); setGray(circle4); setGray(circle5)
                setLineColor(line1, "#4CAF50"); setLineColor(line2, "#FFC107")
                setLineColor(line3, "#BDBDBD"); setLineColor(line4, "#BDBDBD")
            }
            "completed" -> {
                setGreen(circle1); setGreen(circle2); setGreen(circle3)
                val compTime = try { sdf.parse(job.completionDate)?.time ?: 0L } catch (e: Exception) { 0L }
                val now = System.currentTimeMillis()
                val isOld = now - compTime > 7 * 24 * 60 * 60 * 1000L
                if (isOld) {
                    setRed(circle4); setYellow(circle5)
                    setLineColor(line1, "#4CAF50"); setLineColor(line2, "#4CAF50")
                    setLineColor(line3, "#F44336"); setLineColor(line4, "#FFC107")
                } else {
                    setYellow(circle4); setGray(circle5)
                    setLineColor(line1, "#4CAF50"); setLineColor(line2, "#4CAF50")
                    setLineColor(line3, "#FFC107"); setLineColor(line4, "#BDBDBD")
                }
            }
            "ended" -> {
                listOf(circle1, circle2, circle3, circle4, circle5).forEach { setGreen(it) }
                listOf(line1, line2, line3, line4).forEach { setLineColor(it, "#4CAF50") }
            }
            else -> {
                listOf(circle1, circle2, circle3, circle4, circle5).forEach { setGray(it) }
                listOf(line1, line2, line3, line4).forEach { setLineColor(it, "#BDBDBD") }
            }
        }
    }

    // Control visibility and behavior of buttons/icons based on role and job state
    private fun handleButtons(doc: com.google.firebase.firestore.DocumentSnapshot) {
        val jobId = doc.getString("jobId") ?: ""
        val phone = doc.getString("contractNo") ?: ""
        val status = doc.getString("status") ?: ""
        val assignedEngineer = doc.getString("assignedEngineer") ?: ""
        val complaintType = doc.getString("complaintType") ?: ""

        val adminRoles = listOf("manager", "admin", "super admin", "owner")
        val role = userRole.lowercase()

        // === Edit Job Icon ===
        if (role in adminRoles) {
            editButton.visibility = View.VISIBLE
            editButton.setOnClickListener {
                startActivity(Intent(this, Edit_Job_Activity::class.java).apply {
                    putExtra("jobId", jobId)
                })
            }
        } else {
            editButton.visibility = View.GONE
        }

        // === Notification Icon ===
        if (role in adminRoles || role == "engineer (field)") {
            notificationButton.visibility = View.VISIBLE
            notificationButton.setOnClickListener {
                val msg = if (role == "engineer (field)")
                    "Please share your location for service.\nComplaint No: $jobId"
                else
                    "Job Update:\nStatus: $status\nComplaint No: $jobId"

                val uri = Uri.parse("https://wa.me/91$phone?text=${Uri.encode(msg)}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        } else {
            notificationButton.visibility = View.GONE
        }

        // === Call Icon ===
        if (role in adminRoles || role == "engineer (field)") {
            callButton.visibility = View.VISIBLE
            callButton.setOnClickListener {
                val uri = Uri.parse("tel:+91$phone")
                startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
        } else {
            callButton.visibility = View.GONE
        }

        // ... Inside onCreate or wherever your buttons are initialized
        btnPartsUsed.setOnClickListener {
            val adminRoles = listOf("manager", "admin", "super admin", "owner")
            val currentRole = userRole.trim().lowercase(Locale.getDefault())
            Log.d("SPBTN", "User role: $currentRole | jobId: $jobId")

            if (adminRoles.contains(currentRole)) {
                Log.d("SPBTN", "Opening Spare_Parts_Job_Activity for admin role")
                startActivity(Intent(this, Spare_Parts_Job_Activity::class.java).apply {
                    putExtra("jobId", jobId)
                })
            } else {
                Log.d("SPBTN", "Opening Spare_Parts_Used_Activity for engineer role")
                startActivity(Intent(this, Spare_Parts_Used_Activity::class.java).apply {
                    putExtra("jobId", jobId)
                })
            }
        }



        // === Update Repair Status Button ===
        btnUpdateRepair.setOnClickListener {
            when (role) {
                "engineer (workshop)" -> startActivity(Intent(this, Update_Job_Engineer_Workshop_Activity::class.java).putExtra("jobId", jobId))
                "engineer (field)" -> startActivity(Intent(this, Update_Job_Engineer_Field_Activity::class.java).putExtra("jobId", jobId))
                in adminRoles -> {
                    val options = arrayOf("Pending", "Completed")
                    AlertDialog.Builder(this)
                        .setTitle("Update Status")
                        .setItems(options) { _, index ->
                            FirebaseFirestore.getInstance().collection("Jobs").document("job.$jobId")
                                .update("status", options[index])
                            Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show()
                        }.show()
                }
            }
        }

        // === Detailed Bill Button ===
        if (role in adminRoles) {
            btnDetailedBill.visibility = View.VISIBLE
            btnDetailedBill.setOnClickListener {
                startActivity(Intent(this, Detailed_Bill_Activity::class.java).putExtra("jobId", jobId))
            }
        } else {
            btnDetailedBill.visibility = View.GONE
        }

        // === End / Reopen Button Logic ===
        if (role in listOf("engineer (workshop)", "engineer (field)")) {
            // Engineers see "Complete" text
            btnEndReopen.text = "Complete"
            btnEndReopen.setOnClickListener {
                val updates = mapOf(
                    "status" to "Completed",
                    "completedBy" to "$userRole: $userName",
                    "assignedEngineer" to "",
                    "complaintType" to ""
                )
                FirebaseFirestore.getInstance().collection("Jobs").document("job.$jobId")
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Job marked as Completed", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update job", Toast.LENGTH_SHORT).show()
                    }
            }
        } else if (role in adminRoles) {
            // Admins: dialog with End / Reopen
            btnEndReopen.text = "End/ Reopen"
            btnEndReopen.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Choose Action")
                    .setMessage("Do you want to End this job or Reopen it?")
                    .setPositiveButton("End") { _, _ ->
                        FirebaseFirestore.getInstance().collection("Jobs").document("job.$jobId")
                            .update("status", "Ended")
                            .addOnSuccessListener {
                                Toast.makeText(this, "Job marked as Ended", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                    }
                    .setNegativeButton("Reopen") { _, _ ->
                        startActivity(Intent(this, Edit_Job_Activity::class.java).putExtra("jobId", jobId))
                    }
                    .show()
            }
        }
    }
}
