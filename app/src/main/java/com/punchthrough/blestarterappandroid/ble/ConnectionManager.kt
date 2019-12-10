/*
 * Copyright 2019 Punch Through Design LLC
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

package com.punchthrough.blestarterappandroid.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.lang.ref.WeakReference

// Taken from gatt_api.h and used as proof-of-concept only
private const val GATT_MAX_MTU_SIZE = 517

object ConnectionManager {
    val services
        get() = bluetoothGatt?.services

    private var bluetoothGatt: BluetoothGatt? = null
    private var listeners: MutableSet<WeakReference<ConnectionEventListener>> = mutableSetOf()

    fun connect(device: BluetoothDevice, context: Context) {
        Timber.w("Connecting to ${device.address}")
        device.connectGatt(context, false, callback)
    }

    fun teardownConnection() {
        if (bluetoothGatt != null) {
            Timber.w("Disconnecting from ${bluetoothGatt?.device?.address}")
            bluetoothGatt?.close()
            bluetoothGatt = null
            listeners.forEach { it.get()?.onDisconnect?.invoke() }
        }
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

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            if (characteristic.isReadable()) {
                gatt.readCharacteristic(characteristic)
            } else {
                Timber.e("Attempting to read ${characteristic.uuid} that isn't readable!")
            }
        } ?: error("Not connected to a BLE device!")
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    private val callback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Timber.w("Successfully connected to $deviceAddress")
                    bluetoothGatt = gatt
                    Handler(Looper.getMainLooper()).post {
                        bluetoothGatt?.discoverServices()
                    }
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

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Timber.w("Discovered ${services.size} services for ${device.address}.")
                printGattTable()
                requestMtu(GATT_MAX_MTU_SIZE)
                listeners.forEach { it.get()?.onConnectionSetupComplete?.invoke(this) }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Timber.w("ATT MTU changed to $mtu, success: ${status == BluetoothGatt.GATT_SUCCESS}")
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Timber.i("Read characteristic $uuid | value: ${value.toHexString()}")
                        listeners.forEach { it.get()?.onCharacteristicRead?.invoke(this) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Timber.e("Read not permitted for $uuid!")
                    }
                    else -> {
                        Timber.e("Characteristic read failed for $uuid, error: $status")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Timber.i("Wrote to characteristic $uuid | value: ${value.toHexString()}")
                        listeners.forEach { it.get()?.onCharacteristicWrite?.invoke(this) }
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Timber.e("Write not permitted for $uuid!")
                    }
                    else -> {
                        Timber.e("Characteristic write failed for $uuid, error: $status")
                    }
                }
            }
        }
    }
}
