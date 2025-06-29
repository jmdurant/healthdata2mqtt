package com.healthdata.mqtt.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.healthdata.mqtt.data.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    
    // MQTT Settings
    var mqttBrokerMode by remember { mutableStateOf(prefs.mqttBrokerMode) }
    var mqttHost by remember { mutableStateOf(prefs.mqttHost) }
    var mqttPort by remember { mutableStateOf(prefs.mqttPort.toString()) }
    var mqttUsername by remember { mutableStateOf(prefs.mqttUsername ?: "") }
    var mqttPassword by remember { mutableStateOf(prefs.mqttPassword ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    
    // User Profile
    var userFirstName by remember { mutableStateOf(prefs.userFirstName ?: "") }
    var userLastName by remember { mutableStateOf(prefs.userLastName ?: "") }
    var userEmail by remember { mutableStateOf(prefs.userEmail ?: "") }
    var userDateOfBirth by remember { mutableStateOf(prefs.userDateOfBirth ?: "") }
    var userHeight by remember { mutableStateOf(prefs.userHeight.toString()) }
    var userAge by remember { mutableStateOf(prefs.userAge.toString()) }
    var userSex by remember { mutableStateOf(prefs.userSex) }
    
    // Settings
    var bloodPressureStandard by remember { mutableStateOf(prefs.bloodPressureStandard) }
    var autoScan by remember { mutableStateOf(prefs.autoScan) }
    var backgroundScan by remember { mutableStateOf(prefs.backgroundScan) }
    var publishRawData by remember { mutableStateOf(prefs.publishRawData) }
    
    var showSaveDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // App Bar
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                TextButton(
                    onClick = {
                        // Save all settings
                        prefs.mqttBrokerMode = mqttBrokerMode
                        prefs.mqttHost = mqttHost
                        prefs.mqttPort = mqttPort.toIntOrNull() ?: 1883
                        prefs.mqttUsername = mqttUsername.ifBlank { null }
                        prefs.mqttPassword = mqttPassword.ifBlank { null }
                        
                        prefs.userFirstName = userFirstName.ifBlank { null }
                        prefs.userLastName = userLastName.ifBlank { null }
                        prefs.userEmail = userEmail.ifBlank { null }
                        prefs.userDateOfBirth = userDateOfBirth.ifBlank { null }
                        prefs.userHeight = userHeight.toIntOrNull() ?: 170
                        prefs.userAge = userAge.toIntOrNull() ?: 30
                        prefs.userSex = userSex
                        
                        prefs.bloodPressureStandard = bloodPressureStandard
                        prefs.autoScan = autoScan
                        prefs.backgroundScan = backgroundScan
                        prefs.publishRawData = publishRawData
                        
                        showSaveDialog = true
                    }
                ) {
                    Text("Save")
                }
            }
        )
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // MQTT Configuration Section
            SettingsSection(title = "MQTT Broker Configuration") {
                // Broker Mode Selection
                Text(
                    text = "Broker Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (mqttBrokerMode == "internal"),
                                onClick = { mqttBrokerMode = "internal" },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mqttBrokerMode == "internal"),
                            onClick = null
                        )
                        Text(
                            text = "Internal",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (mqttBrokerMode == "external"),
                                onClick = { mqttBrokerMode = "external" },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (mqttBrokerMode == "external"),
                            onClick = null
                        )
                        Text(
                            text = "External",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (mqttBrokerMode == "internal") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "📱 Using embedded MQTT broker on localhost:1883",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = mqttHost,
                        onValueChange = { mqttHost = it },
                        label = { Text("Broker Host") },
                        placeholder = { Text("localhost or 192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = mqttPort,
                        onValueChange = { mqttPort = it },
                        label = { Text("Broker Port") },
                        placeholder = { Text("1883") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = mqttUsername,
                        onValueChange = { mqttUsername = it },
                        label = { Text("Username (Optional)") },
                        placeholder = { Text("healthuser") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = mqttPassword,
                        onValueChange = { mqttPassword = it },
                        label = { Text("Password (Optional)") },
                        placeholder = { Text("Enter password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        singleLine = true
                    )
                }
            }
            
            // User Profile Section
            SettingsSection(title = "User Profile") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = userFirstName,
                        onValueChange = { userFirstName = it },
                        label = { Text("First Name") },
                        placeholder = { Text("John") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = userLastName,
                        onValueChange = { userLastName = it },
                        label = { Text("Last Name") },
                        placeholder = { Text("Doe") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("john.doe@example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = userDateOfBirth,
                    onValueChange = { userDateOfBirth = it },
                    label = { Text("Date of Birth") },
                    placeholder = { Text("1990-01-15 (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = userHeight,
                        onValueChange = { userHeight = it },
                        label = { Text("Height (cm)") },
                        placeholder = { Text("170") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = userAge,
                        onValueChange = { userAge = it },
                        label = { Text("Age") },
                        placeholder = { Text("30") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                
                Text(
                    text = "Sex",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row {
                    RadioButtonOption(
                        text = "Male",
                        selected = userSex == "male",
                        onClick = { userSex = "male" }
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    RadioButtonOption(
                        text = "Female",
                        selected = userSex == "female",
                        onClick = { userSex = "female" }
                    )
                }
            }
            
            // Health Settings Section
            SettingsSection(title = "Health Settings") {
                Text(
                    text = "Blood Pressure Standard",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Row {
                    RadioButtonOption(
                        text = "European (EU)",
                        selected = bloodPressureStandard == "eu",
                        onClick = { bloodPressureStandard = "eu" }
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    RadioButtonOption(
                        text = "US/American",
                        selected = bloodPressureStandard == "us",
                        onClick = { bloodPressureStandard = "us" }
                    )
                }
            }
            
            // App Behavior Section
            SettingsSection(title = "App Behavior") {
                SwitchSetting(
                    title = "Auto-start Scanning",
                    description = "Automatically start BLE scanning when app opens",
                    checked = autoScan,
                    onCheckedChange = { autoScan = it }
                )
                
                SwitchSetting(
                    title = "Background Scanning",
                    description = "Continue scanning in background (uses more battery)",
                    checked = backgroundScan,
                    onCheckedChange = { backgroundScan = it }
                )
                
                SwitchSetting(
                    title = "Publish Raw Data",
                    description = "Send raw device data to MQTT for debugging",
                    checked = publishRawData,
                    onCheckedChange = { publishRawData = it }
                )
            }
            
            // Connection Test Section
            SettingsSection(title = "Connection Test") {
                OutlinedButton(
                    onClick = {
                        // TODO: Implement connection test
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Test MQTT Connection")
                }
                
                Text(
                    text = "Tests connection to MQTT broker with current settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Advanced Section
            SettingsSection(title = "Advanced") {
                OutlinedButton(
                    onClick = {
                        prefs.resetToDefaults()
                        // Reset UI state
                        mqttHost = "localhost"
                        mqttPort = "1883"
                        mqttUsername = ""
                        mqttPassword = ""
                        userFirstName = ""
                        userLastName = ""
                        userEmail = ""
                        userDateOfBirth = ""
                        userHeight = "170"
                        userAge = "30"
                        userSex = "male"
                        bloodPressureStandard = "eu"
                        autoScan = true
                        backgroundScan = true
                        publishRawData = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Defaults")
                }
                
                Text(
                    text = "Resets all settings to default values",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    // Save confirmation dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Settings Saved") },
            text = { Text("Your settings have been saved successfully. Restart scanning to apply MQTT changes.") },
            confirmButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            content()
        }
    }
}

@Composable
fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}