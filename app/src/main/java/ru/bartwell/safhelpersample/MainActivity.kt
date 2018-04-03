package ru.bartwell.safhelpersample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import ru.bartwell.safhelper.SafHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var safHelper: SafHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val externalStoragePaths = getExternalStoragePaths()
        if (externalStoragePaths.isEmpty()) {
            main_path.setText("/sdcard/somedir/somefile.txt")
        } else {
            main_path.setText("${externalStoragePaths[0]}/somedir/somefile.txt")
        }

        main_write_file.setOnClickListener { writeFile(true) }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (safHelper.onActivityResult(this, requestCode, resultCode, resultData)) {
            writeFile(false)
        }
    }

    private fun writeFile(requestPermissions: Boolean) {
        val userFile = File(main_path.text.toString())

        if (userFile.parent == null) {
            showToast("Wrong path")
        } else {
            safHelper = SafHelper(this, userFile.parent)
            if (safHelper.isApplicable()) {
                if (safHelper.isPermissionGranted()) {
                    try {
                        if (safHelper.mkdirs(userFile.parent)) {
                            val outputStream = safHelper.createFile(userFile.path)
                            outputStream.use {
                                it.write("Text in the file".toByteArray())
                            }
                            showToast("Success")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        showToast("Error: " + e.message)
                    }
                } else {
                    if (requestPermissions) {
                        safHelper.requestPermissions(this)
                    } else {
                        showToast("Permissions not granted")
                    }
                }
            } else {
                showToast("File is not on SD Card or Android version < 5.0")
            }
        }
    }

    private fun showToast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    private fun getExternalStoragePaths(): List<String> {
        val result = ArrayList<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            externalCacheDirs
                    .filter { Environment.isExternalStorageRemovable(it) }
                    .mapTo(result) { it.path.split("/Android")[0] }
        }
        return result
    }
}
