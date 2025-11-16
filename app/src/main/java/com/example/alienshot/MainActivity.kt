package com.example.alienshot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.alienshot.services.PhotoWatcherService

class MainActivity : ComponentActivity() {
    
    private val TAG = "MainActivity"
    
    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        
        if (allGranted) {
            Log.d(TAG, "✓ Toutes les permissions accordées")
            startPhotoService()
        } else {
            Log.e(TAG, "✗ Permissions refusées")
            Toast.makeText(
                this,
                "Les permissions sont nécessaires pour surveiller les photos",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Vérification des permissions...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ : Vérifier MANAGE_EXTERNAL_STORAGE
            if (Environment.isExternalStorageManager()) {
                Log.d(TAG, "✓ MANAGE_EXTERNAL_STORAGE accordée")
                startPhotoService()
                finish()
            } else {
                Log.e(TAG, "✗ MANAGE_EXTERNAL_STORAGE manquante")
                Toast.makeText(
                    this,
                    "Veuillez autoriser l'accès à tous les fichiers dans les paramètres",
                    Toast.LENGTH_LONG
                ).show()
                
                // Ouvrir les paramètres pour autoriser l'accès
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
                finish()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : Demander READ_MEDIA_IMAGES
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            if (checkPermissions(permissions)) {
                Log.d(TAG, "✓ Permissions déjà accordées")
                startPhotoService()
                finish()
            } else {
                Log.d(TAG, "Demande de permissions...")
                requestPermissions.launch(permissions)
            }
        } else {
            // Android 12 et inférieur
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            if (checkPermissions(permissions)) {
                Log.d(TAG, "✓ Permissions déjà accordées")
                startPhotoService()
                finish()
            } else {
                Log.d(TAG, "Demande de permissions...")
                requestPermissions.launch(permissions)
            }
        }
    }
    
    private fun checkPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun startPhotoService() {
        Log.d(TAG, "Démarrage du service de surveillance...")
        startService(Intent(this, PhotoWatcherService::class.java))
    }
}