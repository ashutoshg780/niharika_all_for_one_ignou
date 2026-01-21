package com.example.niharika_all_for_one.network

import android.content.Context
import android.content.SharedPreferences

/**
 * AppPreferences - Central place to store user/session data
 * using Android SharedPreferences.
 *
 * This avoids calling Firebase repeatedly.
 */
class AppPreferences(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = preferences.edit()

    // 🔹 Keys for our values
    private val IS_LOGIN = "is_login"
    private val USER_ID = "user_id"
    private val ROLE = "user_role"
    private val NAME = "user_name"
    private val PHONE = "user_phone"
    private val EMAIL = "user_email"
    private val STATUS = "user_status"
    private val PHOTO_URL = "photo_url" // optional for profile pic

    // --- Login flag ---
    fun setIsLogin(value: Boolean) {
        editor.putBoolean(IS_LOGIN, value).apply()
    }
    fun getIsLogin(): Boolean = preferences.getBoolean(IS_LOGIN, false)

    // --- UserId ---
    fun setUserId(value: String?) {
        editor.putString(USER_ID, value).apply()
    }
    fun getUserId(): String? = preferences.getString(USER_ID, null)

    // --- Role ---
    fun setUserRole(value: String?) {
        editor.putString(ROLE, value).apply()
    }
    fun getUserRole(): String? = preferences.getString(ROLE, null)

    // --- Name ---
    fun setName(value: String?) {
        editor.putString(NAME, value).apply()
    }
    fun getName(): String? = preferences.getString(NAME, null)

    // --- Phone ---
    fun setPhone(value: String?) {
        editor.putString(PHONE, value).apply()
    }
    fun getPhone(): String? = preferences.getString(PHONE, null)

    // --- Email ---
    fun setEmail(value: String?) {
        editor.putString(EMAIL, value).apply()
    }
    fun getEmail(): String? = preferences.getString(EMAIL, null)

    // --- Status ---
    fun setStatus(value: String?) {
        editor.putString(STATUS, value).apply()
    }
    fun getStatus(): String? = preferences.getString(STATUS, null)

    // --- Profile Photo URL (if uploaded) ---
    fun setPhotoUrl(value: String?) {
        editor.putString(PHOTO_URL, value).apply()
    }
    fun getPhotoUrl(): String? = preferences.getString(PHOTO_URL, null)

    // --- Clear all (on logout) ---
    fun clearPreferences() {
        editor.clear()
        editor.apply()
    }
}
