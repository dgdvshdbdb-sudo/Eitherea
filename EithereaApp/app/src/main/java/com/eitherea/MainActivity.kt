package com.eitherea

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        database = FirebaseDatabase.getInstance().getReference("commands")

        listenForEithereaCommands()
    }

    private fun listenForEithereaCommands() {
        val query = database.orderByChild("status").equalTo("pending").limitToLast(1)
        
        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (commandSnapshot in snapshot.children) {
                    val command = commandSnapshot.child("command").getValue(String::class.java)
                    val payload = commandSnapshot.child("payload").getValue(String::class.java)
                    val key = commandSnapshot.key
                    
                    if (command != null && key != null) {
                        statusText.text = "Executing:\n$command -> $payload"
                        executePhoneAction(command, payload ?: "")
                        database.child(key).child("status").setValue("done")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Database Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun executePhoneAction(command: String, payload: String) {
        when (command) {
            "FLASHLIGHT" -> {
                val turnOn = payload == "ON"
                toggleFlashlight(turnOn)
            }
            "OPEN_APP" -> openSpecificApp(payload)
            "SEND_WHATSAPP" -> {
                Toast.makeText(this, "Message Action Triggered: $payload", Toast.LENGTH_LONG).show()
            }
            else -> statusText.text = "Unknown Command: $command"
        }
    }

    private fun toggleFlashlight(turnOn: Boolean) {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, turnOn)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Camera access issue", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSpecificApp(appName: String) {
        try {
            if (appName.lowercase().contains("whatsapp")) {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (intent != null) startActivity(intent)
            } else if (appName.lowercase().contains("youtube")) {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
