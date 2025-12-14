package com.miempresa.guitarsha

import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.app.Activity
import java.io.OutputStream
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.IOException


class MainActivity : Activity() {

    private lateinit var seekVolume: SeekBar
    private lateinit var seekDrive: SeekBar
    private lateinit var seekTone: SeekBar

    private lateinit var btnVolPlus: Button
    private lateinit var btnVolMinus: Button
    private lateinit var btnDrvPlus: Button
    private lateinit var btnDrvMinus: Button
    private lateinit var btnTonPlus: Button
    private lateinit var btnTonMinus: Button
    private lateinit var btnConectar: Button

    private lateinit var tvBtStatus: TextView

    private lateinit var btAdapter: BluetoothAdapter
    private var btSocket: BluetoothSocket? = null
    private var btOutput: OutputStream? = null
    private var btConectado = false  // <-- Nuevo estado

    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter()


        if (!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }

        // UI
        tvBtStatus = findViewById(R.id.tvBtStatus)
        btnConectar = findViewById(R.id.btnConectar)

        btnConectar.setOnClickListener {
            conectarBluetooth("guitarsha")
        }

        seekVolume = findViewById(R.id.seekVolume)
        seekDrive = findViewById(R.id.seekDrive)
        seekTone = findViewById(R.id.seekTone)

        btnVolPlus = findViewById(R.id.btnVolPlus)
        btnVolMinus = findViewById(R.id.btnVolMinus)
        btnDrvPlus = findViewById(R.id.btnDrvPlus)
        btnDrvMinus = findViewById(R.id.btnDrvMinus)
        btnTonPlus = findViewById(R.id.btnTonPlus)
        btnTonMinus = findViewById(R.id.btnTonMinus)


        // Listeners
        configurarControl(seekVolume, "VOL")
        configurarControl(seekDrive, "DRV")
        configurarControl(seekTone, "TON")

        btnVolPlus.setOnClickListener { subir(seekVolume) }
        btnVolMinus.setOnClickListener { bajar(seekVolume) }

        btnDrvPlus.setOnClickListener { subir(seekDrive) }
        btnDrvMinus.setOnClickListener { bajar(seekDrive) }

        btnTonPlus.setOnClickListener { subir(seekTone) }
        btnTonMinus.setOnClickListener { bajar(seekTone) }
    }

    private fun subir(seek: SeekBar) {
        seek.progress = (seek.progress + 1).coerceAtMost(100)
    }

    private fun bajar(seek: SeekBar) {
        seek.progress = (seek.progress - 1).coerceAtLeast(0)
    }

    private fun configurarControl(seek: SeekBar, tag: String) {
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                enviarValor(tag, value)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun enviarValor(param: String, value: Int) {

        // Verificar si el Bluetooth está realmente conectado antes de enviar
        if (!btConectado || btOutput == null) {
            Log.w("GuitarSHA", "⚠️ BT no conectado, no se envía")
            return
        }

        val paramChar = when (param) {
            "VOL" -> 'V'
            "DRV" -> 'D'
            "TON" -> 'T'
            else -> return
        }

        val payloadStr = ">$paramChar,$value,"
        val payloadBytes = payloadStr.toByteArray(Charsets.US_ASCII)

        val chkByte = calcularXor(payloadBytes)

        val buffer = java.io.ByteArrayOutputStream()
        buffer.write(payloadBytes)
        buffer.write(chkByte.toInt())
        buffer.write('<'.code)

        val frameBytes = buffer.toByteArray()

        val hexDump = frameBytes.joinToString(" ") {
            String.format("%02X", it)
        }

        Log.d("GuitarSHA", hexDump)

        // Intentar enviar por Bluetooth
        try {
            btOutput!!.write(frameBytes)
        } catch (e: IOException) {
            Log.e("GuitarSHA", "❌ Error escribiendo BT", e)
            btConectado = false

            // Cambiar UI si se pierde la conexión
            runOnUiThread {
                tvBtStatus.text = "Desconectado"
                tvBtStatus.setTextColor(
                    getColor(android.R.color.holo_red_dark)
                )
            }
        }
    }

    private fun calcularXor(data: ByteArray): Byte {
        var xor = 0
        for (b in data) {
            xor = xor xor (b.toInt() and 0xFF)
        }
        return xor.toByte()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("GuitarSHA", "✅ Permiso BLUETOOTH_CONNECT concedido")
            conectarBluetooth("guitarsha") // ← MISMO nombre
        } else {
            Log.e("GuitarSHA", "❌ Permiso BLUETOOTH_CONNECT denegado")
        }
    }

    private fun conectarBluetooth(nombre: String) {
        Log.d("GuitarSHA", "Intentando conectar a $nombre")

        if (!tienePermisoBluetooth()) {
            Log.d("GuitarSHA", "🔐 Pidiendo permiso BLUETOOTH_CONNECT")
            pedirPermisoBluetooth()
            return   // ⛔ CLAVE
        }

        val paired = btAdapter.bondedDevices
        Log.d("GuitarSHA", "Dispositivos emparejados: ${paired.map { it.name }}")

        val device = paired.firstOrNull { it.name == nombre }

        if (device == null) {
            Log.e("GuitarSHA", "❌ No se encontró $nombre entre los emparejados")
            return
        }

        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btAdapter.cancelDiscovery()
            btSocket?.connect()
            btOutput = btSocket?.outputStream

            btConectado = true  // <-- Conexión exitosa

            Log.d("GuitarSHA", "✅ Conectado OK")

            runOnUiThread {
                tvBtStatus.text = "Conectado"
                tvBtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }

        } catch (e: Exception) {
            Log.e("GuitarSHA", "❌ Error al conectar", e)
            btConectado = false  // <-- Si falla la conexión
        }
    }

    private fun tienePermisoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                this,  // 👈 CLAVE
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun pedirPermisoBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
        }
    }

}
