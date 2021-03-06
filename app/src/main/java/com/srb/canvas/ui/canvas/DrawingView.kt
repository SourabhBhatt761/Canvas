package com.srb.canvas.ui.canvas

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.srb.canvas.utils.snackBarMsg
import timber.log.Timber
import java.io.OutputStream

class DrawingView (context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var canvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null

    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null

    private var drawPath: CustomPath? = null
    private var paths = ArrayList<CustomPath>()
    private var undonePaths = ArrayList<CustomPath>()

    private var brushSize: Float = 20F
    private var brushColor: Int = Color.BLACK

    lateinit var result: String
    lateinit var imageUri : Uri

    init {
        setUpDrawing()
    }

    // This method initializes the attributes of the View for DrawingView class.
    private fun setUpDrawing() {
        drawPaint = Paint()
        Timber.i(drawPaint.toString())
        drawPath = CustomPath(brushColor, brushSize)
        Timber.i(drawPath.toString())

        Timber.i(drawPath.toString())

        drawPaint!!.color = brushColor
        drawPaint!!.style = Paint.Style.STROKE
        drawPaint!!.strokeJoin = Paint.Join.ROUND
        drawPaint!!.strokeCap = Paint.Cap.ROUND

        canvasPaint = Paint(Paint.DITHER_FLAG)
        Timber.i(canvasPaint.toString())
        Timber.i(drawPaint.toString())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)

        Timber.i(canvas.toString())
        Timber.i(canvasBitmap.toString())
    }

    /**
     * This method is called when a stroke is drawn on the canvas
     * as a part of the painting.
     */
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas!!.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        for (path in paths) {
            drawPaint!!.strokeWidth = path.brushThickness
            drawPaint!!.color = path.color
            canvas.drawPath(path, drawPaint!!)

            Timber.i(canvas.toString())
        }

        if (!drawPath!!.isEmpty) {
            drawPaint!!.strokeWidth = drawPath!!.brushThickness
            drawPaint!!.color = drawPath!!.color
            canvas.drawPath(drawPath!!, drawPaint!!)

            Timber.i(canvas.toString())
        }
    }

    /**
     * This method acts as an event listener when a touch
     * event is detected on the device.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        Timber.i(touchX.toString(), touchY.toString() )

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = brushColor
                drawPath!!.brushThickness = brushSize
                // Clear any lines and curves from the path, making it empty.
                drawPath!!.reset()
                // Set the beginning of the next contour to the point (x,y).
                drawPath!!.moveTo(touchX, touchY)

                Timber.i(touchX.toString(), touchY.toString() )
                Timber.i(drawPath.toString())
            }
            MotionEvent.ACTION_MOVE -> {
                // Add a line from the last point to the specified point (x,y).
                drawPath!!.lineTo(touchX, touchY)

                Timber.i(drawPath.toString())
            }
            MotionEvent.ACTION_UP -> {
                paths.add(drawPath!!)
                drawPath = CustomPath(brushColor, brushSize)

                Timber.i(drawPath.toString())
            }
            else -> return false
        }

        invalidate()
        return true
    }

    fun setBrushSize(size: Float): Float {
        // Set size based on screen dimension.
        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            size,
            resources.displayMetrics
        )
        drawPaint!!.strokeWidth = brushSize

        return brushSize
    }

    fun setBrushColor(newColor: Int) {
        brushColor = newColor
        drawPaint!!.color = brushColor
    }

    fun undoPath() {
        when {
            paths.size > 0 -> {
                undonePaths.add(paths.removeAt(paths.size - 1))
                invalidate()
            }
        }
    }

    fun redoPath() {
        when {
            undonePaths.size > 0 -> {
                paths.add(undonePaths.removeAt(undonePaths.size - 1))
                invalidate()
            }
        }
    }

    fun clearDrawing() {
        drawPath?.reset() // Avoiding saving redo from Path().
        undonePaths.addAll(paths)
        paths.clear()
        invalidate()
    }

    fun saveBitmap(bitmap: Bitmap): String {
        val resolver = context.contentResolver
        val fileName = "Canvas_${System.currentTimeMillis() / 1000}.png"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)

        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {

            Timber.i(uri.toString())
            saveDrawingToStream(bitmap, resolver.openOutputStream(uri))
            context.contentResolver.update(uri, values, null, null)
            imageUri = uri
            result = uri.toString()
        }

        return result
    }

     fun uploadToFireStore(uri: Uri) {

        Timber.i(uri.toString())
        val fireRef = FirebaseStorage.getInstance().reference.child("uploads").child("Canvas_${System.currentTimeMillis() / 1000}.png")

        fireRef.putFile(uri).addOnCompleteListener {
            if (it.isSuccessful) {
                snackBarMsg(this,"done")
                fireRef.downloadUrl.addOnSuccessListener { url ->
                    Timber.i(url.toString())
                }
            }else {
                Timber.i(it.exception)
            }
        }


    }

    fun getBitmap(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawing = view.background

        when {
            bgDrawing != null -> bgDrawing.draw(canvas)
            else -> canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return bitmap
    }

    private fun saveDrawingToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileExtension(uri : Uri)  : String?{
        val contentResolver = context.contentResolver

        val mimeTypeMap = MimeTypeMap.getSingleton()

        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri))
    }

    // An inner class for custom path with two params as color and stroke size.
    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()
}