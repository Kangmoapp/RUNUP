package com.example.runup.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
@SuppressLint("MissingPermission")
@Singleton
class BleConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bleSensorManager: BleSensorManager // 🌟 데이터 파이프라인 주입
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    // UI에 보여줄 연결 상태 (좌/우 분리)
    private val _leftConnectionState = MutableStateFlow("🔍 대기 중...")
    val leftConnectionState: StateFlow<String> = _leftConnectionState.asStateFlow()

    private val _rightConnectionState = MutableStateFlow("🔍 대기 중...")
    val rightConnectionState: StateFlow<String> = _rightConnectionState.asStateFlow()

    // 블루투스 UUID 설정
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914c")
    private val CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a9")
    private val DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    // 타겟 디바이스 이름
    private val TARGET_LEFT_NAME = "RunUp_L"
    private val TARGET_RIGHT_NAME = "RunUp_R"

    private var dynamicLeftMac: String? = null
    private var dynamicRightMac: String? = null
    private var leftGatt: BluetoothGatt? = null
    private var rightGatt: BluetoothGatt? = null

    // 🌟 블루투스 스캔 시작 (MainActivity에서 권한 허용 후 호출됨)
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BleManager", "블루투스가 꺼져있거나 지원하지 않는 기기입니다.")
            return
        }

        // 🌟 1. 앱 튕김 방지 쉴드: 스캔 권한이 실제로 있는지 검사!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BleManager", "🚨 블루투스 스캔 권한이 아직 허용되지 않았습니다! 스캔을 취소합니다.")
                return // 권한이 없으면 튕기기 전에 조용히 함수 종료
            }
        } else {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BleManager", "🚨 위치 권한이 아직 허용되지 않았습니다! 스캔을 취소합니다.")
                return
            }
        }

        // 🌟 2. 권한이 확인되었을 때만 안전하게 스캔 시작
        if (dynamicLeftMac == null) _leftConnectionState.value = "🔍 '$TARGET_LEFT_NAME' 찾는 중..."
        if (dynamicRightMac == null) _rightConnectionState.value = "🔍 '$TARGET_RIGHT_NAME' 찾는 중..."

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .build()

        bluetoothLeScanner?.startScan(null, settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            val deviceName = device.name ?: result.scanRecord?.deviceName ?: "Unknown"

            if (deviceName == TARGET_LEFT_NAME && dynamicLeftMac == null) {
                bluetoothLeScanner?.stopScan(this)
                dynamicLeftMac = device.address
                _leftConnectionState.value = "🔄 연결 시도 중..."
                connectDevice(device)
            } else if (deviceName == TARGET_RIGHT_NAME && dynamicRightMac == null) {
                bluetoothLeScanner?.stopScan(this)
                dynamicRightMac = device.address
                _rightConnectionState.value = "🔄 연결 시도 중..."
                connectDevice(device)
            }
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        }, 500)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val isLeft = gatt.device.address == dynamicLeftMac
            if (isLeft) leftGatt = gatt else rightGatt = gatt

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (isLeft) _leftConnectionState.value = "🟢 연결됨" else _rightConnectionState.value = "🟢 연결됨"

                Handler(Looper.getMainLooper()).postDelayed({
                    try { gatt.requestMtu(100) } catch (e: Exception) { e.printStackTrace() }
                    Handler(Looper.getMainLooper()).postDelayed({ gatt.discoverServices() }, 1000)
                }, 500)

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                if (isLeft) {
                    _leftConnectionState.value = "🔴 끊어짐 (재연결 대기)"
                    dynamicLeftMac = null
                } else {
                    _rightConnectionState.value = "🔴 끊어짐 (재연결 대기)"
                    dynamicRightMac = null
                }
                // 연결이 끊기면 3초 후 다시 스캔 시작
                Handler(Looper.getMainLooper()).postDelayed({ startScan() }, 3000)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val service = gatt.getService(SERVICE_UUID) ?: return
            val char = service.getCharacteristic(CHAR_UUID) ?: return

            gatt.setCharacteristicNotification(char, true)

            Handler(Looper.getMainLooper()).postDelayed({
                val desc = char.getDescriptor(DESCRIPTOR_UUID)
                if (desc != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(desc, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        gatt.writeDescriptor(desc)
                    }
                }
            }, 500)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val isLeft = gatt.device.address == dynamicLeftMac
                if (isLeft) _leftConnectionState.value = "📡 수신 중!" else _rightConnectionState.value = "📡 수신 중!"

                // 한쪽 기기가 연결 완료되었는데 다른 한쪽이 아직 연결 안 되었다면 다시 스캔 시작
                if (dynamicLeftMac == null || dynamicRightMac == null) {
                    Handler(Looper.getMainLooper()).postDelayed({ startScan() }, 1000)
                }
            }
        }

        // ========================================================
        // 🌟 핵심: 여기서 센서 데이터가 수신되면 버퍼 매니저로 넘깁니다
        // ========================================================
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val rawData = String(characteristic.value ?: return, Charsets.UTF_8).trim()
            val isLeft = gatt.device.address == dynamicLeftMac
            val values = rawData.split(",")

            if (values.size >= 10) {
                val floatArray = FloatArray(10)
                for (i in 0 until 10) {
                    floatArray[i] = values[i].trim().toFloatOrNull() ?: 0f
                }

                // 🌟 AI가 데이터를 모을 수 있도록 파이프라인(BleSensorManager)에 쏴주기
                if (isLeft) {
                    bleSensorManager.updateLeftData(floatArray)
                } else {
                    bleSensorManager.updateRightData(floatArray)
                }
            }
        }
    }

    // 앱 종료 시 모든 연결 해제
    fun disconnectAll() {
        try {
            leftGatt?.disconnect()
            leftGatt?.close()
            leftGatt = null

            rightGatt?.disconnect()
            rightGatt?.close()
            rightGatt = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}