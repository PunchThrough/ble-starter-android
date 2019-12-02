package com.punchthrough.blestarterappandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.punchthrough.blestarterappandroid.ble.ConnectionManager

class BleOperationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble_operations)
    }

    override fun onDestroy() {
        ConnectionManager.teardownConnection()
        super.onDestroy()
    }
}
