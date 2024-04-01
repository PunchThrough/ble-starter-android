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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.punchthrough.blestarterappandroid.ble.ConnectionEventListener
import com.punchthrough.blestarterappandroid.ble.ConnectionManager
import com.punchthrough.blestarterappandroid.databinding.ActivityMainBinding
import timber.log.Timber

private const val PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    /*******************************************
     * Properties
     *******************************************/

    private lateinit var binding: ActivityMainBinding

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var isScanning = false
        set(value) {
            field = value
            runOnUiThread { binding.scanButton.text = if (value) "Stop Scan" else "Start Scan" }
        }

    private val scanResults = mutableListOf<ScanResult>()
    private val scanResultAdapter: ScanResultAdapter by lazy {
        ScanResultAdapter(scanResults) { result ->
            if (isScanning) {
                stopBleScan()
            }
            with(result.device) {
                Timber.w("Connecting to $address")
                ConnectionManager.connect(this, this@MainActivity)
            }
        }
    }

    private val bluetoothEnablingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Timber.i("Bluetooth is enabled, good to go")
        } else {
            Timber.e("User dismissed or denied Bluetooth prompt")
            promptEnableBluetooth()
        }
    }

    /*******************************************
     * Activity function overrides
     *******************************************/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        binding.scanButton.setOnClickListener { if (isScanning) stopBleScan() else startBleScan() }
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        ConnectionManager.registerListener(connectionEventListener)
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) {
            stopBleScan()
        }
        ConnectionManager.unregisterListener(connectionEventListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return
        }
        if (permissions.isEmpty() && grantResults.isEmpty()) {
            Timber.e("Empty permissions and grantResults array in onRequestPermissionsResult")
            Timber.w("This is likely a cancellation due to user interaction interrupted")
            return
        }

        // Log permission request outcomes
        val resultsDescriptions = grantResults.map {
            when (it) {
                PackageManager.PERMISSION_DENIED -> "Denied"
                PackageManager.PERMISSION_GRANTED -> "Granted"
                else -> "Unknown"
            }
        }
        Timber.w("Permissions: ${permissions.toList()}, grant results: $resultsDescriptions")

        // A denied permission is permanently denied if shouldShowRequestPermissionRationale is false
        val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
            it.second == PackageManager.PERMISSION_DENIED &&
                !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
        }
        val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        when {
            containsPermanentDenial -> {
                Timber.e("User permanently denied granting of permissions")
                Timber.e("Requesting for manual granting of permissions from App Settings")
                promptManualPermissionGranting()
            }
            containsDenial -> {
                // It's still possible to re-request permissions
                requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
            }
            allGranted && hasRequiredBluetoothPermissions() -> {
                startBleScan()
            }
            else -> {
                Timber.e("Unexpected scenario encountered when handling permissions")
                recreate()
            }
        }
    }

    /*******************************************
     * Private functions
     *******************************************/

    /**
     * Prompts the user to enable Bluetooth via a system dialog.
     *
     * For Android 12+, [Manifest.permission.BLUETOOTH_CONNECT] is required to use
     * the [BluetoothAdapter.ACTION_REQUEST_ENABLE] intent.
     */
    private fun promptEnableBluetooth() {
        if (hasRequiredBluetoothPermissions() && !bluetoothAdapter.isEnabled) {
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

    @SuppressLint("MissingPermission, NotifyDataSetChanged") // Check performed inside extension fun
    private fun startBleScan() {
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantBluetoothPermissions(PERMISSION_REQUEST_CODE)
        } else {
            scanResults.clear()
            scanResultAdapter.notifyDataSetChanged()
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
        }
    }

    @SuppressLint("MissingPermission") // Check performed inside extension fun
    private fun stopBleScan() {
        if (hasRequiredBluetoothPermissions()) {
            bleScanner.stopScan(scanCallback)
            isScanning = false
        }
    }

    @UiThread
    private fun setupRecyclerView() {
        binding.scanResultsRecyclerView.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
            itemAnimator.let {
                if (it is SimpleItemAnimator) {
                    it.supportsChangeAnimations = false
                }
            }
        }
    }

    @UiThread
    private fun promptManualPermissionGranting() {
        AlertDialog.Builder(this)
            .setTitle(R.string.please_grant_relevant_permissions)
            .setMessage(R.string.app_settings_rationale)
            .setPositiveButton(R.string.app_settings) { _, _ ->
                try {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:$packageName")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: ActivityNotFoundException) {
                    if (!isFinishing) {
                        Toast.makeText(
                            this,
                            R.string.cannot_launch_app_settings,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                finish()
            }
            .setNegativeButton(R.string.quit) { _, _ -> finishAndRemoveTask() }
            .setCancelable(false)
            .show()
    }

    /*******************************************
     * Callback bodies
     *******************************************/

    // If we're getting a scan result, we already have the relevant permission(s)
    @SuppressLint("MissingPermission")
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {
                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Timber.e("onScanFailed: code $errorCode")
        }
    }

    private val connectionEventListener by lazy {
        ConnectionEventListener().apply {
            onConnectionSetupComplete = { gatt ->
                Intent(this@MainActivity, BleOperationsActivity::class.java).also {
                    it.putExtra(BluetoothDevice.EXTRA_DEVICE, gatt.device)
                    startActivity(it)
                }
            }
            @SuppressLint("MissingPermission")
            onDisconnect = {
                val deviceName = if (hasRequiredBluetoothPermissions()) {
                    it.name
                } else {
                    "device"
                }
                runOnUiThread {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.disconnected)
                        .setMessage(
                            getString(R.string.disconnected_or_unable_to_connect_to_device, deviceName)
                        )
                        .setPositiveButton(R.string.ok, null)
                        .show()
                }
            }
        }
    }
}
