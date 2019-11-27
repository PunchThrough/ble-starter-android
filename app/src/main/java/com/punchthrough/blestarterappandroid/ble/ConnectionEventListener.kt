package com.punchthrough.blestarterappandroid.ble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

/**
 * A listener containing callback methods to be registered with [ConnectionManager].
 */
class ConnectionEventListener {
    var onConnectionSetupComplete: ((BluetoothGatt) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null
    var onDescriptorRead: ((BluetoothGattDescriptor) -> Unit)? = null
    var onDescriptorWrite: ((BluetoothGattDescriptor) -> Unit)? = null
    var onCharacteristicChanged: ((BluetoothGattCharacteristic) -> Unit)? = null
    var onCharacteristicRead: ((BluetoothGattCharacteristic) -> Unit)? = null
    var onCharacteristicWrite: ((BluetoothGattCharacteristic) -> Unit)? = null
}
