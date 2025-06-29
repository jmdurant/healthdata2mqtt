package com.healthdata.mqtt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import android.app.Activity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.healthdata.mqtt.service.BleHealthScannerService
import com.healthdata.mqtt.ui.theme.HealthDataMQTTTheme
import com.healthdata.mqtt.ui.SettingsScreen
import com.healthdata.mqtt.data.AppPreferences
import com.healthdata.mqtt.service.MQTTHealthDataPublisher
import com.healthdata.mqtt.service.MQTTConfig
import com.healthdata.mqtt.service.EmbeddedMQTTBrokerService

class MainActivity : ComponentActivity() {
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Some permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth enabled!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth is required for this app", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestRequiredPermissions()
        
        setContent {
            HealthDataMQTTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>().apply {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        }
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun enableBluetooth() {
        bluetoothAdapter?.let { adapter ->
            if (!adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBluetoothLauncher.launch(enableBtIntent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var isScanning by remember { mutableStateOf(false) }
    var deviceCount by remember { mutableStateOf(0) }
    var showSettingsScreen by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Health Data MQTT",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        // User Profile Display
        if (prefs.isUserProfileComplete()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Patient Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = prefs.getFullName(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "DOB: ${prefs.userDateOfBirth ?: "Not set"} • Age: ${prefs.userAge} • ${prefs.userSex.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${prefs.userEmail ?: "No email"} • Height: ${prefs.userHeight}cm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ User Profile Incomplete",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Please complete your profile in Settings for accurate health calculations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Bluetooth Health Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isScanning) "Scanning..." else "Not scanning",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = isScanning,
                        onCheckedChange = { checked ->
                            isScanning = checked
                            if (checked) {
                                startHealthScanService(context)
                            } else {
                                stopHealthScanService(context)
                            }
                        }
                    )
                }
                
                Text(
                    text = "Devices found: $deviceCount",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "MQTT Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "Broker: Not connected",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = "Last message: Never",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Button(
            onClick = {
                // Navigate to settings screen
                showSettingsScreen = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Settings")
        }
        
        Button(
            onClick = {
                try {
                    android.util.Log.d("MainActivity", "🔘 Test Connection button clicked")
                    Toast.makeText(context, "DEBUG: Button clicked, calling testMQTTConnection", Toast.LENGTH_SHORT).show()
                    testMQTTConnection(context, prefs)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "❌ Button click failed: ${e.message}", e)
                    Toast.makeText(context, "❌ Button click failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Connection")
        }
    }
    
    // Show settings screen if requested
    if (showSettingsScreen) {
        SettingsScreen(
            onNavigateBack = { showSettingsScreen = false }
        )
    }
}

private fun startHealthScanService(context: Context) {
    try {
        // Check if we have all required permissions before starting
        val bluetoothPermissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val missingPermissions = bluetoothPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Toast.makeText(context, "Missing permissions: ${missingPermissions.joinToString()}", Toast.LENGTH_LONG).show()
            return
        }
        
        // Check if Bluetooth is enabled
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Please enable Bluetooth first", Toast.LENGTH_LONG).show()
            return
        }
        
        val intent = Intent(context, BleHealthScannerService::class.java)
        context.startForegroundService(intent)
        Toast.makeText(context, "Health scanning started", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to start scanning: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun stopHealthScanService(context: Context) {
    try {
        val intent = Intent(context, BleHealthScannerService::class.java)
        context.stopService(intent)
        Toast.makeText(context, "Health scanning stopped", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to stop scanning: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun testMQTTConnection(context: Context, prefs: AppPreferences) {
    try {
        android.util.Log.d("MainActivity", "🔍 testMQTTConnection called")
        Toast.makeText(context, "DEBUG: testMQTTConnection function entered", Toast.LENGTH_SHORT).show()
        Toast.makeText(context, "🔍 Starting basic MQTT test...", Toast.LENGTH_SHORT).show()
        
        // Simple test without creating any MQTT objects
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                Toast.makeText(context, "✅ Basic test passed - attempting MQTT...", Toast.LENGTH_SHORT).show()
                
                // Now try the actual MQTT test
                if (prefs.isUsingInternalBroker()) {
                    // Check if broker is already running
                    if (EmbeddedMQTTBrokerService.isBrokerRunning(1883)) {
                        Toast.makeText(context, "✅ Embedded broker already running", Toast.LENGTH_SHORT).show()
                        simpleConnectionTest(context, "localhost", 1883)
                    } else {
                        Toast.makeText(context, "📱 Starting embedded broker...", Toast.LENGTH_SHORT).show()
                        android.util.Log.i("MainActivity", "📱 Calling startEmbeddedBroker")
                        startEmbeddedBroker(context)
                        
                        // Wait and verify broker startup
                        android.util.Log.i("MainActivity", "⏳ Calling waitForBrokerAndTest")
                        waitForBrokerAndTest(context, 1883, 0)
                    }
                } else {
                    val broker = prefs.mqttHost
                    val port = prefs.mqttPort
                    
                    if (broker.isBlank()) {
                        Toast.makeText(context, "❌ Please configure MQTT broker in Settings", Toast.LENGTH_LONG).show()
                        return@postDelayed
                    }
                    
                    simpleConnectionTest(context, broker, port)
                }
                
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Handler error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 1000)
        
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Test setup error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun simpleConnectionTest(context: Context, broker: String, port: Int) {
    try {
        android.util.Log.d("MainActivity", "🌐 Starting connection test to $broker:$port")
        Toast.makeText(context, "🌐 DEBUG: Starting connection test to $broker:$port", Toast.LENGTH_SHORT).show()
        
        // Add timeout handler to prevent hanging
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isTestComplete = false
        
        val timeoutRunnable = Runnable {
            if (!isTestComplete) {
                isTestComplete = true
                android.util.Log.e("MainActivity", "❌ Connection test timed out after 15 seconds")
                Toast.makeText(context, "❌ Connection test timed out - broker may not be running", Toast.LENGTH_LONG).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000) // 15 second timeout
        
        // Try creating just the MQTT config first
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                android.util.Log.d("MainActivity", "📋 Creating MQTT config")
                val config = MQTTConfig(
                    brokerHost = broker,
                    brokerPort = port,
                    username = null,
                    password = null
                )
                android.util.Log.d("MainActivity", "✅ Config created: ${config.clientId}")
                Toast.makeText(context, "✅ Config created, testing publisher...", Toast.LENGTH_SHORT).show()
                
                // Try creating the publisher
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        android.util.Log.d("MainActivity", "🔧 Creating HiveMQ publisher...")
                        Toast.makeText(context, "🔧 DEBUG: Creating HiveMQ publisher...", Toast.LENGTH_SHORT).show()
                        
                        val testPublisher = try {
                            MQTTHealthDataPublisher(context, config)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "❌ Publisher creation failed: ${e.javaClass.simpleName}: ${e.message}", e)
                            Toast.makeText(context, "❌ Publisher creation failed: ${e.message}", Toast.LENGTH_LONG).show()
                            return@postDelayed
                        }
                        
                        android.util.Log.d("MainActivity", "✅ HiveMQ publisher created successfully!")
                        Toast.makeText(context, "✅ HiveMQ publisher created successfully! Testing basic operations...", Toast.LENGTH_LONG).show()
                        Toast.makeText(context, "DEBUG: About to start delayed connection test", Toast.LENGTH_SHORT).show()
                        
                        // Add a small delay to ensure UI updates, then test connection
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                android.util.Log.d("MainActivity", "🔧 Starting HiveMQ connection test...")
                                Toast.makeText(context, "🔧 DEBUG: Starting HiveMQ connection test...", Toast.LENGTH_SHORT).show()
                                Toast.makeText(context, "DEBUG: About to call testPublisher.connect()", Toast.LENGTH_SHORT).show()
                                testPublisher.connect(object : MQTTHealthDataPublisher.ConnectionCallback {
                                override fun onConnected() {
                                    if (!isTestComplete) {
                                        isTestComplete = true
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        android.util.Log.i("MainActivity", "🎉 MQTT connection successful!")
                                        Toast.makeText(context, "🎉 MQTT connection successful!", Toast.LENGTH_LONG).show()
                                        testPublisher.disconnect()
                                    }
                                }
                                
                                override fun onConnectionFailed(error: String) {
                                    if (!isTestComplete) {
                                        isTestComplete = true
                                        timeoutHandler.removeCallbacks(timeoutRunnable)
                                        android.util.Log.e("MainActivity", "❌ Connection failed: $error")
                                        Toast.makeText(context, "❌ Connection failed: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                                
                                override fun onDisconnected() {
                                    android.util.Log.d("MainActivity", "📱 Disconnected from MQTT broker")
                                    Toast.makeText(context, "📱 Disconnected", Toast.LENGTH_SHORT).show()
                                }
                            })
                            } catch (e: Exception) {
                                if (!isTestComplete) {
                                    isTestComplete = true
                                    timeoutHandler.removeCallbacks(timeoutRunnable)
                                    android.util.Log.e("MainActivity", "❌ Connection test exception: ${e.message}", e)
                                    Toast.makeText(context, "❌ Connection test failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }, 500) // Small delay to ensure UI updates
                    } catch (e: Exception) {
                        if (!isTestComplete) {
                            isTestComplete = true
                            timeoutHandler.removeCallbacks(timeoutRunnable)
                            android.util.Log.e("MainActivity", "❌ Publisher creation failed: ${e.message}", e)
                            Toast.makeText(context, "❌ Publisher creation failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }, 1000)
                
            } catch (e: Exception) {
                if (!isTestComplete) {
                    isTestComplete = true
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    android.util.Log.e("MainActivity", "❌ Config creation failed: ${e.message}", e)
                    Toast.makeText(context, "❌ Config creation failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }, 1000)
        
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Test setup error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun waitForBrokerAndTest(context: Context, port: Int, attempt: Int) {
    val maxAttempts = 15 // Maximum 15 attempts (15 seconds)
    
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        if (EmbeddedMQTTBrokerService.isBrokerRunning(port)) {
            android.util.Log.i("MainActivity", "✅ Broker is ready after ${attempt + 1} attempts")
            Toast.makeText(context, "✅ Broker ready, testing connection...", Toast.LENGTH_SHORT).show()
            simpleConnectionTest(context, "localhost", port)
        } else if (attempt < maxAttempts) {
            android.util.Log.d("MainActivity", "⏳ Waiting for broker... attempt ${attempt + 1}/$maxAttempts")
            Toast.makeText(context, "⏳ Waiting for broker startup... (${attempt + 1}/$maxAttempts)", Toast.LENGTH_SHORT).show()
            waitForBrokerAndTest(context, port, attempt + 1)
        } else {
            android.util.Log.e("MainActivity", "❌ Broker failed to start after $maxAttempts attempts")
            Toast.makeText(context, "❌ Embedded broker failed to start", Toast.LENGTH_LONG).show()
        }
    }, 1000) // Wait 1 second between attempts
}

private fun testConnection(context: Context, broker: String, port: Int, username: String?, password: String?) {
    try {
        Toast.makeText(context, "Testing MQTT connection to $broker:$port...", Toast.LENGTH_SHORT).show()
        
        // Add a timeout handler to prevent hanging
        val timeoutHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isTestComplete = false
        
        val timeoutRunnable = Runnable {
            if (!isTestComplete) {
                isTestComplete = true
                Toast.makeText(context, "❌ Connection test timed out", Toast.LENGTH_LONG).show()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000) // 15 second timeout
        
        try {
            // Create MQTT config and test publisher
            val config = MQTTConfig(
                brokerHost = broker,
                brokerPort = port,
                username = username,
                password = password
            )
            val testPublisher = MQTTHealthDataPublisher(context, config)
        
            testPublisher.connect(object : MQTTHealthDataPublisher.ConnectionCallback {
                override fun onConnected() {
                    if (!isTestComplete) {
                        isTestComplete = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "✅ MQTT connection successful!", Toast.LENGTH_LONG).show()
                        }
                        testPublisher.disconnect()
                    }
                }
                
                override fun onConnectionFailed(error: String) {
                    if (!isTestComplete) {
                        isTestComplete = true
                        timeoutHandler.removeCallbacks(timeoutRunnable)
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(context, "❌ MQTT connection failed: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                
                override fun onDisconnected() {
                    // Connection test complete
                }
            })
            
        } catch (e: Exception) {
            isTestComplete = true
            timeoutHandler.removeCallbacks(timeoutRunnable)
            Toast.makeText(context, "❌ MQTT client creation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Connection test error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun startEmbeddedBroker(context: Context) {
    try {
        android.util.Log.i("MainActivity", "🚀 startEmbeddedBroker called")
        val intent = Intent(context, EmbeddedMQTTBrokerService::class.java)
        intent.action = EmbeddedMQTTBrokerService.ACTION_START_BROKER
        intent.putExtra(EmbeddedMQTTBrokerService.EXTRA_PORT, 1883)
        intent.putExtra(EmbeddedMQTTBrokerService.EXTRA_ALLOW_ANONYMOUS, true)
        android.util.Log.i("MainActivity", "📤 Starting service with action: ${intent.action}")
        context.startService(intent)
        android.util.Log.i("MainActivity", "✅ Service start initiated")
        Toast.makeText(context, "📱 Starting embedded MQTT broker...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.util.Log.e("MainActivity", "❌ Failed to start broker service", e)
        Toast.makeText(context, "❌ Failed to start embedded broker: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun stopEmbeddedBroker(context: Context) {
    try {
        val intent = Intent(context, EmbeddedMQTTBrokerService::class.java)
        intent.action = EmbeddedMQTTBrokerService.ACTION_STOP_BROKER
        context.startService(intent)
        Toast.makeText(context, "📱 Stopping embedded MQTT broker...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "❌ Failed to stop embedded broker: ${e.message}", Toast.LENGTH_LONG).show()
    }
}