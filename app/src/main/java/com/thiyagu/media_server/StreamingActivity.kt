package com.thiyagu.media_server

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.thiyagu.media_server.databinding.ActivityStreamingBinding
import com.thiyagu.media_server.server.MediaStreamingServer
import com.thiyagu.media_server.utils.RealPathUtil


class StreamingActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityStreamingBinding;
    val BROWSE_FILE_REQUEST = 1000
    var file_path: String? = null
    var media_Streaming_server: MediaStreamingServer? = null
    var server_port = 8888;
    var TAG: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStreamingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        binding.browse.setOnClickListener(this)
        binding.ignitor.setOnClickListener(this)
        binding.serverUrl.setOnClickListener(this)
        TAG = this.localClassName
    }

    override fun onClick(p0: View?) {
        when (p0!!.id) {
            binding.browse.id -> {


                if (isReadStoragePermissionGranted()) {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "*/*"
                    try {

                        startActivityForResult(intent, BROWSE_FILE_REQUEST)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this@StreamingActivity,
                            "There are no file explorer clients installed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {

                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        3
                    )
                }


            }
            binding.ignitor.id -> {
                if (binding.ignitor.text.equals("STOP")) {

                    stopServerIfExist()
                    setStatus()
                } else if (binding.ignitor.text.equals("START")) {
                    stopServerIfExist()
                    file_path?.let { startServer(it) }
                    setStatus()
                    //already server is running
                }


            }
            binding.serverUrl.id->{
               copyToClipboard(binding.serverUrl.text.toString())

            }

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 3 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(applicationContext, "Browse your file now", Toast.LENGTH_LONG).show()
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (null != data && requestCode == BROWSE_FILE_REQUEST && resultCode == Activity.RESULT_OK) {

            file_path = RealPathUtil.getRealPath(this@StreamingActivity, data!!.data)
            binding.path.setText(file_path)
            Toast.makeText(this@StreamingActivity, "File Selected", Toast.LENGTH_LONG).show()
            stopServerIfExist();
            startServer(file_path.toString());
            setStatus()

        } else {
            Toast.makeText(this@StreamingActivity, "File Select Failed", Toast.LENGTH_LONG).show()
        }


    }

    private fun setStatus() {
        if (media_Streaming_server!!.isAlive) {
            binding.status.setText("Server Running")
            binding.ignitor.setText("STOP")
            Toast.makeText(applicationContext, "Server running", Toast.LENGTH_SHORT).show()

        } else {
            binding.status.setText("Server Stopped")
            binding.ignitor.setText("START")
        }
    }

    private fun startServer(file_path: String) {
        media_Streaming_server =
            MediaStreamingServer(
                file_path,
                this.server_port
            )
        media_Streaming_server!!.start()
        binding.serverUrl.setText(media_Streaming_server!!.getServerUrl(getIP()))
        // url = media_server!!.getServerUrl(getIP())


    }

    private fun stopServerIfExist() {
        if (media_Streaming_server != null) {
            media_Streaming_server!!.closeAllConnections()
            media_Streaming_server!!.stop()
        }
//        try {
//            //ServerSocket(server_port).close()
//
//        } catch (e: IOException) {
//            Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG).show()
//        }
        // Stop server before starting new one if exist


    }

    fun isReadStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v(TAG, "Permission is granted")
                true
            } else {
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted")
            true
        }
    }


    fun getIP(): String {

        val wm = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ip: String = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)



        return ip


    }

    fun Context.copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip =
            ClipData.newPlainText("server_url", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(applicationContext,"url copied "+ text,Toast.LENGTH_LONG).show()
    }
}