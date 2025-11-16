package com.example.alienshot.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.alienshot.R
import com.example.alienshot.workers.ImageProcessingWorker
import androidx.work.*
import java.io.File

class PhotoWatcherService : Service() {

    private val TAG = "PhotoWatcherService"
    private val watchPath = "/storage/emulated/0/DCIM/Imaging Edge Mobile"

    private lateinit var observer: FileObserver

    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d(TAG, "═══════════════════════════════════════════")
            Log.d(TAG, "Service créé - Démarrage de la surveillance")
            Log.d(TAG, "Dossier surveillé: $watchPath")
            
            startForegroundService()
            Log.d(TAG, "✓ Service foreground démarré")
            
            // Tester différents chemins possibles
        val possiblePaths = listOf(
            "/storage/emulated/0/DCIM/Imaging Edge Mobile",
            "/storage/emulated/0/DCIM/ImagingEdgeMobile",
            "/sdcard/DCIM/Imaging Edge Mobile",
            "/mnt/sdcard/DCIM/Imaging Edge Mobile"
        )
        
        possiblePaths.forEach { path ->
            val testDir = File(path)
            Log.d(TAG, "Test chemin: $path")
            Log.d(TAG, "  Existe: ${testDir.exists()}")
            Log.d(TAG, "  Est dossier: ${testDir.isDirectory}")
            Log.d(TAG, "  Peut lire: ${testDir.canRead()}")
            if (testDir.exists()) {
                Log.d(TAG, "  Nombre de fichiers: ${testDir.listFiles()?.size ?: "null"}")
            }
        }
        Log.d(TAG, "═══════════════════════════════════════════")
        
        // Vérifier si le dossier existe
        val watchDir = File(watchPath)
        if (watchDir.exists() && watchDir.isDirectory) {
            Log.d(TAG, "✓ Le dossier existe et est accessible")
            
            val allFiles = watchDir.listFiles()
            Log.d(TAG, "Nombre de fichiers actuels: ${allFiles?.size ?: 0}")
            
            // Lister TOUS les fichiers avec détails
            allFiles?.forEach { file ->
                Log.d(TAG, "═══════════════════════════════")
                Log.d(TAG, "Nom complet: ${file.name}")
                Log.d(TAG, "Extension brute: '${file.extension}'")
                Log.d(TAG, "Extension lowercase: '${file.extension.lowercase()}'")
                Log.d(TAG, "Est un fichier: ${file.isFile}")
                Log.d(TAG, "Taille: ${file.length()} bytes")
                Log.d(TAG, "Chemin absolu: ${file.absolutePath}")
                
                if (file.extension.lowercase() == "jpg" || file.extension.lowercase() == "jpeg" && file.isFile && file.length() > 0) {
                    Log.d(TAG, "✓✓✓ IMAGE JPG/JPEG CONFIRMÉE ✓✓✓")
                    Log.d(TAG, ">>> TRAITEMENT DE L'IMAGE <<<")
                    enqueuePhotoProcessing(file.absolutePath)
                } else {
                    Log.d(TAG, "✗ Ignoré (pas une image JPG/JPEG valide ou fichier vide)")
                }
            }
            Log.d(TAG, "═══════════════════════════════")
        } else {
            Log.e(TAG, "✗ ERREUR: Le dossier n'existe pas ou n'est pas accessible!")
            Log.e(TAG, "Vérifiez les permissions de stockage")
        }

        observer = object : FileObserver(watchPath, CREATE or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                Log.d(TAG, "Événement détecté - Type: $event, Fichier: $path")
                
                // Ignorer les fichiers temporaires .pending-
                if (path?.startsWith(".pending-") == true) {
                    Log.d(TAG, "Fichier temporaire ignoré (.pending)")
                    return
                }
                
                if ((event == CREATE || event == MOVED_TO) && (path?.endsWith(".jpg", ignoreCase = true) == true || path?.endsWith(".jpeg", ignoreCase = true) == true)) {
                    val fullPath = "$watchPath/$path"
                    Log.d(TAG, "✓ Nouvelle image JPG/JPEG détectée: $fullPath")
                    
                    // Attendre un peu que le fichier soit complètement écrit
                    Thread.sleep(500)
                    
                    enqueuePhotoProcessing(fullPath)
                } else {
                    Log.d(TAG, "Événement ignoré (pas une image JPG/JPEG), fichier: $path")
                }
            }
        }
        observer.startWatching()
        Log.d(TAG, "FileObserver activé - En attente de nouvelles images...")
        
        } catch (e: Exception) {
            Log.e(TAG, "✗✗✗ ERREUR FATALE LORS DU DÉMARRAGE DU SERVICE ✗✗✗", e)
            Log.e(TAG, "Message: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun enqueuePhotoProcessing(path: String) {
        Log.d(TAG, "Envoi de l'image au Worker pour traitement: $path")
        
        val work = OneTimeWorkRequestBuilder<ImageProcessingWorker>()
            .setInputData(workDataOf("IMAGE_PATH" to path))
            .build()

        WorkManager.getInstance(applicationContext).enqueue(work)
        Log.d(TAG, "Tâche de traitement mise en file d'attente avec ID: ${work.id}")
    }

    override fun onDestroy() {
        super.onDestroy()
        observer.stopWatching()
        Log.d(TAG, "Service arrêté - Surveillance terminée")
    }

    private fun startForegroundService() {
        val channelId = "PhotoWatcherChannel"
        val channelName = "Photo Watcher"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Surveillance des nouvelles photos")
            .setContentText("Le service est en cours d'exécution")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
