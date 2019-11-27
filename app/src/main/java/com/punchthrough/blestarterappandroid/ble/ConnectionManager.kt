package com.punchthrough.blestarterappandroid.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import timber.log.Timber
import java.lang.ref.WeakReference

object ConnectionManager {
    private var bluetoothGatt: BluetoothGatt? = null
    private var listeners: MutableSet<WeakReference<ConnectionEventListener>> = mutableSetOf()

    fun connect(device: BluetoothDevice, context: Context) {
        Timber.w("Connecting to ${device.address}")
        device.connectGatt(context, false, callback)
    }

    fun teardownConnection() {
        Timber.w("Disconnecting from ${bluetoothGatt?.device?.address}")
        bluetoothGatt?.close()
        bluetoothGatt = null
        listeners.forEach { it.get()?.onDisconnect?.invoke() }
    }

    fun registerListener(listener: ConnectionEventListener) {
        if (listeners.map { it.get() }.contains(listener)) { return }
        listeners.add(WeakReference(listener))
        listeners = listeners.filter { it.get() != null }.toMutableSet()
        Timber.d("Added listener $listener, ${listeners.size} listeners total")
    }

    fun unregisterListener(listener: ConnectionEventListener) {
        // Removing elements while in a loop results in a java.util.ConcurrentModificationException
        var toRemove: WeakReference<ConnectionEventListener>? = null
        listeners.forEach {
            if (it.get() == listener) {
                toRemove = it
            }
        }
        toRemove?.let {
            listeners.remove(it)
            Timber.d("Removed listener ${it.get()}, ${listeners.size} listeners total")
        }
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.w("Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(gatt) }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.e("Successfully disconnected from $deviceAddress")
                    teardownConnection()
                }
            } else {
                /** Assuming the simplest case, teardown connection upon error.
                 *  For more complex use cases, check to see if status is the following
                 *  and recover accordingly:
                 *  - [BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION]
                 *  - [BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION]
                 *  - [BluetoothGatt.GATT_READ_NOT_PERMITTED]
                 *  - [BluetoothGatt.GATT_WRITE_NOT_PERMITTED]
                 *  - [BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED]
                 */
                Timber.e("Error $status encountered for $deviceAddress! Disconnecting...")
                teardownConnection()
            }
        }
    }
}
