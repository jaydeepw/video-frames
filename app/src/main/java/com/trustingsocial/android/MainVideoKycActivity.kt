package com.trustingsocial.android

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import kotlinx.android.synthetic.main.activity_main_video_kyc.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainVideoKycActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_video_kyc)
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
                    captureFrames(Uri.parse(DUMMY_VIDEO_URI))
                } else {
                    captureCamera()
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            if (IS_DEVELOPING) {
                captureFrames(Uri.parse(DUMMY_VIDEO_URI))
            }
        }

        showRandomNumber()
    }

    private fun showRandomNumber() {
        randomNumber.text = Random.nextInt(100_000, 999_999).toString()
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

    /**
     * This method should not be called from the main thread.
     */
    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, projection, null, null, null)
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
                captureFrames(videoFileUri)
            }
        } else {
            Log.w(TAG, "activity cancelled...")
        }
    }

    private fun captureFrames(videoFileUri: Uri?) {
        // Toast.makeText(this, videoFileUri.toString(), Toast.LENGTH_LONG).show()
        val path = getRealPathFromURI(this, videoFileUri!!)
        Log.d(TAG, "path: $path")

        if (path == null) {
            throw IllegalStateException("After parsing the video URI, path found to be null")
        }
        Log.d(TAG, "path: $path")
        val file = File(path)
        val bitmaps = getFrames(file)
        viewPager.adapter = getAdapter(bitmaps)
    }

    /**
     * This method should not be called from the main thread.
     */
    private fun getFrames(file: File): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val bitmaps = mutableListOf<Bitmap>()
        var inputStream: FileInputStream? = null
        try {
            val absPath = file.absolutePath
            Log.d(TAG, "absPath: $absPath")
            inputStream = FileInputStream(absPath)

            retriever.setDataSource(inputStream.fd)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            Log.d(TAG, "time: $time")
            for (count in 0 until NUMBER_OF_FRAMES) {
                val bitmap = retriever.getFrameAtTime(
                    TimeUnit.MILLISECONDS.toMicros(getRandomTimeWithinVideoDuration())
                )
                bitmaps.add(bitmap)
                Log.d(TAG, "height: ${bitmap.height}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
            inputStream?.close()
        }

        return bitmaps
    }

    private fun getRandomTimeWithinVideoDuration(): Long {
        val startTimeInMillis = 1L
        // keeping 500ms lesser just to be on the safer side.
        val endTimeInMillis = (getVideoDuration() * 1000) - 500L
        return Random.nextLong(startTimeInMillis, endTimeInMillis)
    }

    private fun getAdapter(bitmaps: List<Bitmap>): FragmentStatePagerAdapter {

        return object : FragmentStatePagerAdapter(supportFragmentManager) {

            override fun getItem(position: Int): Fragment {
                return CardFragment(bitmaps[position])
            }

            override fun getCount(): Int {
                return bitmaps.size
            }
        }
    }

    companion object {
        private val TAG = MainVideoKycActivity::class.java.simpleName
        private const val VIDEO_CAPTURED: Int = 21
        private const val PERMISSIONS_CODE_STORAGE = 22
        private const val NUMBER_OF_FRAMES = 5

        private const val DUMMY_VIDEO_URI = "content://media/external/video/media/174071"
        // this ƒlag was just used to speed up the development.
        // Nothing else.
        private const val IS_DEVELOPING = false
    }
}
