package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Jobs_List_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: JobAdapter
    private val jobList = mutableListOf<Job>()

    private var currentUserRole = ""
    private var currentUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jobs_list)
        auth = FirebaseAuth.getInstance()

        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        recyclerView = findViewById(R.id.recyclerViewJobs)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = JobAdapter(jobList) { job ->
            // ✅ Start Job_Status_Activity and pass jobId
            val intent = Intent(this, Job_Status_Activity::class.java)
            intent.putExtra("jobId", job.jobId)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        checkUser()

//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)  // ✅ Use AppPreferences
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // Check logged-in user details and set current role & name
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null)
//        if (phoneNumber.isNullOrEmpty()) {
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
//
//        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
//            .addOnSuccessListener { document ->
//                if (document.exists()) {
//                    currentUserRole = document.getString("role") ?: ""
//                    currentUserName = document.getString("fullName") ?: ""
//                    RoleTitle.text = "$currentUserRole: $currentUserName"
//
//                    // After user details loaded, load jobs
//                    loadJobs()
//                } else {
//                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
//                    finish()
//                }
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Error fetching user: ${it.message}", Toast.LENGTH_SHORT).show()
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)  // ✅ Use AppPreferences
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUserRole = document.getString("role") ?: ""
                    currentUserName = document.getString("fullName") ?: ""
                    RoleTitle.text = "$currentUserRole: $currentUserName"
                    loadJobs()
                } else {
                    Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching user: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Load jobs from Firestore with role-based filtering
    private fun loadJobs() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Jobs")
            .orderBy("createdAtTimestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                jobList.clear()

                for (doc in result) {
                    val job = doc.toObject(Job::class.java)

                    // Check if the current user can view this job
                    if (canViewJob(job)) {
                        jobList.add(job)
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error fetching jobs: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Check if the current user can view the job based on role and assigned engineer
    private fun canViewJob(job: Job): Boolean {
        return when {
            // Roles that can view ALL jobs
            currentUserRole in listOf("Manager", "Admin", "Super Admin", "Owner") -> true

            // Engineer (Workshop) role can only see Workshop jobs assigned to them
            currentUserRole == "Engineer (Workshop)" &&
                    job.assignedEngineer == currentUserName &&
                    jobMatchesComplaintType(job, "Workshop") -> true

            // Engineer (Field) role can only see Field jobs assigned to them
            currentUserRole == "Engineer (Field)" &&
                    job.assignedEngineer == currentUserName &&
                    jobMatchesComplaintType(job, "Field") -> true

            // Any other case, user can't view the job
            else -> false
        }
    }

    // Helper to check job's complaint type matches engineer's role
    private fun jobMatchesComplaintType(job: Job, type: String): Boolean {
        val jobIdPrefix = job.jobId.take(3) // If you store complaintType separately, use that field instead
        return when (type) {
            "Workshop" -> jobIdPrefix == "JID" // Adjust if necessary
            "Field" -> jobIdPrefix == "JID" // Adjust if necessary
            else -> false
        }
    }
}