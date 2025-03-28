package isen.ledoux.androidsmartdevice

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class DeviceDetailsActivity : ComponentActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothDevice: BluetoothDevice? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val device = intent.getParcelableExtra<BluetoothDevice>("deviceName")

        setContent {
            val context = LocalContext.current

            // États pour les LEDs et la connexion
            var led1State by remember { mutableStateOf(false) }
            var led2State by remember { mutableStateOf(false) }
            var led3State by remember { mutableStateOf(false) }
            var isConnected by remember { mutableStateOf(false) }

            // États pour les compteurs des boutons physiques
            var button1Count by remember { mutableStateOf("") }
            var button3Count by remember { mutableStateOf("") }

            LaunchedEffect(device) {
                device?.let { bluetoothDevice = it }
            }

            DeviceDetailsScreen(
                deviceName = device?.name ?: "Appareil inconnu",
                led1State = led1State,
                led2State = led2State,
                led3State = led3State,
                button1Count = button1Count,
                button3Count = button3Count,
                onLedToggle = { ledId ->
                    when (ledId) {
                        1 -> led1State = !led1State
                        2 -> led2State = !led2State
                        3 -> led3State = !led3State
                    }
                    sendLedCommand(ledId, listOf(led1State, led2State, led3State)[ledId - 1])
                },
                onConnectClick = {
                    bluetoothDevice?.let { device ->
                        if (!isConnected) {
                            connectToDevice(device, context, { btn1, btn3 ->
                                button1Count = btn1.toString()
                                button3Count = btn3.toString()
                            })
                            isConnected = true
                        }
                    }
                },
                onDisconnectClick = {
                    bluetoothGatt?.close()
                    isConnected = false
                    Toast.makeText(context, "Déconnecté de l'appareil", Toast.LENGTH_SHORT).show()
                },
                isConnected = isConnected
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(
        device: BluetoothDevice,
        context: Context,
        updateCounts: (Int, Int) -> Unit
    ) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Le Bluetooth est désactivé", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothGatt = device.connectGatt(this, true, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                runOnUiThread {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Toast.makeText(context, "Connecté à ${device.name}", Toast.LENGTH_SHORT).show()
                        gatt?.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Toast.makeText(context, "Déconnecté", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Service 3, Caractéristique 2 (Bouton principal)
                    val service3 = gatt?.services?.getOrNull(2)
                    service3?.characteristics?.getOrNull(1)?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.getOrNull(0)?.let { descriptor ->
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }

                    // Service 2, Caractéristique 1 (Troisième bouton)
                    val service2 = gatt?.services?.getOrNull(1)
                    service2?.characteristics?.getOrNull(0)?.let { characteristic ->
                        gatt.setCharacteristicNotification(characteristic, true)
                        characteristic.descriptors?.getOrNull(0)?.let { descriptor ->
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
                characteristic?.let {
                    if (it.value != null && it.value.size >= 2) {
                        // Lier les états des boutons (inverser les indices si nécessaire)
                        val btn1State = it.value[1].toInt() // Bouton principal
                        val btn3State = it.value[0].toInt() // Troisième bouton

                        runOnUiThread {
                            updateCounts(btn1State, btn3State) // Mettre à jour les compteurs avec les états des boutons
                        }
                    }
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun sendLedCommand(ledId: Int, state: Boolean) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.services.getOrNull(2)
            service?.characteristics?.getOrNull(0)?.let {
                val ledCommand = if (state) ledId.toByte() else 0x00
                it.value = byteArrayOf(ledCommand)
                gatt.writeCharacteristic(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
    }
}

@Composable
fun DeviceDetailsScreen(
    deviceName: String,
    led1State: Boolean,
    led2State: Boolean,
    led3State: Boolean,
    button1Count: String,
    button3Count: String,
    onLedToggle: (Int) -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    isConnected: Boolean
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Device Info",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Nom de l'appareil : $deviceName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isConnected) onDisconnectClick() else onConnectClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = if (isConnected) "Se déconnecter" else "Se connecter")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Contrôle des LEDs",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LedButton(ledId = 1, isOn = led1State, onLedToggle = onLedToggle)
            LedButton(ledId = 2, isOn = led2State, onLedToggle = onLedToggle)
            LedButton(ledId = 3, isOn = led3State, onLedToggle = onLedToggle)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Compteurs de clics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bouton Principal : $button1Count clics",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Troisième Bouton : $button3Count clics",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notifications :",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
            }
        }
    }
}


@Composable
fun LedButton(ledId: Int, isOn: Boolean, onLedToggle: (Int) -> Unit) {
    Button(
        onClick = { onLedToggle(ledId) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(text = "LED $ledId - ${if (isOn) "ON" else "OFF"}")
    }
}
