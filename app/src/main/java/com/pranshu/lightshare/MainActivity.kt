package com.pranshu.lightshare

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private val PICK_FILE_REQUEST = 1
    private lateinit var gridView: GridView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val sendButton = Button(this).apply {
            text = "Send (Test)"
            setOnClickListener {
                // For test: just encode "Hello"
                gridView.setDataTest()
            }
        }

        val fileSendButton = Button(this).apply {
            text = "Send File"
            setOnClickListener {
                openFilePicker()
            }
        }

        val receiveButton = Button(this).apply {
            text = "Receive"
            setOnClickListener {
                val intent = Intent(this@MainActivity, ReceiverActivity::class.java)
                startActivity(intent)
            }
        }

        gridView = GridView(this)
        layout.addView(sendButton)
        layout.addView(fileSendButton)
        layout.addView(receiveButton)
        layout.addView(gridView)

        setContentView(layout)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(Intent.createChooser(intent, "Select a file"), PICK_FILE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            if (uri != null) {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    gridView.setData(bytes)
                }
            }
        }
    }
}

class GridView(context: android.content.Context) : View(context) {
    private val gridSize = 16
    private val cellColors = Array(gridSize) { Array(gridSize) { Color.BLACK } }
    private val paint = Paint()
    private var data: ByteArray = ByteArray(0)

    init {
        // Start animation for idle state
        postDelayed({ updateGrid() }, 100)
    }

    fun setDataTest() {
        val testString = "Hello"
        val bytes = testString.toByteArray(Charsets.UTF_8)
        setData(bytes)
    }

    fun setData(newData: ByteArray) {
        Log.d("GridView", "Data size: ${newData.size}")
        data = newData
        generateDataGrid()
        invalidate()
    }

    private fun updateGrid() {
        if (data.isEmpty()) {
            generateRandomData()
            invalidate()
            postDelayed({ updateGrid() }, 100)
        }
    }

    private fun generateRandomData() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val bits = Random.nextInt(0, 4)
                cellColors[i][j] = bitsToColor(bits)
            }
        }
    }

    private fun generateDataGrid() {
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                val cellIndex = i * gridSize + j
                val byteIndex = cellIndex / 4
                val bitPairIndex = cellIndex % 4

                val bits = if (byteIndex < data.size) {
                    val byte = data[byteIndex].toInt() and 0xFF
                    val shift = 6 - (bitPairIndex * 2)
                    (byte shr shift) and 0b11
                } else {
                    0
                }

                cellColors[i][j] = bitsToColor(bits)
            }
        }
    }

    private fun bitsToColor(bits: Int): Int {
        return when (bits) {
            0b00 -> Color.BLACK
            0b01 -> Color.RED
            0b10 -> Color.GREEN
            0b11 -> Color.BLUE
            else -> Color.BLACK
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cellWidth = width.toFloat() / gridSize
        val cellHeight = height.toFloat() / gridSize

        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                paint.color = cellColors[i][j]
                canvas.drawRect(
                    j * cellWidth,
                    i * cellHeight,
                    (j + 1) * cellWidth,
                    i * cellHeight + cellHeight,
                    paint
                )
            }
        }
    }
}
