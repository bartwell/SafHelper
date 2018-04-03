package ru.bartwell.safhelper

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.provider.DocumentFile
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream

/**
 * Created by BArtWell on 25.02.2018.
 */

private const val DEFAULT_REQUEST_CODE = 10001
private val IS_SAF_AVAILABLE = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

class SafHelper(private val context: Context, private val basePath: String, private val intentRequestCode: Int = DEFAULT_REQUEST_CODE) {

    private var permission: UriPermission? = null
    private var basePermittedPath: String? = null

    init {
        Log.d(Utils.TAG, "New instance")
        init()
    }

    private fun init() {
        Log.d(Utils.TAG, "Is SAF available: $IS_SAF_AVAILABLE")
        if (IS_SAF_AVAILABLE) {
            val permissionHelper = PermissionHelper(context, Utils.normalizePath(basePath))
            basePermittedPath = permissionHelper.basePermittedPath
            Log.d(Utils.TAG, "Base permitted path: $basePermittedPath")
            permission = permissionHelper.permission
            Log.d(Utils.TAG, "Permission: ${permission?.uri} (${permission?.isWritePermission})")
        }
    }

    fun requestPermissions(activity: Activity) {
        Log.d(Utils.TAG, "requestPermissions")
        if (IS_SAF_AVAILABLE) {
            activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), intentRequestCode)
        }
    }

    fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, resultData: Intent?): Boolean {
        Log.d(Utils.TAG, "onActivityResult($requestCode,$resultCode)")
        if (requestCode == intentRequestCode) {
            if (resultCode == Activity.RESULT_OK && IS_SAF_AVAILABLE) {
                val uri = resultData?.data
                Log.d(Utils.TAG, "onActivityResult: $uri")
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                activity.grantUriPermission(activity.packageName, uri, flags)
                activity.contentResolver.takePersistableUriPermission(uri!!, flags)
                init()
            }
            return true
        }
        return false
    }

    fun isPermissionGranted() = (IS_SAF_AVAILABLE && permission != null && permission!!.isWritePermission)

    fun isApplicable() = IS_SAF_AVAILABLE && Utils.findExternalStoragePath(context, basePath) != null

    private fun isPathOnManagedStorage(path: String) = path.startsWith(basePermittedPath!!)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun mkdirs(dirPath: String): Boolean {
        Log.d(Utils.TAG, "mkdirs($dirPath)")

        val relativePath = checkAndGetRelativePath(dirPath)
        Log.d(Utils.TAG, "mkdirs: relative path = $relativePath")
        if (relativePath.isEmpty()) {
            Log.d(Utils.TAG, "mkdirs: path is a root of the storage, exit")
            return true
        }

        var tmpPath = ""
        for (part in Utils.splitPathToParts(relativePath)) {
            if (!tmpPath.isEmpty()) {
                tmpPath += File.separator
            }
            tmpPath += part
            if (!internalMkdir(tmpPath)) {
                return false
            }
        }

        return true
    }

    private fun checkAndGetRelativePath(path: String): String {
        Log.d(Utils.TAG, "checkAndGetRelativePath($path)")

        if (!isApplicable()) {
            throw UnsupportedOperationException("Can be done with SAF. You should check path with isApplicable() before call it")
        }

        val normalizedPath = Utils.normalizePath(path)
        Log.d(Utils.TAG, "checkAndGetRelativePath: normalized path = $normalizedPath")
        if (!isPathOnManagedStorage(normalizedPath)) {
            throw UnsupportedOperationException("Path is outside the managed storage. Path should starts with $basePermittedPath")
        }

        val relativePath = Utils.getRelativePath(basePermittedPath!!, normalizedPath)
        Log.d(Utils.TAG, "checkAndGetRelativePath: relative path = $relativePath")
        return relativePath
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun internalMkdir(dirPath: String): Boolean {
        Log.d(Utils.TAG, "internalMkdir($dirPath)")
        val (path, name) = Utils.getPathAndNameFromPath(dirPath)
        val documentFile = getDocumentFileFromPath(path)
        documentFile.findFile(name)?.let {
            if (it.exists()) {
                Log.d(Utils.TAG, "internalMkdir: directory already exists")
                return it.isDirectory
            }
        }
        return documentFile.createDirectory(name) != null
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun mkdir(dirPath: String): Boolean {
        Log.d(Utils.TAG, "mkdir($dirPath)")

        val relativePath = checkAndGetRelativePath(dirPath)
        if (relativePath.isEmpty()) {
            Log.d(Utils.TAG, "mkdir: path is a root of the storage, exit")
            return true
        }

        return internalMkdir(relativePath)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun createFile(filePath: String): OutputStream {
        Log.d(Utils.TAG, "createFile($filePath)")

        val relativePath = checkAndGetRelativePath(filePath)
        if (relativePath.isEmpty()) {
            throw UnsupportedOperationException("File name is not specified")
        }

        var uri: Uri? = null
        val (path, name) = Utils.getPathAndNameFromPath(relativePath)
        val documentFile = getDocumentFileFromPath(path)

        documentFile.findFile(name)?.let {
            if (it.exists()) {
                if (it.isFile) {
                    Log.d(Utils.TAG, "createFile: file already exists")
                    uri = it.uri
                } else {
                    throw UnsupportedOperationException("$name already exists and not a file (cannot override it)")
                }
            }
        }

        if (uri == null) {
            Log.d(Utils.TAG, "createFile: file not exists, create new")
            uri = documentFile.createFile(null, name).uri
        }

        Log.d(Utils.TAG, "createFile: Write data")
        return context.contentResolver.openOutputStream(uri)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getDocumentFileFromPath(relativePath: String): DocumentFile {
        Log.d(Utils.TAG, "getDocumentFileFromPath($relativePath)")
        val parts = Utils.splitPathToParts(relativePath)
        Log.d(Utils.TAG, "getDocumentFileFromPath: parts=$parts")
        var document = DocumentFile.fromTreeUri(context, permission?.uri)
        for (part in parts) {
            document = document.findFile(part) ?: throw FileNotFoundException("$part doesn't exists")
        }
        Log.d(Utils.TAG, "getDocumentFileFromPath: success")
        return document
    }
}