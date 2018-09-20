package ru.bartwell.safhelper

import android.content.Context
import android.content.UriPermission
import android.os.Build
import android.provider.DocumentsContract
import android.support.annotation.RequiresApi
import java.io.File

/**
 * Created by BArtWell on 15.03.2018.
 */

private const val STORAGE_ID_SUFFIX = ":"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal class PermissionHelper(context: Context, path: String) {

    var permission: UriPermission? = null
    var basePermittedPath: String? = null

    init {
        loop@ for (p in getSortedPermissions(context)) {
            val storagePath = Utils.findExternalStoragePath(context, path)
            if (storagePath != null) {
                val (storageId1, relativePath1) = getStorageIdAndPath(p)
                var (storageId2, relativePath2) = getStorageIdAndPath(storagePath, path)
                if (relativePath2.isEmpty()) {
                    relativePath2 = File.separator
                }
                if (!storageId1.isEmpty() && storageId1 == storageId2 && relativePath2.startsWith(relativePath1)) {
                    permission = p
                    basePermittedPath = Utils.normalizePath(storagePath + File.separator + relativePath1)
                    break@loop
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getSortedPermissions(context: Context): List<UriPermission> {
        return context.contentResolver.persistedUriPermissions.sortedWith(Comparator { o1, o2 -> o1.uri.toString().length.compareTo(o2.uri.toString().length) })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getStorageIdAndPath(storagePath: String, path: String): Pair<String, String> {
        val pathParts = Utils.splitPathToParts(storagePath)
        return if (pathParts.isEmpty()) {
            Pair("", "")
        } else {
            Pair(pathParts[pathParts.size - 1], Utils.getRelativePath(storagePath, path))
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getStorageIdAndPath(permission: UriPermission): Pair<String, String> {
        val parts = DocumentsContract.getTreeDocumentId(permission.uri).split(STORAGE_ID_SUFFIX)
        return Pair(parts[0], File.separator + parts[1])
    }
}