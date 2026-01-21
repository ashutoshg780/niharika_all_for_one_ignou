package com.example.niharika_all_for_one.utils

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.niharika_all_for_one.Profile_Activity
import com.example.niharika_all_for_one.Start_Screen_Activity
import com.example.niharika_all_for_one.R
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth

/**
 * HeaderManager - handles the common header across all screens.
 * It fetches user info from AppPreferences and wires the buttons.
 */
object HeaderManager {

    fun init(activity: Activity) {
        val appPrefs = AppPreferences(activity)

        // --- Header Views ---
        val profileBtn: ImageView = activity.findViewById(R.id.profileBtn)
        val logoutButton: ImageView = activity.findViewById(R.id.logoutButton)
        val title: TextView = activity.findViewById(R.id.title)
        val profileRole: TextView = activity.findViewById(R.id.profileRole)

        // --- Dynamic values from AppPreferences ---
        val userName = appPrefs.getName() ?: "User" // 📝 getting name here
        val role = appPrefs.getUserRole() ?: "Unknown Role" // 📝 showing role here
        val photoUrl = appPrefs.getPhotoUrl()

        // --- Update UI ---
        title.text = "NIHARIKA ENTERPRISES" // fixed company title
        profileRole.text = "$userName ($role)" // show name + role

        // --- Load Profile Image (if available) ---
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(activity)
                .load(photoUrl)
                .placeholder(R.drawable.placeholder_profile)
                .into(profileBtn)
        }

        // --- Profile button click ---
        profileBtn.setOnClickListener {
            activity.startActivity(Intent(activity, Profile_Activity::class.java))
        }

        // --- Logout button click ---
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            appPrefs.clearPreferences() // 📝 logout button clears prefs
            Toast.makeText(activity, "Logged out successfully", Toast.LENGTH_SHORT).show()
            activity.startActivity(Intent(activity, Start_Screen_Activity::class.java))
            activity.finish()
        }
    }
}
