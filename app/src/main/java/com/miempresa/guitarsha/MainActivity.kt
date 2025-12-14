package com.miempresa.guitarsha

import android.bluetooth.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.app.Activity
import java.io.OutputStream
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.IOException
import me.tankery.lib.circularseekbar.CircularSeekBar

class MainActivity : Activity() {

    private lateinit var seekVolume: CircularSeekBar
    private lateinit var seekDrive: CircularSeekBar
    private lateinit var seekTone: CircularSeekBar

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
    private var btConectado = false

    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!btAdapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }

        tvBtStatus = findViewById(R.id.tvBtStatus)
        btnConectar = findViewById(R.id.btnConectar)

        btnConectar.setOnClickListener {
            conectarBluetooth("guitarsha")
        }

        seekVolume = findViewById(R.id.seekVolume)
        seekDrive = findViewById(R.id.seekDrive)
        seekTone = findViewById(R.id.seekTone)

        // Configuración por código (CLAVE)
        seekVolume.max = 100f
        seekVolume.progress = 50f

        seekDrive.max = 100f
        seekDrive.progress = 50f

        seekTone.max = 100f
        seekTone.progress = 50f

        btnVolPlus = findViewById(R.id.btnVolPlus)
        btnVolMinus = findViewById(R.id.btnVolMinus)
        btnDrvPlus = findViewById(R.id.btnDrvPlus)
        btnDrvMinus = findViewById(R.id.btnDrvMinus)
        btnTonPlus = findViewById(R.id.btnTonPlus)
        btnTonMinus = findViewById(R.id.btnTonMinus)

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

    private fun subir(seek: CircularSeekBar) {
        seek.progress = (seek.progress + 1).coerceAtMost(100f)
    }

    private fun bajar(seek: CircularSeekBar) {
        seek.progress = (seek.progress - 1).coerceAtLeast(0f)
    }

    private fun configurarControl(seek: CircularSeekBar, tag: String) {
        seek.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {

            override fun onProgressChanged(
                circularSeekBar: CircularSeekBar?,
                progress: Float,
                fromUser: Boolean
            ) {
                enviarValor(tag, progress.toInt())
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar?) {}
            override fun onStopTrackingTouch(seekBar: CircularSeekBar?) {}
        })
    }

    private fun enviarValor(param: String, value: Int) {
        if (!btConectado || btOutput == null) return

        val paramChar = when (param) {
            "VOL" -> 'V'
            "DRV" -> 'D'
            "TON" -> 'T'
            else -> return
        }

        val payload = ">$paramChar,$value,".toByteArray(Charsets.US_ASCII)
        val chk = calcularXor(payload)

        val frame = payload + chk + '<'.code.toByte()

        try {
            btOutput!!.write(frame)
        } catch (e: IOException) {
            btConectado = false
            runOnUiThread {
                tvBtStatus.text = "Desconectado"
                tvBtStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun calcularXor(data: ByteArray): Byte {
        var xor = 0
        for (b in data) xor = xor xor (b.toInt() and 0xFF)
        return xor.toByte()
    }

    private fun conectarBluetooth(nombre: String) {
        if (!tienePermisoBluetooth()) {
            pedirPermisoBluetooth()
            return
        }

        val device = btAdapter.bondedDevices.firstOrNull { it.name == nombre }
            ?: return

        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btAdapter.cancelDiscovery()
            btSocket?.connect()
            btOutput = btSocket?.outputStream
            btConectado = true

            runOnUiThread {
                tvBtStatus.text = "Conectado"
                tvBtStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            }

        } catch (_: Exception) {
            btConectado = false
        }
    }

    private fun tienePermisoBluetooth(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

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
