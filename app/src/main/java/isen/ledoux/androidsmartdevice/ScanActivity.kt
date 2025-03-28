package isen.ledoux.androidsmartdevice

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import isen.ledoux.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme

class ScanActivity : ComponentActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val devices = mutableStateListOf<BluetoothDevice>()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (hasAllPermissions(this)) {
                startScan()
            } else {
                Toast.makeText(this, "Permissions refusées", Toast.LENGTH_SHORT).show()
            }
        }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (!devices.any { it.address == device.address } && !device.name.isNullOrEmpty()) {
                    devices.add(device)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScan() {
        if (hasAllPermissions(this)) {
            bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
        } else {
            Toast.makeText(this, "Permissions manquantes", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (hasAllPermissions(this)) {
            bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    private fun hasAllPermissions(context: Context): Boolean {
        val requiredPermissions = getPermissionsToRequest()
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getPermissionsToRequest(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestPermissions() {
        val permissionsToRequest = getPermissionsToRequest().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            startScan()
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        setContent {
            AndroidSmartDeviceTheme {
                var isScanning by remember { mutableStateOf(false) }
                var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }

                if (selectedDevice != null) {
                    DeviceControlScreen(
                        device = selectedDevice!!,
                        onBack = {
                            selectedDevice = null
                        }
                    )
                } else {
                    ScanScreen(
                        isScanning = isScanning,
                        devices = devices,
                        onScanToggle = {
                            if (isScanning) stopScan() else requestPermissions()
                            isScanning = !isScanning
                        },
                        onDeviceClick = {
                            stopScan()
                            selectedDevice = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanScreen(
    isScanning: Boolean,
    devices: List<BluetoothDevice>,
    onScanToggle: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Scan BLE", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onScanToggle) {
            Text(if (isScanning) "Arrêter le scan" else "Démarrer le scan")
        }

        Spacer(modifier = Modifier.height(16.dp))
        if (isScanning) CircularProgressIndicator()

        devices.forEach { device ->
            Button(
                onClick = { onDeviceClick(device) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(text = "Nom : ${device.name}")
            }
        }
    }
}
