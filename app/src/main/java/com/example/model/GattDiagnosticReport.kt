package com.example.model

data class GattCharacteristicInfo(
    val uuid: String,
    val name: String,
    val properties: List<String>,
    val isReadable: Boolean,
    val isWritable: Boolean,
    val isNotifiable: Boolean
)

data class GattServiceInfo(
    val uuid: String,
    val name: String,
    val isPrimary: Boolean,
    val characteristics: List<GattCharacteristicInfo>
)

enum class WatchEcosystemType {
    REDMI_XIAOMI_MI_FITNESS, // RTOS based, encrypted protocol
    WEAR_OS_GENERIC,         // Full Android Wear OS
    STANDARD_BLE_PERIPHERAL, // Generic GATT fitness/heart rate band
    UNKNOWN
}

data class GattDiagnosticReport(
    val deviceName: String,
    val deviceAddress: String,
    val ecosystem: WatchEcosystemType,
    val services: List<GattServiceInfo>,
    val hasBatteryService: Boolean,
    val hasDeviceInfoService: Boolean,
    val hasCurrentTimeService: Boolean,
    val hasXiaomiAuthProtocol: Boolean,
    val supportsWearOsApk: Boolean,
    val supportsRawBleVideoStream: Boolean,
    val supportsCustomDirectCommands: Boolean,
    val supportsMediaNotifications: Boolean,
    val summary: String,
    val supportedFeatures: List<String>,
    val restrictedFeatures: List<String>,
    val recommendedActions: List<String>
)
