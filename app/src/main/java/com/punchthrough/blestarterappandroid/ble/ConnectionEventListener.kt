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

package com.punchthrough.blestarterappandroid.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

/** A listener containing callback methods to be registered with [ConnectionManager].*/
class ConnectionEventListener {
    var onConnectionSetupComplete: ((gatt: BluetoothGatt) -> Unit)? = null

    var onDisconnect: ((device: BluetoothDevice) -> Unit)? = null

    var onDescriptorRead: (
        (
            device: BluetoothDevice,
            descriptor: BluetoothGattDescriptor,
            value: ByteArray
        ) -> Unit
    )? = null

    var onDescriptorWrite: (
        (
            device: BluetoothDevice,
            descriptor: BluetoothGattDescriptor
        ) -> Unit
    )? = null

    var onCharacteristicChanged: (
        (
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) -> Unit
    )? = null

    var onCharacteristicRead: (
        (
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) -> Unit
    )? = null

    var onCharacteristicWrite: (
        (
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic
        ) -> Unit
    )? = null

    var onNotificationsEnabled: (
        (
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic
        ) -> Unit
    )? = null

    var onNotificationsDisabled: (
        (
            device: BluetoothDevice,
            characteristic: BluetoothGattCharacteristic
        ) -> Unit
    )? = null

    var onMtuChanged: ((device: BluetoothDevice, newMtu: Int) -> Unit)? = null
}
