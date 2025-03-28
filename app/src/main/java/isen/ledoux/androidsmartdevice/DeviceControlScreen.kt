package isen.ledoux.androidsmartdevice

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun DeviceControlScreen(
    device: BluetoothDevice,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var bluetoothGatt: BluetoothGatt? = null
    var isConnected by remember { mutableStateOf(false) }
    var ledStates by remember { mutableStateOf(listOf(false, false, false)) }
    var clickCount3 by remember { mutableStateOf(0) }

    /**
     * Active les notifications en écrivant dans le descripteur CCCD
     */
    fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        descriptor?.let {
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }

    LaunchedEffect(Unit) {
        bluetoothGatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                isConnected = true

                // Active les notifications sur le bouton 3
                val charButton3 = gatt.services.getOrNull(3)?.characteristics?.getOrNull(0)
                charButton3?.let {
                    enableNotification(gatt, it)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                val targetChar = gatt.services.getOrNull(3)?.characteristics?.getOrNull(0)
                if (characteristic.uuid == targetChar?.uuid) {
                    clickCount3++
                }
            }
        })
    }

    fun toggleLed(index: Int) {
        val ledOn = ledStates[index]
        val valueToWrite = if (!ledOn) {
            when (index) {
                0 -> 0x01
                1 -> 0x02
                2 -> 0x03
                else -> 0x00
            }
        } else {
            0x00
        }

        val service = bluetoothGatt?.services?.getOrNull(2)
        val characteristic = service?.characteristics?.getOrNull(0)
        characteristic?.value = byteArrayOf(valueToWrite.toByte())
        bluetoothGatt?.writeCharacteristic(characteristic)

        ledStates = ledStates.toMutableList().also { it[index] = !ledOn }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    modifier = Modifier
                        .clickable {
                            bluetoothGatt?.disconnect()
                            bluetoothGatt?.close()
                            onBack()
                        }
                        .padding(end = 16.dp)
                )
                Text("Contrôle de ${device.name}", style = MaterialTheme.typography.titleLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isConnected) {
                CircularProgressIndicator()
                Text("Connexion BLE en cours...", modifier = Modifier.padding(top = 8.dp))
            } else {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(120.dp)
                )
                Text("Connecté à ${device.address}", fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))
                Text("LEDs", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0..2) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "LED ${i + 1}",
                            tint = if (ledStates[i]) Color.Green else Color.Gray,
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { toggleLed(i) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Clics bouton 3 : $clickCount3", fontSize = 16.sp)
            }
        }
    }
}
