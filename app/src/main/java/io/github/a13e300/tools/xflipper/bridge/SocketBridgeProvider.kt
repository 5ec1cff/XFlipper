package io.github.a13e300.tools.xflipper.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import io.github.a13e300.tools.xflipper.Logger

class SocketBridgeProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        p0: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(p0: Uri): String? = null

    override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

    override fun delete(p0: Uri, p1: String?, p2: Array<out String>?): Int = 0

    override fun update(p0: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == "create") {
            val fd = Os.socket(OsConstants.AF_INET6, OsConstants.SOCK_STREAM, 0)
            val pfd = ParcelFileDescriptor.dup(fd)
            Logger.d("create socket ${pfd.fd}")
            Os.close(fd)
            val b = Bundle()
            b.putParcelable("fd", pfd)
            return b
        }
        return super.call(method, arg, extras)
    }
}