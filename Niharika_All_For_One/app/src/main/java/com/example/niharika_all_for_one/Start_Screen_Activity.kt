package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class Start_Screen_Activity: AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnSignup: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_start_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnLogin = findViewById(R.id.btn_login)
        btnSignup = findViewById(R.id.btn_signup)


        // Set up button click listeners
        btnLogin.setOnClickListener {
            startActivity(Intent(this, Sign_in_Activity::class.java))
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, Sign_up_Activity::class.java))
        }
    }
}


