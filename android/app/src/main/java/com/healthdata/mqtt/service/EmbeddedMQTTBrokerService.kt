package com.healthdata.mqtt.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import io.moquette.broker.Server
import io.moquette.broker.config.IConfig
import io.moquette.broker.config.MemoryConfig
import java.util.Properties

class EmbeddedMQTTBrokerService : Service() {
    private val TAG = "EmbeddedMQTTBroker"
    private var mqttBroker: Server? = null
    private var isRunning = false
    
    companion object {
        const val DEFAULT_PORT = 1883
        const val ACTION_START_BROKER = "START_BROKER"
        const val ACTION_STOP_BROKER = "STOP_BROKER"
        const val ACTION_CHECK_STATUS = "CHECK_STATUS"
        const val EXTRA_PORT = "port"
        const val EXTRA_ALLOW_ANONYMOUS = "allow_anonymous"
        
        fun isBrokerRunning(port: Int = DEFAULT_PORT): Boolean {
            // For Android, we can't reliably test localhost connections
            // Check if the service exists and isRunning flag is set
            return serviceInstance?.isRunning == true
        }
        
        private var serviceInstance: EmbeddedMQTTBrokerService? = null
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🔧 EmbeddedMQTTBrokerService onCreate called")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    init {
        Log.i(TAG, "🏗️ EmbeddedMQTTBrokerService created")
        serviceInstance = this
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "📥 onStartCommand called with action: ${intent?.action}")
        Log.i(TAG, "📥 Intent extras: port=${intent?.getIntExtra(EXTRA_PORT, -1)}, allowAnonymous=${intent?.getBooleanExtra(EXTRA_ALLOW_ANONYMOUS, false)}")
        
        when (intent?.action) {
            ACTION_START_BROKER -> {
                Log.i(TAG, "🚀 ACTION_START_BROKER received")
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                val allowAnonymous = intent.getBooleanExtra(EXTRA_ALLOW_ANONYMOUS, true)
                Log.i(TAG, "🔧 Starting broker with port=$port, allowAnonymous=$allowAnonymous")
                startBroker(port, allowAnonymous)
            }
            ACTION_STOP_BROKER -> {
                Log.i(TAG, "🛑 ACTION_STOP_BROKER received")
                stopBroker()
            }
            else -> {
                Log.w(TAG, "⚠️ Unknown action received: ${intent?.action}")
            }
        }
        return START_STICKY
    }
    
    private fun startBroker(port: Int = DEFAULT_PORT, allowAnonymous: Boolean = true) {
        if (isRunning) {
            Log.i(TAG, "MQTT broker already running")
            return
        }
        
        try {
            Log.i(TAG, "🚀 Starting embedded MQTT broker on port $port")
            
            // Create minimal broker configuration for Android
            Log.d(TAG, "Creating minimal broker properties...")
            val properties = Properties().apply {
                // Basic MQTT settings
                setProperty("port", port.toString())
                setProperty("host", "0.0.0.0")
                setProperty("allow_anonymous", allowAnonymous.toString())
                
                // Use memory-only persistence (no file I/O)
                setProperty("persistence_enabled", "false")
                setProperty("persistent_queue_type", "memory")
                
                // Disable authentication files
                setProperty("password_file", "")
                setProperty("acl_file", "")
                
                // Force NIO transport for Android compatibility
                setProperty("netty.epoll", "false")
                setProperty("netty.kqueue", "false")
            }
            
            Log.d(TAG, "Creating MemoryConfig with ${properties.size} properties")
            val config: IConfig = MemoryConfig(properties)
            
            // Create and start broker with minimal config
            Log.d(TAG, "Creating Moquette Server...")
            mqttBroker = Server()
            
            Log.d(TAG, "Starting Moquette server...")
            mqttBroker!!.startServer(config)
            
            isRunning = true
            Log.i(TAG, "✅ Embedded MQTT broker started successfully on port $port")
            
            // Enhanced broker verification with multiple attempts
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val brokerStatus = if (mqttBroker != null) "Server object exists" else "Server object is null"
                    Log.i(TAG, "🔍 Broker status check: isRunning=$isRunning, $brokerStatus")
                    
                    // Try multiple verification attempts
                    for (attempt in 1..3) {
                        val socket = java.net.Socket()
                        try {
                            socket.connect(java.net.InetSocketAddress("localhost", port), 2000)
                            Log.i(TAG, "✅ Broker port $port is accepting connections (attempt $attempt)")
                            socket.close()
                            break
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ Broker port $port not ready on attempt $attempt: ${e.message}")
                            if (attempt < 3) Thread.sleep(500)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Broker verification failed: ${e.message}", e)
                }
            }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start embedded MQTT broker: ${e.javaClass.simpleName}: ${e.message}", e)
            e.printStackTrace()
            isRunning = false
            mqttBroker = null
        }
    }
    
    private fun stopBroker() {
        try {
            if (isRunning && mqttBroker != null) {
                Log.i(TAG, "Stopping embedded MQTT broker")
                mqttBroker!!.stopServer()
                mqttBroker = null
                isRunning = false
                Log.i(TAG, "✅ Embedded MQTT broker stopped")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping embedded MQTT broker", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopBroker()
        serviceInstance = null
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun getBrokerPort(): Int = DEFAULT_PORT
}