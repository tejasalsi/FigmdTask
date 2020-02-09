package com.task.figmd.canvasutils

import android.content.Context
import android.graphics.*
import android.os.AsyncTask
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


private const val STROKE_WIDTH = 12f


class CanvasView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private lateinit var mCanvas: Canvas
    private lateinit var mBitmap: Bitmap

    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var erasing = false

    private val backgroundColor =
        ResourcesCompat.getColor(resources, android.R.color.transparent, null)
    private val drawColor = ResourcesCompat.getColor(resources, android.R.color.black, null)
    private val paint = Paint().apply {
        color = drawColor
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = STROKE_WIDTH
    }
    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop
    private val mMatrix = Matrix()
    private val mSrcRectF = RectF()
    private val mDestRectF = RectF()

    private var path = Path()
    private var imageBitmap: Bitmap? = null
    private var loadImgBitmap = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::mBitmap.isInitialized) mBitmap.recycle()

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap)
        mCanvas.drawColor(backgroundColor)

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (loadImgBitmap) {

            mSrcRectF.set(0F, 0F, imageBitmap!!.width.toFloat(), imageBitmap!!.height.toFloat())
            mDestRectF.set(0F, 0F, width.toFloat()/3, height.toFloat()/3)
            mMatrix.setRectToRect(mSrcRectF, mDestRectF, Matrix.ScaleToFit.CENTER)

            canvas!!.drawBitmap(imageBitmap!!, mMatrix, null)
            canvas!!.drawBitmap(mBitmap, 0f, 0f, null)
            loadImgBitmap = false
        } else {
            if(imageBitmap != null) {
                canvas!!.drawBitmap(imageBitmap!!, mMatrix, null)
            }
            canvas!!.drawBitmap(mBitmap, 0f, 0f, null)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        motionTouchEventX = event!!.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    private fun touchStart() {
        path.reset()
        path.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove() {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            path.quadTo(
                currentX,
                currentY,
                (motionTouchEventX + currentX) / 2,
                (motionTouchEventY + currentY) / 2
            )
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to cache it.
            mCanvas.drawPath(path, paint)
        }
        invalidate()
    }

    private fun touchUp() {
        path.reset()
    }

    fun setEraserMode(mode: Boolean) {
        erasing = mode
        if (mode) {
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            paint.xfermode = null
        }
    }

    fun loadBitmap(bitmap: Bitmap) {
        imageBitmap = bitmap
        loadImgBitmap = true
    }

    fun saveCanvas() {
        val dir = File(context!!.externalMediaDirs[0].path + "/Images")
        if(!dir.exists()) {
            dir.mkdir()
        }
        val newFile = File(dir, "canvas.png")

        try {
            val fileOutputStream = FileOutputStream(newFile)
            val imgProcessAsync = object : AsyncTask<String, Int, Bitmap>() {
                override fun doInBackground(vararg params: String?): Bitmap {
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                    return mBitmap
                }

                override fun onPostExecute(result: Bitmap?) {
                    super.onPostExecute(result)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    Toast.makeText(context,
                        "Save Bitmap: Successful",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            imgProcessAsync.execute()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(context,
                "Something wrong",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(
                context,
                "Something wrong",
                Toast.LENGTH_LONG
            ).show()
        }

    }
}