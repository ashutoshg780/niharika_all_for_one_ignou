package com.example.niharika_all_for_one.ui.theme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Example Button Click (Optional)
        binding.textView.setOnClickListener {
            binding.textView.text = "Welcome to Niharika All For One"
        }
    }
}
