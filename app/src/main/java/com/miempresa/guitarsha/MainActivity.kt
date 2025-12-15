package com.miempresa.guitarsha

import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.app.Activity
import java.io.OutputStream
import java.util.UUID
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import java.io.IOException
import me.tankery.lib.circularseekbar.CircularSeekBar
import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnEnd

class MainActivity : Activity() {

    private lateinit var seekVolume: CircularSeekBar
    private lateinit var seekDrive: CircularSeekBar
    private lateinit var seekTone: CircularSeekBar

    private lateinit var tvVolumeValue: TextView
    private lateinit var tvDriveValue: TextView
    private lateinit var tvToneValue: TextView

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

    private var enviarDuranteAnimacion = true
    private var animVol: ValueAnimator? = null
    private var animDrv: ValueAnimator? = null
    private var animTon: ValueAnimator? = null

    private val SPP_UUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Presets (SharedPreferences)
    private val presetKeys = arrayOf("P1", "P2", "P3", "P4", "P5")
    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("presets", Context.MODE_PRIVATE)

        // Bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!btAdapter.isEnabled) startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

        // UI base
        tvBtStatus = findViewById(R.id.tvBtStatus)
        btnConectar = findViewById(R.id.btnConectar)
        btnConectar.setOnClickListener { conectarBluetooth("guitarsha") }

        // Knobs
        seekVolume = findViewById(R.id.seekVolume)
        seekDrive  = findViewById(R.id.seekDrive)
        seekTone   = findViewById(R.id.seekTone)

        tvVolumeValue = findViewById(R.id.tvVolumeValue)
        tvDriveValue  = findViewById(R.id.tvDriveValue)
        tvToneValue   = findViewById(R.id.tvToneValue)

        initKnob(seekVolume, tvVolumeValue)
        initKnob(seekDrive,  tvDriveValue)
        initKnob(seekTone,   tvToneValue)

        btnVolPlus  = findViewById(R.id.btnVolPlus)
        btnVolMinus = findViewById(R.id.btnVolMinus)
        btnDrvPlus  = findViewById(R.id.btnDrvPlus)
        btnDrvMinus = findViewById(R.id.btnDrvMinus)
        btnTonPlus  = findViewById(R.id.btnTonPlus)
        btnTonMinus = findViewById(R.id.btnTonMinus)

        configurarControl(seekVolume, tvVolumeValue, "VOL")
        configurarControl(seekDrive,  tvDriveValue,  "DRV")
        configurarControl(seekTone,   tvToneValue,   "TON")

        btnVolPlus.setOnClickListener  { subir(seekVolume) }
        btnVolMinus.setOnClickListener { bajar(seekVolume) }
        btnDrvPlus.setOnClickListener  { subir(seekDrive) }
        btnDrvMinus.setOnClickListener { bajar(seekDrive) }
        btnTonPlus.setOnClickListener  { subir(seekTone) }
        btnTonMinus.setOnClickListener { bajar(seekTone) }

        // Presets: safe find + listener
        for (i in 1..5) {
            val resId = resources.getIdentifier("btnP$i", "id", packageName)
            val btn = findViewById<Button?>(resId)
            btn?.let { button ->
                button.setOnClickListener { cargarPreset(i) }
                button.setOnLongClickListener {
                    guardarPreset(i)
                    true
                }
            }
        }
    }

    private fun initKnob(seek: CircularSeekBar, label: TextView) {
        seek.max = 100f
        seek.progress = 50f
        label.text = "50"
    }

    private fun subir(seek: CircularSeekBar) {
        seek.progress = (seek.progress + 1).coerceAtMost(100f)
    }

    private fun bajar(seek: CircularSeekBar) {
        seek.progress = (seek.progress - 1).coerceAtLeast(0f)
    }

    private fun configurarControl(seek: CircularSeekBar, label: TextView, tag: String) {
        seek.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {

            override fun onProgressChanged(circularSeekBar: CircularSeekBar?, progress: Float, fromUser: Boolean) {
                val value = progress.toInt()
                label.text = value.toString()
                // Solo enviar si NO estamos animando un preset
                if (enviarDuranteAnimacion) {
                    enviarValor(tag, value)
                }
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

        try { btOutput!!.write(frame) }
        catch (e: IOException) {
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

    // 🔵 Presets
    private fun guardarPreset(n: Int) {
        val json = "${seekVolume.progress.toInt()},${seekDrive.progress.toInt()},${seekTone.progress.toInt()}"
        prefs.edit().putString(presetKeys[n-1], json).apply()
        Toast.makeText(this, "Preset $n guardado", Toast.LENGTH_SHORT).show()
    }

    // 🔵 Presets con animación y envío eficiente
    private fun cargarPreset(n: Int) {
        val json = prefs.getString(presetKeys[n - 1], null) ?: return
        val parts = json.split(",")
        if (parts.size != 3) return

        val targetVol = parts[0].toIntOrNull() ?: return
        val targetDrv = parts[1].toIntOrNull() ?: return
        val targetTon = parts[2].toIntOrNull() ?: return

        // Cancelar animaciones anteriores
        animVol?.cancel()
        animDrv?.cancel()
        animTon?.cancel()

        val startVol = seekVolume.progress
        val startDrv = seekDrive.progress
        val startTon = seekTone.progress

        // Desactivar envío mientras dura la animación
        enviarDuranteAnimacion = false

        // Animación Volume
        animVol = ValueAnimator.ofFloat(startVol, targetVol.toFloat()).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                seekVolume.progress = valueAnimator.animatedValue as Float
                tvVolumeValue.text = seekVolume.progress.toInt().toString()
            }
            doOnEnd { enviarValor("VOL", targetVol) }
            start()
        }

        // Animación Drive
        animDrv = ValueAnimator.ofFloat(startDrv, targetDrv.toFloat()).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                seekDrive.progress = valueAnimator.animatedValue as Float
                tvDriveValue.text = seekDrive.progress.toInt().toString()
            }
            doOnEnd { enviarValor("DRV", targetDrv) }
            start()
        }

        // Animación Tone
        animTon = ValueAnimator.ofFloat(startTon, targetTon.toFloat()).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                seekTone.progress = valueAnimator.animatedValue as Float
                tvToneValue.text = seekTone.progress.toInt().toString()
            }
            doOnEnd {
                enviarValor("TON", targetTon)
                enviarDuranteAnimacion = true  // reactivar envíos normales
            }
            start()
        }
    }

    // 🔵 Bluetooth
    private fun conectarBluetooth(nombre: String) {
        if (!tienePermisoBluetooth()) { pedirPermisoBluetooth(); return }

        val device = btAdapter.bondedDevices.firstOrNull { it.name == nombre } ?: return

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
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun pedirPermisoBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
        }
    }
}
