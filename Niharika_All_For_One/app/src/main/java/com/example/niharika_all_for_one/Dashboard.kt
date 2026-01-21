package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.niharika_all_for_one.utils.HeaderManager
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard : AppCompatActivity() {

    // --- Cards ---
    private lateinit var cardNewJob: CardView
    private lateinit var cardJobsPending: CardView
    private lateinit var cardJobsCompleted: CardView

    // --- Card TextViews for counts ---
    private lateinit var tvJobsPending: TextView
    private lateinit var tvJobsCompleted: TextView

    // --- Action Buttons ---
    private lateinit var btnJobsList: Button
    private lateinit var btnNewJob: Button
    private lateinit var btnSpairPartsBag: Button
    private lateinit var btnSpairPartsWorkshop: Button
    private lateinit var btnStartEndDay: Button
    private lateinit var btnWorkingJobs: Button
    private lateinit var btnPendingJobs: Button
    private lateinit var btnCompletedJobs: Button
    private lateinit var btnSpairParts: Button
    private lateinit var btnSendReminder: Button
    private lateinit var btnGenerateReport: Button
    private lateinit var btnArchives: Button
    private lateinit var btnAttendance: Button
    private lateinit var btnPayouts: Button
    private lateinit var btnApproveNewId: Button
    private lateinit var btnWarrrantyStatus: Button
    private lateinit var btnOrderSpairParts: Button
    private lateinit var btnWriteMail: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnCustomerList: Button

    // Role and User Info
    private lateinit var role: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize shared header
        HeaderManager.init(this)
        initViews()
        dashboardUI()
        initClickListeners()
    }

    private fun initViews() {
        // --- Cards ---
        cardNewJob = findViewById(R.id.cardNewJob)
        cardJobsPending = findViewById(R.id.cardJobsPending)
        cardJobsCompleted = findViewById(R.id.cardJobsCompleted)

        // --- Card Count TextViews ---
        tvJobsPending = findViewById(R.id.jobsPending)
        tvJobsCompleted = findViewById(R.id.jobsCompleted)

        // --- Action Buttons ---
        btnJobsList = findViewById(R.id.btnJobsList)
        btnNewJob = findViewById(R.id.btnNewJob)
        btnSpairPartsBag = findViewById(R.id.btnSpairPartsBag)
        btnSpairPartsWorkshop = findViewById(R.id.btnSpairPartsWorkshop)
        btnWorkingJobs = findViewById(R.id.btnWorkingJobs)
        btnPendingJobs = findViewById(R.id.btnPendingJobs)
        btnCompletedJobs = findViewById(R.id.btnCompletedJobs)
        btnSpairParts = findViewById(R.id.btnSpairParts)
        btnSendReminder = findViewById(R.id.btnSendReminder)
        btnGenerateReport = findViewById(R.id.btnGenerateReport)
        btnArchives = findViewById(R.id.btnArchives)
        btnAttendance = findViewById(R.id.btnAttendance)
        btnPayouts = findViewById(R.id.btnPayouts)
        btnApproveNewId = findViewById(R.id.btnApproveNewId)
        btnWarrrantyStatus = findViewById(R.id.btnWarrrantyStatus)
        btnOrderSpairParts = findViewById(R.id.btnOrderSpairParts)
        btnWriteMail = findViewById(R.id.btnWriteMail)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnCustomerList = findViewById(R.id.btnCustomerList)
        btnStartEndDay = findViewById(R.id.btnStartEndDay)
    }

    private fun dashboardUI() {
        val prefs = AppPreferences(this)

        // ✅ Debug logs
        Log.d("DashboardDebug", "Phone: ${prefs.getPhone()}")
        Log.d("DashboardDebug", "IsLogin: ${prefs.getIsLogin()}")
        Log.d("DashboardDebug", "Role: ${prefs.getUserRole()}")
        Log.d("DashboardDebug", "Name: ${prefs.getName()}")
        Log.d("DashboardDebug", "Email: ${prefs.getEmail()}")

        role = prefs.getUserRole() ?: "Unknown"
        userName = prefs.getName() ?: "Unknown"

        // ✅ Agar role null hai toh pehle check karo
        if (role == "Unknown" || role.isEmpty()) {
            Log.e("Dashboard", "Role is null or empty!")
            Toast.makeText(this, "Role not found. Please login again.", Toast.LENGTH_LONG).show()
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        // Apply role-specific UI
        when (role) {
            "Engineer (Workshop)" -> {
                cardJobsPending.visibility = View.VISIBLE
                cardJobsCompleted.visibility = View.VISIBLE
                btnJobsList.visibility = View.VISIBLE
                btnNewJob.visibility = View.VISIBLE
                btnSpairPartsBag.visibility = View.VISIBLE
                btnSpairPartsWorkshop.visibility = View.VISIBLE
                btnStartEndDay.visibility = View.VISIBLE
            }
            "Engineer (Field)" -> {
                cardJobsPending.visibility = View.VISIBLE
                cardJobsCompleted.visibility = View.VISIBLE
                btnJobsList.visibility = View.VISIBLE
                btnSpairPartsWorkshop.visibility = View.VISIBLE
                btnStartEndDay.visibility = View.VISIBLE
            }
            "Manager" -> {
                cardNewJob.visibility = View.VISIBLE
                cardJobsCompleted.visibility = View.VISIBLE
                btnWorkingJobs.visibility = View.VISIBLE
                btnPendingJobs.visibility = View.VISIBLE
                btnCompletedJobs.visibility = View.VISIBLE
                btnSpairParts.visibility = View.VISIBLE
                btnSendReminder.visibility = View.VISIBLE
                btnGenerateReport.visibility = View.VISIBLE
                btnStartEndDay.visibility = View.VISIBLE
            }
            "Admin" -> {
                cardNewJob.visibility = View.VISIBLE
                cardJobsCompleted.visibility = View.VISIBLE
                btnWorkingJobs.visibility = View.VISIBLE
                btnPendingJobs.visibility = View.VISIBLE
                btnCompletedJobs.visibility = View.VISIBLE
                btnSpairParts.visibility = View.VISIBLE
                btnSendReminder.visibility = View.VISIBLE
                btnGenerateReport.visibility = View.VISIBLE
                btnArchives.visibility = View.VISIBLE
                btnAttendance.visibility = View.VISIBLE
                btnWarrrantyStatus.visibility = View.VISIBLE
                btnOrderSpairParts.visibility = View.VISIBLE
                btnWriteMail.visibility = View.VISIBLE
                btnStatistics.visibility = View.VISIBLE
                btnCustomerList.visibility = View.VISIBLE
                btnStartEndDay.visibility = View.VISIBLE
            }
            "Owner" -> {
                cardNewJob.visibility = View.VISIBLE
                cardJobsCompleted.visibility = View.VISIBLE
                btnWorkingJobs.visibility = View.VISIBLE
                btnPendingJobs.visibility = View.VISIBLE
                btnCompletedJobs.visibility = View.VISIBLE
                btnSpairParts.visibility = View.VISIBLE
                btnSendReminder.visibility = View.VISIBLE
                btnGenerateReport.visibility = View.VISIBLE
                btnArchives.visibility = View.VISIBLE
                btnAttendance.visibility = View.VISIBLE
                btnPayouts.visibility = View.VISIBLE
                btnApproveNewId.visibility = View.VISIBLE
                btnWarrrantyStatus.visibility = View.VISIBLE
                btnOrderSpairParts.visibility = View.VISIBLE
                btnWriteMail.visibility = View.VISIBLE
                btnStatistics.visibility = View.VISIBLE
            }
            else -> {
                Log.w("Dashboard", "Unknown role: $role")
                Toast.makeText(this, "Invalid Role: $role", Toast.LENGTH_LONG).show()
                prefs.clearPreferences()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Start_Screen_Activity::class.java))
                finish()
            }
        }

        // ✅ Load job counts after UI setup
        loadJobCounts()
    }

    // ✅ Load job counts from Firestore based on user role
    private fun loadJobCounts() {
        val db = FirebaseFirestore.getInstance()

        db.collection("Jobs")
            .get()
            .addOnSuccessListener { result ->
                var pendingCount = 0
                var completedCount = 0

                val currentTimeMillis = System.currentTimeMillis()
                val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L

                for (doc in result) {
                    val status = doc.getString("status")?.lowercase() ?: ""
                    val complaintDate = doc.getString("complaintDate") ?: ""
                    val assignedEngineer = doc.getString("assignedEngineer") ?: ""
                    val complaintType = doc.getString("complaintType") ?: ""

                    // ✅ Check if current user can view this job
                    if (!canViewJob(assignedEngineer, complaintType)) {
                        continue // Skip jobs this user can't see
                    }

                    // Parse complaint date to check if older than 7 days
                    val complaintDateMillis = try {
                        java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.getDefault())
                            .parse(complaintDate)?.time ?: currentTimeMillis
                    } catch (e: Exception) {
                        currentTimeMillis
                    }

                    val isOlderThanWeek = (currentTimeMillis - complaintDateMillis) > oneWeekMillis

                    when {
                        // Completed and Ended jobs count
                        status == "completed" || status == "ended" -> completedCount++

                        // Pending jobs: New, Pending, or Red (old pending/new jobs)
                        status == "new" || status == "pending" ||
                                ((status == "new" || status == "pending") && isOlderThanWeek) -> pendingCount++
                    }
                }

                // Update UI with counts
                tvJobsPending.text = pendingCount.toString()
                tvJobsCompleted.text = completedCount.toString()

                Log.d("Dashboard", "Pending: $pendingCount, Completed: $completedCount")
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error loading job counts: ${e.message}")
                Toast.makeText(this, "Failed to load job counts", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ Check if current user can view this job (same logic as Jobs_List_Activity)
    private fun canViewJob(assignedEngineer: String, complaintType: String): Boolean {
        return when {
            // Roles that can view ALL jobs
            role in listOf("Manager", "Admin", "Super Admin", "Owner") -> true

            // Engineer (Workshop) role can only see Workshop jobs assigned to them
            role == "Engineer (Workshop)" &&
                    assignedEngineer == userName &&
                    complaintType.equals("Workshop", ignoreCase = true) -> true

            // Engineer (Field) role can only see Field jobs assigned to them
            role == "Engineer (Field)" &&
                    assignedEngineer == userName &&
                    complaintType.equals("Field", ignoreCase = true) -> true

            // Any other case, user can't view the job
            else -> false
        }
    }

    private fun initClickListeners() {
        cardNewJob.setOnClickListener {
            startActivity(Intent(this, New_Job_Activity::class.java))
        }

        cardJobsPending.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        cardJobsCompleted.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnJobsList.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnNewJob.setOnClickListener {
            startActivity(Intent(this, New_Job_Activity::class.java))
        }

        btnSpairPartsBag.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

        btnSpairPartsWorkshop.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

//        btnStartEndDay.setOnClickListener {
//            Toast.makeText(this, "Start/End Day Clicked", Toast.LENGTH_SHORT).show()
//        }

        btnStartEndDay.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse("https://play.google.com/apps/testing/com.tgb.niharikaattendanceenrolment")
            startActivity(intent)
        }

        btnWorkingJobs.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnPendingJobs.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnCompletedJobs.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnSpairParts.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

        btnSendReminder.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
        }

        btnGenerateReport.setOnClickListener {
            startActivity(Intent(this, Report_Generation_Activity::class.java))
        }

        btnArchives.setOnClickListener {
            Toast.makeText(this, "Archives", Toast.LENGTH_SHORT).show()
        }

//        btnAttendance.setOnClickListener {
//            startActivity(Intent(this, Attendance_Activity::class.java))
//        }

        btnAttendance.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = android.net.Uri.parse("https://play.google.com/apps/testing/com.tgb.niharikaattendanceenrolment")
            startActivity(intent)
        }

        btnPayouts.setOnClickListener {
            startActivity(Intent(this, Profile_Activity::class.java))
        }

        btnApproveNewId.setOnClickListener {
            startActivity(Intent(this, Approvals_Activity::class.java))
        }

        btnWarrrantyStatus.setOnClickListener {
            Toast.makeText(this, "Warranty Status Clicked", Toast.LENGTH_SHORT).show()
        }

        btnOrderSpairParts.setOnClickListener {
            Toast.makeText(this, "Order Spare Parts Clicked", Toast.LENGTH_SHORT).show()
        }

        btnWriteMail.setOnClickListener {
            Toast.makeText(this, "Write Mail Clicked", Toast.LENGTH_SHORT).show()
        }

        btnStatistics.setOnClickListener {
            Toast.makeText(this, "Statistics Clicked", Toast.LENGTH_SHORT).show()
        }

        btnCustomerList.setOnClickListener {
            startActivity(Intent(this, Customer_List_Activity::class.java))
        }
    }
}