package de.simon.dankelmann.submarine.permissioncheck

import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat.checkSelfPermission
import de.simon.dankelmann.submarine.appcontext.AppContext

class PermissionCheck {
    companion object {
        private val _logTag = "PermissionCheck"
        fun checkPermission(permission:String):Boolean{
            return true
            if(checkSelfPermission(AppContext.appContext!!, permission) == PackageManager.PERMISSION_GRANTED){
                return true
            }
            Log.d(_logTag, "Permission not granted: $permission")
            return false
        }
    }
}