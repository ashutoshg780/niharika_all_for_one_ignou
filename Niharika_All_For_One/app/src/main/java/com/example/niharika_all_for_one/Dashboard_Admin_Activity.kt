package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Dashboard_Admin_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Profile: ImageView

    private lateinit var cardNewJob: CardView
    private lateinit var cardJobsCompleted: CardView
    private lateinit var btnWorkingJobs: Button
    private lateinit var btnPendingJobs: Button
    private lateinit var btnCompletedJobs: Button
    private lateinit var btnSpairParts: Button
    private lateinit var btnSendReminder: Button
    private lateinit var btnGenerateReport: Button
    private lateinit var btnArchives: Button
    private lateinit var btnAttendance: Button
    private lateinit var btnWarrrantyStatus: Button
    private lateinit var btnOrderSpairParts: Button
    private lateinit var btnWriteMail: Button
    private lateinit var btnStatistics: Button
    private lateinit var btnCustomerList: Button
    private lateinit var btnStartEndDay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_dashboard_admin)

        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)

        checkUser()

        // Initialize buttons
        LogOut = findViewById(R.id.logoutButton)
        Profile = findViewById(R.id.profileBtn)

        cardNewJob = findViewById(R.id.cardNewJob)
        cardJobsCompleted = findViewById(R.id.cardJobsCompleted)
        btnWorkingJobs = findViewById(R.id.btnWorkingJobs)
        btnPendingJobs = findViewById(R.id.btnPendingJobs)
        btnCompletedJobs = findViewById(R.id.btnCompletedJobs)
        btnSpairParts = findViewById(R.id.btnSpairParts)
        btnSendReminder = findViewById(R.id.btnSendReminder)
        btnGenerateReport = findViewById(R.id.btnGenerateReport)
        btnArchives = findViewById(R.id.btnArchives)
        btnAttendance = findViewById(R.id.btnAttendance)
        btnWarrrantyStatus = findViewById(R.id.btnWarrrantyStatus)

        btnOrderSpairParts = findViewById(R.id.btnOrderSpairParts)
        btnWriteMail = findViewById(R.id.btnWriteMail)
        btnStatistics = findViewById(R.id.btnStatistics)
        btnCustomerList = findViewById(R.id.btnCustomerList)
        btnStartEndDay = findViewById(R.id.btnStartEndDay)

        //Logouts the page and opens start page
        LogOut.setOnClickListener {
            // Clear Firebase session
            FirebaseAuth.getInstance().signOut()

            // Clear SharedPreferences (stored phone number)
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()

            // Navigate to Start Screen
            val intent = Intent(this, Start_Screen_Activity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // opens the Profile page
        Profile.setOnClickListener {
            startActivity(Intent(this,Profile_Activity::class.java))
        }

        // Navigate to New Job Page
        cardNewJob.setOnClickListener {
            startActivity(Intent(this, New_Job_Activity::class.java))
        }

        cardJobsCompleted.setOnClickListener {
            startActivity(Intent(this, Jobs_List_Activity::class.java))
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
            Toast.makeText(this, "Start/End Day Clicked: Wait for app Update", Toast.LENGTH_SHORT).show()
        }

        btnAttendance.setOnClickListener {
            startActivity(Intent(this, Attendance_Activity::class.java))
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

        btnStartEndDay.setOnClickListener {
            val db = FirebaseFirestore.getInstance()
            val user = "System" // Replace with actual logged-in user if needed
            val itemCode = "SP001"
            val itemName = "MCR Controller"
            val price = "450"
            val quantity = "2 kg"
            val orderId = "ORD123"

            // Format current date-time as DD/MM/YYYY HH:MM AM/PM
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
            val currentDateTime = sdf.format(java.util.Date())

            // === ✅ 1. Create Spare_Parts_Prices ===
            db.collection("Spare_Parts_Prices").document(itemCode).set(
                mapOf(
                    "itemName" to itemName,
                    "price" to price,
                    "lastUpdatedBy" to user,
                    "lastUpdatedOn" to currentDateTime
                )
            )

            // === ✅ 2. Create Spare_Parts_Price_History ===
            val priceHistoryId = "${itemCode}_SPH_$currentDateTime"
            db.collection("Spare_Parts_Price_History").document(priceHistoryId).set(
                mapOf(
                    "itemCode" to itemCode,
                    "itemName" to itemName,
                    "previousPrice" to "400", // example
                    "newPrice" to price,
                    "updatedBy" to user,
                    "updatedOn" to currentDateTime
                )
            )

            // === ✅ 3. Create Spare_Parts_Stock ===
            db.collection("Spare_Parts_Stock").document(itemCode).set(
                mapOf(
                    "itemName" to itemName,
                    "stockQuantity" to quantity, // Store as String to support units
                    "lastUpdatedBy" to user,
                    "lastUpdatedOn" to currentDateTime
                )
            )

            // === ✅ 4. Create Spare_Parts_Orders ===
            db.collection("Spare_Parts_Orders").document(orderId).set(
                mapOf(
                    "orderedBy" to user,
                    "orderedOn" to currentDateTime,
                    "status" to "Pending",
                    "itemsOrdered" to listOf(
                        mapOf(
                            "itemCode" to itemCode,
                            "itemName" to itemName,
                            "quantity" to quantity
                        )
                    )
                )
            )

            // === ✅ 5. Create Spare_Parts_Order_History ===
            db.collection("Spare_Parts_Order_History").document().set(
                mapOf(
                    "orderId" to orderId,
                    "itemCode" to itemCode,
                    "itemName" to itemName,
                    "orderedQty" to quantity,
                    "receivedQty" to "0",
                    "confirmedBy" to "",
                    "confirmationDate" to "",
                    "remarks" to "Waiting for delivery"
                )
            )

            Toast.makeText(this, "All collections initialized!", Toast.LENGTH_SHORT).show()
        }

        btnStartEndDay.setOnClickListener {
            Toast.makeText(this, "Start/End Day Clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkUser() {
        // Get phone number from SharedPreferences
        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("phone", null)

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not available!", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("Users").document(phoneNumber)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role")
                    val name = document.getString("fullName")

                    if (role == "Admin") {
                        RoleTitle.text = "$role: $name"
                        Log.d("DashboardDebug", "Fetched role: $role and name: $name")
                    } else {
                        Toast.makeText(this, "Access Denied: Role = $role", Toast.LENGTH_SHORT).show()
                        auth.signOut()
                        startActivity(Intent(this, Start_Screen_Activity::class.java))
                        finish()
                    }

                } else {
                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, Start_Screen_Activity::class.java))
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user data!", Toast.LENGTH_SHORT).show()
                Log.e("DashboardError", "Firestore error: ${it.message}")
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Start_Screen_Activity::class.java))
                finish()
            }
    }
}