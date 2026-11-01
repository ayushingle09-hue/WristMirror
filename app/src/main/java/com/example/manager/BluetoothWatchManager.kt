package com.example.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.model.GattCharacteristicInfo
import com.example.model.GattDiagnosticReport
import com.example.model.GattServiceInfo
import com.example.model.WatchDevice
import com.example.model.WatchEcosystemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class BluetoothWatchManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<WatchDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<WatchDevice>> = _discoveredDevices.asStateFlow()

    private val _connectedDevice = MutableStateFlow<WatchDevice?>(null)
    val connectedDevice: StateFlow<WatchDevice?> = _connectedDevice.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<GattDiagnosticReport?>(null)
    val diagnosticReport: StateFlow<GattDiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Disconnected")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val BATTERY_SERVICE_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    private val BATTERY_LEVEL_CHAR_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    private val DEVICE_INFO_SERVICE_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    private val CURRENT_TIME_SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")

    // Xiaomi / Huami proprietary authentication and service UUIDs
    private val XIAOMI_AUTH_SERVICE_1 = "0000fee0-0000-1000-8000-00805f9b34fb"
    private val XIAOMI_AUTH_SERVICE_2 = "0000fee1-0000-1000-8000-00805f9b34fb"

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val name = try { device.name ?: "Unknown Smartwatch" } catch (_: SecurityException) { "Unknown Smartwatch" }
            val address = device.address ?: return
            val rssi = result.rssi

            val currentList = _discoveredDevices.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.address == address }
            val watchDevice = WatchDevice(
                name = name,
                address = address,
                rssi = rssi,
                isBonded = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false }
            )

            if (existingIndex >= 0) {
                currentList[existingIndex] = watchDevice
            } else {
                currentList.add(watchDevice)
            }
            _discoveredDevices.value = currentList.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _connectionStatus.value = "Scan failed (error $errorCode)"
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            mainHandler.post {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionStatus.value = "Connected. Discovering services..."
                        val device = gatt?.device
                        val name = device?.name ?: "Connected Watch"
                        val address = device?.address ?: ""
                        _connectedDevice.value = WatchDevice(
                            name = name,
                            address = address,
                            rssi = -60,
                            isConnected = true
                        )
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        _connectionStatus.value = "Disconnected"
                        _connectedDevice.value = null
                        activeGatt?.close()
                        activeGatt = null
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _connectionStatus.value = "Connecting to device..."
                    }
                    else -> {
                        _connectionStatus.value = "Connection state: $newState"
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                mainHandler.post {
                    _connectionStatus.value = "Services discovered. Analyzing compatibility..."
                    analyzeServices(gatt)

                    // Try to read battery characteristic if available
                    val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
                    val batteryChar = batteryService?.getCharacteristic(BATTERY_LEVEL_CHAR_UUID)
                    if (batteryChar != null) {
                        gatt.readCharacteristic(batteryChar)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                if (characteristic.uuid == BATTERY_LEVEL_CHAR_UUID) {
                    val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                    mainHandler.post {
                        _connectedDevice.value = _connectedDevice.value?.copy(batteryLevel = batteryLevel)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _connectionStatus.value = "Bluetooth is disabled or unavailable"
            return false
        }

        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner ?: return false
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            _discoveredDevices.value = emptyList()
            _isScanning.value = true
            _connectionStatus.value = "Scanning for smartwatches..."
            scanner.startScan(null, settings, scanCallback)

            // Auto-stop scan after 12 seconds
            mainHandler.postDelayed({
                if (_isScanning.value) {
                    stopScan()
                }
            }, 12000)
            return true
        } catch (e: SecurityException) {
            _connectionStatus.value = "Permission required: Bluetooth Scan"
            _isScanning.value = false
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (_: SecurityException) {
        }
        _isScanning.value = false
        if (_connectionStatus.value.contains("Scanning")) {
            _connectionStatus.value = "Scan completed (${_discoveredDevices.value.size} devices found)"
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(deviceAddress: String): Boolean {
        stopScan()
        disconnect()

        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return false
        _connectionStatus.value = "Connecting to ${deviceAddress}..."

        try {
            activeGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
            return true
        } catch (e: SecurityException) {
            _connectionStatus.value = "Bluetooth Connect permission denied"
            return false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: SecurityException) {
        }
        activeGatt = null
        _connectedDevice.value = null
        _connectionStatus.value = "Disconnected"
    }

    private fun analyzeServices(gatt: BluetoothGatt) {
        val deviceName = try { gatt.device.name ?: "Unknown Watch" } catch (_: SecurityException) { "Unknown Watch" }
        val deviceAddress = gatt.device.address ?: ""
        val gattServices = gatt.services

        val serviceInfoList = mutableListOf<GattServiceInfo>()
        var hasBattery = false
        var hasDeviceInfo = false
        var hasCurrentTime = false
        var hasXiaomiAuth = false

        for (service in gattServices) {
            val sUuid = service.uuid.toString().lowercase()
            val charInfoList = mutableListOf<GattCharacteristicInfo>()

            if (service.uuid == BATTERY_SERVICE_UUID) hasBattery = true
            if (service.uuid == DEVICE_INFO_SERVICE_UUID) hasDeviceInfo = true
            if (service.uuid == CURRENT_TIME_SERVICE_UUID) hasCurrentTime = true
            if (sUuid.contains("fee0") || sUuid.contains("fee1")) hasXiaomiAuth = true

            for (ch in service.characteristics) {
                val props = mutableListOf<String>()
                val p = ch.properties
                if ((p and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("READ")
                if ((p and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("WRITE")
                if ((p and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("NOTIFY")
                if ((p and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("INDICATE")

                charInfoList.add(
                    GattCharacteristicInfo(
                        uuid = ch.uuid.toString(),
                        name = resolveCharacteristicName(ch.uuid),
                        properties = props,
                        isReadable = props.contains("READ"),
                        isWritable = props.contains("WRITE"),
                        isNotifiable = props.contains("NOTIFY")
                    )
                )
            }

            serviceInfoList.add(
                GattServiceInfo(
                    uuid = service.uuid.toString(),
                    name = resolveServiceName(service.uuid),
                    isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
                    characteristics = charInfoList
                )
            )
        }

        val isRedmiOrXiaomi = hasXiaomiAuth || deviceName.contains("Redmi", ignoreCase = true) ||
                deviceName.contains("Xiaomi", ignoreCase = true) ||
                deviceName.contains("Move", ignoreCase = true)

        val ecosystem = if (isRedmiOrXiaomi) {
            WatchEcosystemType.REDMI_XIAOMI_MI_FITNESS
        } else {
            WatchEcosystemType.STANDARD_BLE_PERIPHERAL
        }

        buildDiagnosticReport(deviceName, deviceAddress, ecosystem, serviceInfoList, hasBattery, hasDeviceInfo, hasCurrentTime, hasXiaomiAuth)
    }

    private fun buildDiagnosticReport(
        deviceName: String,
        deviceAddress: String,
        ecosystem: WatchEcosystemType,
        services: List<GattServiceInfo>,
        hasBattery: Boolean,
        hasDeviceInfo: Boolean,
        hasCurrentTime: Boolean,
        hasXiaomiAuth: Boolean
    ) {
        val supportedList = mutableListOf<String>()
        val restrictedList = mutableListOf<String>()
        val recommendedList = mutableListOf<String>()
        val summary: String

        if (ecosystem == WatchEcosystemType.REDMI_XIAOMI_MI_FITNESS) {
            summary = "Target hardware identified: Redmi Watch Move AMOLED (Mi Fitness RTOS). The watch operates a closed, proprietary RTOS paired exclusively to the Mi Fitness app using encrypted BLE tokens. Arbitrary third-party watch apps and direct raw video streaming are unsupported by the watch firmware. Media controls and notifications operate through the Android system bridge."

            supportedList.add("Android Media Playback sync (Play/Pause, Skip, Volume)")
            supportedList.add("Real-time Watch Mirror HUD Simulator on phone display")
            supportedList.add("Smartwatch Notification forwarding via Android system & Mi Fitness")
            supportedList.add("Selected App launch shortcuts and remote trigger")
            if (hasBattery) supportedList.add("Bluetooth Battery Service telemetry")

            restrictedList.add("Direct screen video frame injection into Redmi Watch Move display (Watch RTOS firmware lacks video framebuffer receiver characteristic)")
            restrictedList.add("Wear OS / standalone Android APK installation (Watch runs RTOS, not Wear OS)")
            restrictedList.add("Unencrypted direct BLE GATT write commands (Mi Fitness handshake encryption is active)")

            recommendedList.add("Use WristMirror's Phone HUD Companion mode for live screen mirroring and app preview.")
            recommendedList.add("Keep Mi Fitness running with Notification Access enabled to receive WristMirror quick actions and alerts.")
            recommendedList.add("Use standard Android MediaSession playback controls which Redmi Watch Move natively picks up.")
        } else {
            summary = "Standard BLE Smartwatch/Band discovered. Standard GATT profiles detected."
            if (hasBattery) supportedList.add("Standard Battery Level (UUID 0x180F)")
            if (hasDeviceInfo) supportedList.add("Device Information Profile (UUID 0x180A)")
            if (hasCurrentTime) supportedList.add("Time Synchronization (UUID 0x1805)")
            supportedList.add("Phone-side Watch Mirror HUD Simulator")
            supportedList.add("Media Remote control via AudioManager")

            restrictedList.add("Direct high-speed video streaming (standard BLE bandwidth limited to ~20-50 KB/s without custom display receiver)")
            restrictedList.add("Custom watchface app without Wear OS platform")

            recommendedList.add("Ensure watch companion app has Notification Listener permission enabled.")
            recommendedList.add("Test BLE characteristic read/write via Diagnostics panel.")
        }

        val report = GattDiagnosticReport(
            deviceName = deviceName,
            deviceAddress = deviceAddress,
            ecosystem = ecosystem,
            services = services,
            hasBatteryService = hasBattery,
            hasDeviceInfoService = hasDeviceInfo,
            hasCurrentTimeService = hasCurrentTime,
            hasXiaomiAuthProtocol = hasXiaomiAuth,
            supportsWearOsApk = false,
            supportsRawBleVideoStream = false,
            supportsCustomDirectCommands = false,
            supportsMediaNotifications = true,
            summary = summary,
            supportedFeatures = supportedList,
            restrictedFeatures = restrictedList,
            recommendedActions = recommendedList
        )

        _diagnosticReport.value = report
        _connectionStatus.value = "Connected • Diagnostic Report Ready"
    }

    /**
     * Diagnostic simulator that specifically models the Redmi Watch Move AMOLED
     * when real hardware is not nearby, allowing complete testing of the compatibility analysis.
     */
    fun loadRedmiWatchMoveDiagnosticSimulation() {
        val simulatedServices = listOf(
            GattServiceInfo(
                uuid = "00001800-0000-1000-8000-00805f9b34fb",
                name = "Generic Access",
                isPrimary = true,
                characteristics = listOf(
                    GattCharacteristicInfo("00002a00-0000-1000-8000-00805f9b34fb", "Device Name (Redmi Watch Move)", listOf("READ"), true, false, false),
                    GattCharacteristicInfo("00002a01-0000-1000-8000-00805f9b34fb", "Appearance", listOf("READ"), true, false, false)
                )
            ),
            GattServiceInfo(
                uuid = "00001801-0000-1000-8000-00805f9b34fb",
                name = "Generic Attribute",
                isPrimary = true,
                characteristics = listOf(
                    GattCharacteristicInfo("00002a05-0000-1000-8000-00805f9b34fb", "Service Changed", listOf("INDICATE"), false, false, false)
                )
            ),
            GattServiceInfo(
                uuid = "0000180f-0000-1000-8000-00805f9b34fb",
                name = "Battery Service",
                isPrimary = true,
                characteristics = listOf(
                    GattCharacteristicInfo("00002a19-0000-1000-8000-00805f9b34fb", "Battery Level (84%)", listOf("READ", "NOTIFY"), true, false, true)
                )
            ),
            GattServiceInfo(
                uuid = "0000180a-0000-1000-8000-00805f9b34fb",
                name = "Device Information",
                isPrimary = true,
                characteristics = listOf(
                    GattCharacteristicInfo("00002a24-0000-1000-8000-00805f9b34fb", "Model Number (M2320W1)", listOf("READ"), true, false, false),
                    GattCharacteristicInfo("00002a26-0000-1000-8000-00805f9b34fb", "Firmware Revision (1.4.128)", listOf("READ"), true, false, false),
                    GattCharacteristicInfo("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name (Xiaomi Inc.)", listOf("READ"), true, false, false)
                )
            ),
            GattServiceInfo(
                uuid = "0000fee0-0000-1000-8000-00805f9b34fb",
                name = "Xiaomi / Mi Fitness Proprietary Auth",
                isPrimary = true,
                characteristics = listOf(
                    GattCharacteristicInfo("00000009-0000-3512-2118-0009af100700", "Mi Auth Handshake [ECDH Encrypted]", listOf("READ", "WRITE", "NOTIFY"), true, true, true),
                    GattCharacteristicInfo("00000010-0000-3512-2118-0009af100700", "Mi Fitness Data Channel", listOf("WRITE", "NOTIFY"), false, true, true)
                )
            )
        )

        _connectedDevice.value = WatchDevice(
            name = "Redmi Watch Move AMOLED",
            address = "78:02:B7:44:91:2C",
            rssi = -54,
            isBonded = true,
            isConnected = true,
            batteryLevel = 84
        )

        buildDiagnosticReport(
            deviceName = "Redmi Watch Move AMOLED",
            deviceAddress = "78:02:B7:44:91:2C",
            ecosystem = WatchEcosystemType.REDMI_XIAOMI_MI_FITNESS,
            services = simulatedServices,
            hasBattery = true,
            hasDeviceInfo = true,
            hasCurrentTime = true,
            hasXiaomiAuth = true
        )
    }

    private fun resolveServiceName(uuid: UUID): String {
        return when (uuid) {
            BATTERY_SERVICE_UUID -> "Battery Service (0x180F)"
            DEVICE_INFO_SERVICE_UUID -> "Device Information (0x180A)"
            CURRENT_TIME_SERVICE_UUID -> "Current Time Service (0x1805)"
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb") -> "Generic Access (0x1800)"
            UUID.fromString("00001801-0000-1000-8000-00805f9b34fb") -> "Generic Attribute (0x1801)"
            else -> {
                val s = uuid.toString().lowercase()
                if (s.contains("fee0") || s.contains("fee1")) {
                    "Xiaomi / Mi Fitness Proprietary Service"
                } else {
                    "GATT Service ($uuid)"
                }
            }
        }
    }

    private fun resolveCharacteristicName(uuid: UUID): String {
        return when (uuid.toString().lowercase()) {
            "00002a19-0000-1000-8000-00805f9b34fb" -> "Battery Level"
            "00002a00-0000-1000-8000-00805f9b34fb" -> "Device Name"
            "00002a01-0000-1000-8000-00805f9b34fb" -> "Appearance"
            "00002a29-0000-1000-8000-00805f9b34fb" -> "Manufacturer Name"
            "00002a24-0000-1000-8000-00805f9b34fb" -> "Model Number"
            "00002a26-0000-1000-8000-00805f9b34fb" -> "Firmware Revision"
            else -> uuid.toString().substring(0, 8)
        }
    }
}
