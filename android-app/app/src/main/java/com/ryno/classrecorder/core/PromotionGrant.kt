package com.ryno.classrecorder.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Parcel
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.SystemServiceHelper

/**
 * Grants this app Android 16's "can be promoted" preference.
 *
 * INotificationManager.setCanBePromoted is guarded by
 * checkCallerIsSystemOrSystemUiOrShell(), so an app cannot set it for itself.
 * Without it NotificationManagerService never assigns FLAG_PROMOTED_ONGOING,
 * and that flag is what turns an ongoing notification into a Live Update -
 * which is what HyperOS 3 draws in its island.
 *
 * Shizuku gives us a binder to the system service attributed as shell uid
 * (2000), the identity the guard wants. SystemServiceHelper resolves both the
 * service binder and the real transaction code for the method name, so no
 * remote code execution and no guessing at codes.
 */
object PromotionGrant {
    private const val TAG = "ClassRecorderPromo"
    private const val PERMISSION = "moe.shizuku.manager.permission.API_V23"
    private const val SERVICE = "notification"
    private const val DESCRIPTOR = "android.app.INotificationManager"
    private const val METHOD = "setCanBePromoted"

    fun isShizukuReady(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** Ask NotificationManagerService to treat our notifications as promotable. */
    fun request(context: Context, enable: Boolean): Boolean {
        if (!isShizukuReady()) {
            Log.i(TAG, "shizuku not ready")
            return false
        }
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            val service = SystemServiceHelper.getSystemService(SERVICE)
                ?: run {
                    Log.i(TAG, "no " + SERVICE + " service")
                    return false
                }
            // getTransactionCode() reads the AIDL $Stub table, which HyperOS does
            // not expose for INotificationManager. Fall back to the code verified
            // empirically on this build: 150 is the only transaction in 145..153
            // that clears the interface-token check and reaches the attribution
            // guard (see service-call reply comparison in the project notes).
            val code = SystemServiceHelper.getTransactionCode(SERVICE, METHOD) ?: 150
            data.writeInterfaceToken(DESCRIPTOR)
            data.writeString(context.packageName)
            data.writeInt(context.applicationInfo.uid)
            data.writeInt(if (enable) 1 else 0)
            data.writeInt(1) // fromUser
            service.transact(code, data, reply, 0)
            reply.readException()
            Log.i(TAG, "setCanBePromoted ok (enable=" + enable + ", code=" + code + ")")
            true
        } catch (t: Throwable) {
            Log.i(TAG, "setCanBePromoted failed: " + t.javaClass.simpleName + ": " + t.message)
            false
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
