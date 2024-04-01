/*
 * Copyright 2024 Punch Through Design LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.punchthrough.blestarterappandroid

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Determine whether the current [Context] has been granted the relevant [Manifest.permission].
 */
fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
        PackageManager.PERMISSION_GRANTED
}

/**
 * Determine whether the current [Context] has been granted the relevant permissions to perform
 * Bluetooth operations depending on the mobile device's Android version.
 */
fun Context.hasRequiredBluetoothPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

/**
 * Request for the necessary permissions for Bluetooth operations to work.
 */
fun Activity.requestRelevantBluetoothPermissions(requestCode: Int) {
    if (hasRequiredBluetoothPermissions()) {
        Timber.w("Required permission(s) for Bluetooth already granted")
        return
    }
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (bluetoothPermissionRationaleRequired()) {
                displayNearbyDevicesPermissionRationale(requestCode)
            } else {
                requestNearbyDevicesPermissions(requestCode)
            }
        }
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
            if (locationPermissionRationaleRequired()) {
                displayLocationPermissionRationale(requestCode)
            } else {
                requestLocationPermission(requestCode)
            }
        }
    }
}

//region Location permission
private fun Activity.locationPermissionRationaleRequired(): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
}

private fun Activity.displayLocationPermissionRationale(requestCode: Int) {
    runOnUiThread {
        AlertDialog.Builder(this)
            .setTitle(R.string.location_permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestLocationPermission(requestCode)
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }
}

private fun Activity.requestLocationPermission(requestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requestCode
    )
}
//endregion

//region Nearby Devices permissions
private fun Activity.bluetoothPermissionRationaleRequired(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.BLUETOOTH_SCAN
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            this, Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        false
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Activity.displayNearbyDevicesPermissionRationale(requestCode: Int) {
    runOnUiThread {
        AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_permission_required)
            .setMessage(R.string.bluetooth_permission_rationale)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestNearbyDevicesPermissions(requestCode)
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Activity.requestNearbyDevicesPermissions(requestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ),
        requestCode
    )
}
//endregion
