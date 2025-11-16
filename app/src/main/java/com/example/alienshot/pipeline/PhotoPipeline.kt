package com.example.alienshot.pipeline

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo

object PhotoPipeline {

    /**
     * Pipeline de base pour préparer l'image
     */
    fun process(src: Mat): Mat {
        var img = src.clone()
        
        // Traitement de base léger
        img = denoise(img)
        img = whiteBalance(img)
        img = autoLevel(img)
        
        return img
    }

    /**
     * Applique le filtre Ollie (vintage doux, tons chauds)
     */
    fun applyOllieFilter(src: Mat): Mat {
        var img = src.clone()
        
        // Base: denoise + balance
        img = denoise(img)
        img = whiteBalance(img)
        
        // Tons chauds: augmenter les teintes orangées
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)
        val channels = ArrayList<Mat>()
        Core.split(hsv, channels)
        
        // Saturation modérée
        Core.multiply(channels[1], Scalar(1.15), channels[1])
        
        Core.merge(channels, hsv)
        Imgproc.cvtColor(hsv, img, Imgproc.COLOR_HSV2BGR)
        
        // Contraste doux
        img.convertTo(img, -1, 1.1, 5.0)
        
        // Légère teinte chaude
        val warmTint = Mat.zeros(img.size(), img.type())
        warmTint.setTo(Scalar(5.0, 10.0, 15.0)) // BGR: plus de rouge/jaune
        Core.add(img, warmTint, img)
        
        return img
    }

    /**
     * Applique le filtre Eiffel (dramatique, contrastes élevés)
     */
    fun applyEiffelFilter(src: Mat): Mat {
        var img = src.clone()
        
        // Base
        img = denoise(img)
        
        // CLAHE agressif pour contraste dramatique
        val lab = Mat()
        Imgproc.cvtColor(img, lab, Imgproc.COLOR_BGR2Lab)
        val channels = ArrayList<Mat>()
        Core.split(lab, channels)
        
        val clahe = Imgproc.createCLAHE()
        clahe.clipLimit = 3.0  // Plus élevé pour effet dramatique
        clahe.apply(channels[0], channels[0])
        
        Core.merge(channels, lab)
        Imgproc.cvtColor(lab, img, Imgproc.COLOR_Lab2BGR)
        
        // Contraste élevé
        img.convertTo(img, -1, 1.3, -10.0)
        
        // Saturation augmentée
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)
        val hsvChannels = ArrayList<Mat>()
        Core.split(hsv, hsvChannels)
        Core.multiply(hsvChannels[1], Scalar(1.20), hsvChannels[1])
        Core.merge(hsvChannels, hsv)
        Imgproc.cvtColor(hsv, img, Imgproc.COLOR_HSV2BGR)
        
        // Sharpen pour détails
        img = sharpen(img)
        
        return img
    }

    /**
     * Applique le filtre Reel (vintage cinématographique)
     */
    fun applyReelFilter(src: Mat): Mat {
        var img = src.clone()
        
        // Base
        img = denoise(img)
        
        // Tons désaturés légèrement
        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)
        val channels = ArrayList<Mat>()
        Core.split(hsv, channels)
        
        // Réduire légèrement la saturation pour effet vintage
        Core.multiply(channels[1], Scalar(0.85), channels[1])
        
        Core.merge(channels, hsv)
        Imgproc.cvtColor(hsv, img, Imgproc.COLOR_HSV2BGR)
        
        // Contraste modéré
        img.convertTo(img, -1, 1.15, 0.0)
        
        // Vignettage subtil (assombrir les bords)
        val rows = img.rows()
        val cols = img.cols()
        val centerX = cols / 2.0
        val centerY = rows / 2.0
        val maxDistance = Math.sqrt(centerX * centerX + centerY * centerY)
        
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val distance = Math.sqrt(
                    Math.pow(j - centerX, 2.0) + Math.pow(i - centerY, 2.0)
                )
                val vignette = 1.0 - (distance / maxDistance * 0.4)
                
                val pixel = img.get(i, j)
                pixel[0] = (pixel[0] * vignette).coerceIn(0.0, 255.0)
                pixel[1] = (pixel[1] * vignette).coerceIn(0.0, 255.0)
                pixel[2] = (pixel[2] * vignette).coerceIn(0.0, 255.0)
                img.put(i, j, *pixel)
            }
        }
        
        // Teinte légèrement sépia
        val sepiaTint = Mat.zeros(img.size(), img.type())
        sepiaTint.setTo(Scalar(10.0, 15.0, 20.0))
        Core.add(img, sepiaTint, img)
        
        return img
    }

    /**
     * Débruitage léger pour commencer avec une base propre
     */
    private fun denoise(src: Mat): Mat {
        val dst = Mat()
        // Paramètres plus légers : h=3 au lieu de 5 pour garder plus de détails
        Photo.fastNlMeansDenoisingColored(src, dst, 3f, 3f, 7, 21)
        return dst
    }

    /**
     * Balance des blancs avec CLAHE moins agressif
     */
    private fun whiteBalance(src: Mat): Mat {
        val lab = Mat()
        Imgproc.cvtColor(src, lab, Imgproc.COLOR_BGR2Lab)
        val channels = ArrayList<Mat>()
        Core.split(lab, channels)

        val clahe = Imgproc.createCLAHE()
        clahe.clipLimit = 1.5  // Moins agressif que 2.5
        clahe.apply(channels[0], channels[0])

        Core.merge(channels, lab)
        val result = Mat()
        Imgproc.cvtColor(lab, result, Imgproc.COLOR_Lab2BGR)
        return result
    }

    /**
     * Auto-level plus doux pour éviter le clipping
     */
    private fun autoLevel(src: Mat): Mat {
        val dst = Mat()
        src.convertTo(dst, CvType.CV_32F)
        Core.normalize(dst, dst, 0.0, 1.0, Core.NORM_MINMAX)
        dst.convertTo(dst, CvType.CV_8U, 255.0)
        return dst
    }

    /**
     * Super-Resolution moins agressive (x1.2 au lieu de x1.5)
     */
    private fun superResolution(src: Mat): Mat {
        val dst = Mat()
        val newSize = Size(src.width() * 1.2, src.height() * 1.2)
        Imgproc.resize(src, dst, newSize, 0.0, 0.0, Imgproc.INTER_CUBIC)
        return dst
    }

    /**
     * Sharpen léger pour un rendu naturel
     */
    private fun sharpen(src: Mat): Mat {
        val kernel = Mat.ones(3, 3, CvType.CV_32F)
        Core.multiply(kernel, Scalar(-0.1), kernel)
        kernel.put(1, 1, 1.8)  // Centre légèrement renforcé
        
        val dst = Mat()
        Imgproc.filter2D(src, dst, src.depth(), kernel)
        return dst
    }

    /**
     * Boost saturation très léger (+5% au lieu de +10%)
     */
    private fun boostSaturation(src: Mat): Mat {
        val hsv = Mat()
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGR2HSV)

        val channels = ArrayList<Mat>()
        Core.split(hsv, channels)

        Core.multiply(channels[1], Scalar(1.05), channels[1])  // +5% seulement

        Core.merge(channels, hsv)

        val dst = Mat()
        Imgproc.cvtColor(hsv, dst, Imgproc.COLOR_HSV2BGR)
        return dst
    }
}
