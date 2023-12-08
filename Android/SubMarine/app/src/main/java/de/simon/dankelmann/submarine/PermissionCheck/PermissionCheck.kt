package de.simon.dankelmann.submarine.PermissionCheck

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.ContextCompat.startActivity
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.BuildConfig
import de.simon.dankelmann.submarine.MainActivity

class PermissionCheck (){
    companion object {
        private val _logTag = "PermissionCheck"
        fun checkPermission(permission:String, activity: Activity):Boolean{

            if (permission == "android.permission.BLUETOOTH_ADVERTISE" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_ADVERTISE was first introduced in api level 31
                return true
            }

            if (permission == "android.permission.BLUETOOTH_SCAN" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_SCAN was first introduced in api level 31
                return true
            }

            if (permission == "android.permission.BLUETOOTH_CONNECT" && Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            {
                // android.permission.BLUETOOTH_CONNECT was first introduced in api level 31
                return true
            }

            //return true
            if(checkSelfPermission(AppContext.getContext(), permission) == PackageManager.PERMISSION_GRANTED){
                Log.d(_logTag, "Permission granted: $permission")
                return true
            } else {
                Log.d(_logTag, "requesting $permission now")
                if(permission == android.Manifest.permission.MANAGE_EXTERNAL_STORAGE){
                    val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                    activity.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            uri
                        )
                    )
                } else {
                    ActivityCompat.requestPermissions(activity, arrayOf(permission), 1000)
                }

            }
            Log.d(_logTag, "Permission not granted: $permission")
            return false
        }

        fun requireAllPermissions(activity: Activity, permissions: Array<String>){
            var requestNeeded: MutableList<String> = mutableListOf()
            var dialogNeeded: MutableList<String> = mutableListOf()

            permissions.forEachIndexed { index, permission ->
                if(checkSelfPermission(AppContext.getContext(), permission) == PackageManager.PERMISSION_GRANTED){
                    Log.d(_logTag, "Permission granted: $permission")
                } else {

                    if(permission == android.Manifest.permission.MANAGE_EXTERNAL_STORAGE){
                        if(Environment.isExternalStorageManager() == false){
                            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                            activity.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    uri
                                )
                            )
                        }
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                            // Show an explanation asynchronously
                            Log.d(_logTag, "Show explanation for: $permission")
                            dialogNeeded.add(permission)
                        } else {
                            requestNeeded.add(permission)
                        }
                    }
                }
             }


            // SHOW DIALOG
            if(requestNeeded.size > 0){
                requestPermissions(activity, requestNeeded.toTypedArray())
               // showAlert(activity, requestNeeded.toTypedArray())
            }

            if(dialogNeeded.size > 0){
                    dialogNeeded.forEachIndexed { index, permission ->
                        run {
                            showAlert(activity, permission)
                        }
                }
            }
        }

        private fun showAlert(activity: Activity, permission:String) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle("Requesting permission")
            builder.setMessage("Submarine requires the following permission: $permission, please grant it in order to proceed")
            builder.setPositiveButton("OK", { dialog, which -> requestPermissions(activity, arrayOf(permission)) })
            builder.setNeutralButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()
        }

        private fun requestPermissions(activity: Activity, permissions: Array<String>){
                ActivityCompat.requestPermissions(activity, permissions, 1234)
        }

        fun processPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
            var result = 0
            if (grantResults.isNotEmpty()) {
                for (item in grantResults) {
                    result += item
                }
            }
            if (result == PackageManager.PERMISSION_GRANTED) return true
            return false
        }
    }
    }