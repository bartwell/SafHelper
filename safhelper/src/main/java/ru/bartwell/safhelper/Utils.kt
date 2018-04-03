package ru.bartwell.safhelper

import android.content.Context
import android.os.Build
import android.os.Environment
import android.support.annotation.RequiresApi
import android.text.TextUtils
import android.util.Log
import java.io.File

/**
 * Created by BArtWell on 27.02.2018.
 */

private const val CACHE_DIR_PATH_PART = "/Android"

internal class Utils {
    companion object {
        const val TAG = "SafHelper"

        fun normalizePath(path: String): String = File(path).absolutePath

        fun getPathAndNameFromPath(path: String): Pair<String, String> {
            val parts = splitPathToParts(path)
            return if (parts.size < 2) {
                Pair(File.separator, parts[parts.size - 1])
            } else {
                Pair(File.separator + TextUtils.join(File.separator, parts.dropLast(1)), parts[parts.size - 1])
            }
        }

        fun splitPathToParts(path: String) = path.split(File.separator).filter { !TextUtils.isEmpty(it) }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun findExternalStoragePath(context: Context, path: String) = getExternalStoragePaths(context).firstOrNull { path.startsWith(it) }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun getExternalStoragePaths(context: Context): List<String> {
            val result = ArrayList<String>()
            context.externalCacheDirs
                    .filter { Environment.isExternalStorageRemovable(it) }
                    .mapTo(result) { it.path.split(CACHE_DIR_PATH_PART)[0] }
            Log.d(TAG, "getExternalStoragePaths: $result")
            return result
        }

        fun getRelativePath(storagePath: String, path: String): String {
            if (path.length > storagePath.length) {
                return path.substring(storagePath.length)
            }
            return ""
        }
    }
}