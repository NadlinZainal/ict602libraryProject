package com.example.ict602library

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ict602library.ui.theme.Ict602libraryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ict602libraryTheme {
                LibraryApp()
            }
        }
    }
}

@Composable
fun LibraryApp() {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(checkPermissions(context)) }
    var showRules by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    
    val isEmulator = Build.FINGERPRINT.contains("generic") || Build.MODEL.contains("Emulator") || Build.MODEL.contains("Android SDK built for x86")

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasPermissions) {
                Text(text = "Permissions required for BLE scanning")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    permissionLauncher.launch(getRequiredPermissions())
                }) {
                    Text("Grant Permissions")
                }
            } else {
                Text(
                    text = if (isScanning) "Scanning for Library Beacon..." else "Scanner Idle",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { isScanning = !isScanning }) {
                    Text(if (isScanning) "Stop Scanning" else "Start Scanning")
                }

                if (isEmulator) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("Emulator Detected", color = Color.Gray)
                    Button(
                        onClick = { showRules = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Simulate Beacon Found")
                    }
                }
            }
        }

        if (showRules) {
            LibraryRulesDialog(onDismiss = { showRules = false })
        }
    }

    if (hasPermissions && isScanning) {
        BleScannerEffect(onBeaconDetected = {
            showRules = true
            isScanning = false
        })
    }
}

@Composable
fun BleScannerEffect(onBeaconDetected: () -> Unit) {
    val context = LocalContext.current
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    val scanner = bluetoothAdapter?.bluetoothLeScanner

    // --- CONFIGURE YOUR BEACON HERE ---
    val targetName = "LibraryBeacon" // Change this to your beacon's name
    val targetAddress = "AA:BB:CC:11:22:33" // Optional: Change to your beacon's MAC address
    // ----------------------------------

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val deviceName = result.device.name
                val deviceAddress = result.device.address
                
                Log.d("BleScanner", "Found: $deviceName ($deviceAddress)")

                // Check by Name OR Address
                if (deviceName == targetName || deviceAddress == targetAddress) {
                    onBeaconDetected()
                }
            }
        }
    }

    DisposableEffect(scanner) {
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                scanner?.startScan(scanCallback)
            }
        } catch (e: Exception) {
            Log.e("BleScanner", "Scan error", e)
        }
        
        onDispose {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    scanner?.stopScan(scanCallback)
                }
            } catch (e: Exception) { /* Ignore */ }
        }
    }
}

@Composable
fun LibraryRulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Welcome to the Library") },
        text = {
            Column {
                Text(text = "Please follow the rules:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "• No food or drinks")
                Text(text = "• Keep quiet")
                Text(text = "• Silence your phone")
                Text(text = "• Handle books with care")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("I Understand")
            }
        }
    )
}

fun checkPermissions(context: Context): Boolean {
    return getRequiredPermissions().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}

fun getRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }
}
