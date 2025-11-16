package com.example.alienshot.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import com.example.alienshot.pipeline.PhotoPipeline
import java.io.File

class ImageProcessingWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val TAG = "ImageProcessingWorker"
    private val EDITED_DIR = "/storage/emulated/0/DCIM/alienshotEdited"
    private val RAW_DIR = "/storage/emulated/0/DCIM/alienshotRaw"

    override fun doWork(): Result {
        val path = inputData.getString("IMAGE_PATH")
        
        if (path == null) {
            Log.e(TAG, "✗ ERREUR: Chemin d'image null")
            return Result.failure()
        }
        
        Log.d(TAG, "Début du traitement de l'image: $path")
        
        // Vérifier si le fichier existe
        val file = File(path)
        if (!file.exists()) {
            Log.e(TAG, "✗ ERREUR: Le fichier n'existe pas: $path")
            return Result.failure()
        }
        
        val fileSize = file.length()
        Log.d(TAG, "Taille du fichier: ${fileSize / 1024} KB")
        
        // Vérifier que le fichier n'est pas vide ou trop petit
        if (fileSize < 1024) {
            Log.e(TAG, "✗ ERREUR: Fichier trop petit ou vide (< 1KB), probablement en cours de transfert")
            return Result.failure()
        }

        Log.d(TAG, "Lecture de l'image avec OpenCV...")
        val src = Imgcodecs.imread(path)
        
        if (src.empty()) {
            Log.e(TAG, "✗ ERREUR: Impossible de lire l'image avec OpenCV")
            return Result.failure()
        }
        
        Log.d(TAG, "✓ Image chargée: ${src.width()}x${src.height()}")
        
        // Créer les dossiers de destination s'ils n'existent pas
        val editedDir = File(EDITED_DIR)
        val rawDir = File(RAW_DIR)
        
        if (!editedDir.exists()) {
            Log.d(TAG, "Création du dossier: $EDITED_DIR")
            editedDir.mkdirs()
        }
        
        if (!rawDir.exists()) {
            Log.d(TAG, "Création du dossier: $RAW_DIR")
            rawDir.mkdirs()
        }

        val fileName = file.nameWithoutExtension
        val extension = file.extension
        var successCount = 0
        
        // Appliquer les 3 filtres Google Photos
        val filters = listOf(
            "Ollie" to PhotoPipeline::applyOllieFilter,
            "Eiffel" to PhotoPipeline::applyEiffelFilter,
            "Reel" to PhotoPipeline::applyReelFilter
        )
        
        filters.forEach { (filterName, filterFunction) ->
            try {
                Log.d(TAG, "Application du filtre $filterName...")
                val filtered = filterFunction(src)
                
                val outputPath = "$EDITED_DIR/$fileName-$filterName.$extension"
                Log.d(TAG, "Enregistrement: $outputPath")
                
                val success = Imgcodecs.imwrite(outputPath, filtered)
                
                if (success) {
                    val outputFile = File(outputPath)
                    Log.d(TAG, "✓ Filtre $filterName sauvegardé (${outputFile.length() / 1024} KB)")
                    successCount++
                } else {
                    Log.e(TAG, "✗ Échec sauvegarde filtre $filterName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Erreur filtre $filterName: ${e.message}")
            }
        }
        
        Log.d(TAG, "Résumé: $successCount/3 filtres appliqués avec succès")
        
        if (successCount > 0) {
            // Déplacer l'image originale vers alienshotRaw
            val rawPath = "$RAW_DIR/${file.name}"
            Log.d(TAG, "Déplacement de l'original vers: $rawPath")
            
            val moved = file.renameTo(File(rawPath))
            if (moved) {
                Log.d(TAG, "✓ Image originale déplacée avec succès!")
            } else {
                Log.w(TAG, "⚠ Impossible de déplacer l'image originale")
            }
            
            return Result.success()
        } else {
            Log.e(TAG, "✗ ERREUR: Aucun filtre n'a pu être appliqué")
            return Result.failure()
        }
    }
}
