package com.deepster.messenger.auth

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import android.util.Log
import android.widget.Toast
import com.deepster.messenger.message.LatestMessagesActivity
import com.deepster.messenger.R
import com.deepster.messenger.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*

class RegisterActivity : AppCompatActivity() {

    private val TAG = javaClass.canonicalName
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        register_button_register.setOnClickListener {
            performRegister()
        }

        already_have_account_textview.setOnClickListener {
            Log.d(javaClass.toString(), "Try to show login activity")

            // Launch the login activity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        select_photo_button_register.setOnClickListener {
            Log.d(TAG, "Try to show photo selector")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
    }

    var selectedPhotoURI: Uri? = null

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            // Check what the selected image was
            Log.d(TAG, "Photo was selected")

            selectedPhotoURI = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoURI)

            select_photo_imageview_register.setImageBitmap(bitmap) // Show the avatar
            select_photo_button_register.alpha = 0.0f // Hide the button
        }
    }

    private fun performRegister() {
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter text in Email/Pw fields", Toast.LENGTH_SHORT).show()
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully created user with uid: ${it.user.uid}")
                uploadImage()
            }.addOnFailureListener { exception ->
                Log.d(TAG, "Failed to create user: ${exception.message}")
            }
    }

    private fun uploadImage() {
        val fileName = UUID.randomUUID().toString()
        val reference = storage.getReference("/images/$fileName")

        reference.putFile(selectedPhotoURI!!).addOnSuccessListener {
            Log.d(TAG, "Successfully uploaded image: ${it.metadata?.path}")
            reference.downloadUrl.addOnSuccessListener { imageURL ->
                Log.d(TAG, "File location: $imageURL")
                saveUserToDatabase(imageURL.toString())
            }
        }
    }

    private fun saveUserToDatabase(imageURL: String) {
        val uid = auth.uid ?: ""
        println(uid)
        val username = username_editText_register.text.toString()
        val reference = database.getReference("/users/$uid")

        println(reference.key)

        val user = User(uid, username, imageURL)

        reference.setValue(user).addOnSuccessListener {
            Log.d(TAG, "Added $uid to the database")

            val intent = Intent(baseContext, LatestMessagesActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }.addOnFailureListener {
            Log.d(TAG, "Couldn't add $uid to the database")
        }
    }
}


