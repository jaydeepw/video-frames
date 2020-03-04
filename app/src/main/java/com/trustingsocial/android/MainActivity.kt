package com.trustingsocial.android

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileInputStream

// content://media/external/video/media/174021
// /storage/emulated/0/DCIM/Camera/VID_20200304_100219.mp4
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        /*fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/

        button.setOnClickListener {
            captureCamera()
        }

        if (checkSelfPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ), PERMISSIONS_CODE
            )
        }

        if (IS_DEVELOPING) {
            showFramesFromUri(Uri.parse("content://media/external/video/media/174021"))
        }
    }

    private fun captureCamera() {
        val captureVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        captureVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3)
        startActivityForResult(captureVideoIntent, VIDEO_CAPTURED)
    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null)
            val column_index: Int? = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor?.moveToFirst()
            cursor?.getString(column_index!!)
        } finally {
            cursor?.close()
        }
    }

    val handler = Handler()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val videoFileUri = data?.data
        Log.d("M", "videoFileUri: $videoFileUri")
        showFramesFromUri(videoFileUri)
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
        // retriever.frameAtTime
    }

/*
    public fun getFileDuration(Context context, File file) : String {
        String result = null;
        MediaMetadataRetriever retriever = null
        FileInputStream inputStream = null

        try {
            retriever = new MediaMetadataRetriever()
            inputStream = new FileInputStream(file.getAbsolutePath());
            retriever.setDataSource(inputStream.getFD());
            long time = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            result = String.format(context.getResources().getString(R.string.player_time_format),
                AppUtil.getPlayerMinutes(time), AppUtil.getPlayerSoconds(time));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        } finally{
            if (retriever != null){
                retriever.release()
            }if (inputStream != null){
                inputStream.close()
            }
        }
        return result;
    }
*/

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
        private const val PERMISSIONS_CODE: Int = 22
        private val TAG = MainActivity::class.java.simpleName

        private const val IS_DEVELOPING = true
    }
}
