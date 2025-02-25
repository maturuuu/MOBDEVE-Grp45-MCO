package com.mobdeve_group45_mco

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve_group45_mco.databinding.ActivityCreatePostBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class CreatePost : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var postManager: PostManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postManager = PostManager()

        val city = intent.getStringExtra("CITY")
        val countryCode = intent.getStringExtra("COUNTRY_CODE")

        binding.btnSubmitPost.setOnClickListener {
            val postContent = binding.etPostContent.text.toString().trim()
            if (postContent.isNotEmpty()) {
                if (countryCode != null && city != null) {
                    postManager.createPost(
                        postContent, city, countryCode,
                        onSuccess = {
                            Toast.makeText(this, "Post submitted successfully!", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onFailure = { errorMsg ->
                            Toast.makeText(this, "Failed to submit post: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(this, "City and/or country code is missing", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please write something!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


