package com.mobdeve_group45_mco

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

// Data Model
data class Post(
    val post: String = "",
    val date: String = "",
    var name: String = "",
    val city: String = "",
    val countryCode: String = ""
)

// Facade class for handling post creation
class PostManager() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    @RequiresApi(Build.VERSION_CODES.O)
    fun createPost(postContent: String, city: String, countryCode: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val currentUser = auth.currentUser ?: return
        val postId = UUID.randomUUID().toString()


        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val post = Post(
                        post = postContent,
                        date = currentDate,
                        name = userDocument.getString("name") ?: "Unknown",
                        city = city,
                        countryCode = countryCode
                    )

                    db.collection("posts").document(postId).set(post)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e.message ?: "Unknown error") }
                } else {
                    onFailure("User document not found for uid: ${currentUser.uid}")
                }
            }
            .addOnFailureListener { e -> onFailure("Error fetching user: ${e.message}") }
    }

    //new function for fetching post data
    fun getPosts(city: String, countryCode: String, onSuccess: (List<Post>) -> Unit, onFailure: (String) -> Unit) {
        db.collection("posts")
            .whereEqualTo("city", city)
            .whereEqualTo("countryCode", countryCode)
            .get()
            .addOnSuccessListener { documents ->
                val posts = documents.mapNotNull { it.toObject(Post::class.java) }
                onSuccess(posts)
            }
            .addOnFailureListener { e -> onFailure("Error fetching posts: ${e.message}") }
    }
}

