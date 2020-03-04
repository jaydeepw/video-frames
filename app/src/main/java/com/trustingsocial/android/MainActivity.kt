package com.trustingsocial.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

// content://media/external/video/media/174021
// /storage/emulated/0/DCIM/Camera/VID_20200304_100219.mp4
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        button.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "All storage permissions are already given")
                requestStoragePermission()
            } else {
                Log.i(TAG, "All permissions are already given")
                if (IS_DEVELOPING) {
                    showFramesFromUri(Uri.parse("content://media/external/video/media/174028"))
                } else {
                    captureCamera()
                }
            }
        }
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PERMISSIONS_CODE_STORAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_CODE_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty()
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED
                ) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.i(TAG, "All permissions are already given")
                    captureCamera()
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "Some permissions are denied", Toast.LENGTH_LONG).show()
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    private fun captureCamera() {
        val captureVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        captureVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, getVideoDuration())
        startActivityForResult(captureVideoIntent, VIDEO_CAPTURED)
    }

    private fun getVideoDuration(): Int {
        return if (IS_DEVELOPING) {
            3
        } else {
            5
        }
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex: Int? = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(columnIndex!!)
        } finally {
            cursor?.close()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == VIDEO_CAPTURED) {
                val videoFileUri = data?.data
                Log.d(TAG, "videoFileUri: $videoFileUri")
                showFramesFromUri(videoFileUri)
            }
        } else {
            Log.d(TAG, "activity cancelled...")
        }
    }

    private fun showFramesFromUri(videoFileUri: Uri?) {
        Toast.makeText(this, videoFileUri.toString(), Toast.LENGTH_LONG).show()
        val path = getRealPathFromURI(this, videoFileUri!!)
        Log.d(TAG, "path: $path")
        val file = File(path)

        /*var path: String?
        Thread(Runnable {
            path = getRealPathFromURI(this, videoFileUri!!)
            Log.d("", "path: $path")

            handler.post {
                Toast.makeText(this, path, Toast.LENGTH_LONG).show()
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(path)
            }
        }).start()*/

        /*videoView.setVideoURI(videoFileUri)
        videoView.start()*/

        Log.d(TAG, "path: $path")
        val retriever = MediaMetadataRetriever()
        // retriever.setDataSource(path)
        val absPath = file.absolutePath
        Log.d(TAG, "absPath: $absPath")
        val inputStream = FileInputStream(absPath)
        retriever.setDataSource(inputStream.fd)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        Log.d(TAG, "time: $time")
        val bitmap = retriever.getFrameAtTime(TimeUnit.SECONDS.toMicros(1))
        Log.d(TAG, "height: ${bitmap.height}")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val VIDEO_CAPTURED: Int = 21
        private const val PERMISSIONS_CODE_STORAGE: Int = 22
        private val TAG = MainActivity::class.java.simpleName

        private const val IS_DEVELOPING = false
    }
}
