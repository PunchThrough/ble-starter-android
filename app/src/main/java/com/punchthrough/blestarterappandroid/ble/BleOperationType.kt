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
import android.content.Context
import java.util.UUID

/** Abstract sealed class representing a type of BLE operation */
sealed class BleOperationType {
    abstract val device: BluetoothDevice
}

/** Connect to [device] and perform service discovery */
data class Connect(override val device: BluetoothDevice, val context: Context) : BleOperationType()

/** Disconnect from [device] and release all connection resources */
data class Disconnect(override val device: BluetoothDevice) : BleOperationType()

/** Write [payload] as the value of a characteristic represented by [characteristicUuid] */
data class CharacteristicWrite(
    override val device: BluetoothDevice,
    val characteristicUuid: UUID,
    val writeType: Int,
    val payload: ByteArray
) : BleOperationType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CharacteristicWrite

        if (device != other.device) return false
        if (characteristicUuid != other.characteristicUuid) return false
        if (writeType != other.writeType) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + characteristicUuid.hashCode()
        result = 31 * result + writeType
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/** Read the value of a characteristic represented by [characteristicUuid] */
data class CharacteristicRead(
    override val device: BluetoothDevice,
    val characteristicUuid: UUID
) : BleOperationType()

/** Write [payload] as the value of a descriptor represented by [descriptorUuid] */
data class DescriptorWrite(
    override val device: BluetoothDevice,
    val descriptorUuid: UUID,
    val payload: ByteArray
) : BleOperationType() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DescriptorWrite

        if (device != other.device) return false
        if (descriptorUuid != other.descriptorUuid) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = device.hashCode()
        result = 31 * result + descriptorUuid.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/** Read the value of a descriptor represented by [descriptorUuid] */
data class DescriptorRead(
    override val device: BluetoothDevice,
    val descriptorUuid: UUID
) : BleOperationType()

/** Enable notifications/indications on a characteristic represented by [characteristicUuid] */
data class EnableNotifications(
    override val device: BluetoothDevice,
    val characteristicUuid: UUID
) : BleOperationType()

/** Disable notifications/indications on a characteristic represented by [characteristicUuid] */
data class DisableNotifications(
    override val device: BluetoothDevice,
    val characteristicUuid: UUID
) : BleOperationType()

/** Request for an MTU of [mtu] */
data class MtuRequest(
    override val device: BluetoothDevice,
    val mtu: Int
) : BleOperationType()
