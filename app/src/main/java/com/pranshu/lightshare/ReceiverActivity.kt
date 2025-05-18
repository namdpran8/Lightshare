package com.pranshu.lightshare

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import java.io.ByteArrayOutputStream
import kotlin.math.pow
import kotlin.math.sqrt

class ReceiverActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var outputTextView: TextView

    private val TAG = "ReceiverActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        previewView = PreviewView(this)
        outputTextView = TextView(this).apply {
            textSize = 18f
            setPadding(16, 16, 16, 16)
        }

        val layout = FrameLayout(this).apply {
            addView(previewView)
            addView(outputTextView)
        }

        setContentView(layout)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), ColorGridAnalyzer())

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private inner class ColorGridAnalyzer : ImageAnalysis.Analyzer {
        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run {
                imageProxy.close()
                return
            }

            val bitmap = mediaImage.toBitmap() ?: run {
                imageProxy.close()
                return
            }

            val bytes = analyzeImage(bitmap)

            runOnUiThread {
                val text = bytes.toString(Charsets.UTF_8)
                outputTextView.text = "Received: $text"
            }

            imageProxy.close()
        }

        private fun analyzeImage(bitmap: Bitmap): ByteArray {
            val gridSize = 16
            val cellWidth = bitmap.width / gridSize
            val cellHeight = bitmap.height / gridSize

            val bitList = mutableListOf<Int>()

            for (i in 0 until gridSize) {
                for (j in 0 until gridSize) {
                    val x = j * cellWidth + cellWidth / 2
                    val y = i * cellHeight + cellHeight / 2
                    val pixelColor = averageColor(bitmap, x, y)

                    val bits = colorToBits(pixelColor)
                    bitList.add(bits)
                }
            }

            val output = ByteArrayOutputStream()
            for (i in bitList.indices step 4) {
                if (i + 3 >= bitList.size) break
                val byte =
                    (bitList[i] shl 6) or (bitList[i + 1] shl 4) or (bitList[i + 2] shl 2) or bitList[i + 3]
                output.write(byte)
            }

            return output.toByteArray()
        }
        private fun colorToBits(color: Int): Int {
            val colors = listOf(
                Color.BLACK to 0b00,
                Color.RED to 0b01,
                Color.GREEN to 0b10,
                Color.BLUE to 0b11
            )

            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)

            fun distance(c: Int): Double {
                val cr = Color.red(c)
                val cg = Color.green(c)
                val cb = Color.blue(c)
                return sqrt(((r - cr).toDouble().pow(2)) + ((g - cg).toDouble().pow(2)) + ((b - cb).toDouble().pow(2)))
            }

            return colors.minByOrNull { distance(it.first) }?.second ?: 0b00
        }

        private fun averageColor(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int = 1): Int {
            var r = 0
            var g = 0
            var b = 0
            var count = 0

            for (dx in -radius..radius) {
                for (dy in -radius..radius) {
                    val x = (centerX + dx).coerceIn(0, bitmap.width - 1)
                    val y = (centerY + dy).coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(x, y)
                    r += Color.red(pixel)
                    g += Color.green(pixel)
                    b += Color.blue(pixel)
                    count++
                }
            }

            return Color.rgb(r / count, g / count, b / count)
        }


        private fun Image.toBitmap(): Bitmap? {
            return when (format) {
                ImageFormat.YUV_420_888 -> {
                    // YUV -> Bitmap logic
                    val yBuffer = planes[0].buffer
                    val uBuffer = planes[1].buffer
                    val vBuffer = planes[2].buffer

                    val ySize = yBuffer.remaining()
                    val uSize = uBuffer.remaining()
                    val vSize = vBuffer.remaining()

                    val nv21 = ByteArray(ySize + uSize + vSize)
                    yBuffer.get(nv21, 0, ySize)
                    vBuffer.get(nv21, ySize, vSize)
                    uBuffer.get(nv21, ySize + vSize, uSize)

                    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
                    val out = ByteArrayOutputStream()
                    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
                    val imageBytes = out.toByteArray()
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }

                ImageFormat.UNKNOWN, ImageFormat.PRIVATE -> null

                else -> {
                    // RGBA_8888
                    val buffer = planes[0].buffer
                    buffer.rewind()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)
                    bitmap
                }
            }
        }

    }
}