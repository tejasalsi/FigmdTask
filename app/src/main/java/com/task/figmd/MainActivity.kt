package com.task.figmd

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.InputStream
import java.util.*

private const val REQUEST_STORAGE = 1

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var mWidth = 0
    private var mHeight = 0
    private val permissionArray = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnEraser.setOnClickListener(this)
        btnPen.setOnClickListener(this)
        btnImage.setOnClickListener(this)
        btnSave.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        when(p0) {
            btnEraser -> {
                sketchCanvas.setEraserMode(true)
            }
            btnPen -> {
                sketchCanvas.setEraserMode(false)
            }
            btnImage -> {
                selectImage()
            }
            btnSave -> {
                if(checkForPermissions(permissionArray, REQUEST_STORAGE)) {
                    sketchCanvas.saveCanvas()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        mWidth = sketchCanvas.width
        mHeight = sketchCanvas.height
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === 1000 && resultCode === Activity.RESULT_OK) {
            val imageData: Uri? = data!!.data
            val bitmap: Bitmap = getBitmapFromUri(imageData!!)
            if (bitmap != null) {
                sketchCanvas.loadBitmap(bitmap)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == REQUEST_STORAGE) {
            var result = true
            if (grantResults.isNotEmpty()) {
                for (permission in grantResults) {
                    if (permission != PackageManager.PERMISSION_GRANTED) {
                        result = false
                        break
                    }
                }
            }
            if (grantResults.isNotEmpty() && result) {
                sketchCanvas.saveCanvas()
            } else {
                var permissionResult = true//use this later to show dialog about permission required
                for (permission in permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            permission
                        )
                    ) {
                        permissionResult = false
                        break
                    }
                }
            }
        }
    }

    private fun selectImage() {
        val i = Intent()
        i.type = "image/*"
        i.action = Intent.ACTION_GET_CONTENT

        val customChooserIntent = Intent.createChooser(i, "Select image to load")
        startActivityForResult(customChooserIntent, 1000)
    }

    private fun getBitmapFromUri(bitmapUri: Uri): Bitmap {
        var bitmap: Bitmap? = null


        var inputStream: InputStream? = null
        try {
            inputStream = contentResolver.openInputStream(bitmapUri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            // Calculate inSampleSize
            options.inSampleSize = getSampleSize(options, mWidth, mHeight)
            options.inJustDecodeBounds = false
            inputStream = contentResolver.openInputStream(bitmapUri)
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            if (bitmap == null) {
                Toast.makeText(baseContext, "Image is not Loaded", Toast.LENGTH_SHORT).show()
                return bitmap!!
            }
            inputStream!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return bitmap!!
    }

    private fun getSampleSize(options: BitmapFactory.Options, sWidth: Int,sHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var sampleSize = 1

        if (height > sHeight || width > sWidth) {
            val heightRatio =
                Math.round(height.toFloat() / sHeight as Float)
            val widthRatio =
                Math.round(width.toFloat() / sWidth as Float)
            sampleSize = if (heightRatio < widthRatio) heightRatio else widthRatio
        }

        return sampleSize
    }

    //This can be taken to BaseActivity
    private fun checkForPermissions(permissions: Array<String>, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var result: Int
            val listPermissions = ArrayList<String>()
            for (permission in permissions) {
                result = ContextCompat.checkSelfPermission(this, permission)
                if (result != PackageManager.PERMISSION_GRANTED) {
                    listPermissions.add(permission)
                }
            }
            if (!listPermissions.isEmpty()) {
                requestPermissionsSafely(listPermissions.toTypedArray(), requestCode)
                return false
            }
        }
        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun requestPermissionsSafely(permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode)
        }
    }
}
