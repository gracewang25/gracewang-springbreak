package com.example.gracewangspringbreak

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import java.util.Locale

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0
    private val shakeThreshold = 2.7f
    private val shakeTimeout = 500
    private lateinit var selectedLanguage: String
    private lateinit var speechText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val languages = arrayOf("Select Language", "English", "French", "Chinese", "Spanish")
        val spinner: Spinner = findViewById(R.id.languageSpinner)
        spinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languages)

        speechText = findViewById(R.id.editText)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    selectedLanguage = languages[position]
                    promptSpeechInput(selectedLanguage)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun promptSpeechInput(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)

        val locale = when (language) {
            "English" -> "en-US"
            "French" -> "fr-FR"
            "Chinese" -> "zh-CN"
            "Spanish" -> "es-ES"
            else -> Locale.getDefault()
        }

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something in $language...")

        try {
            startActivityForResult(intent, 100)
        } catch (a: Exception) {
            Toast.makeText(
                applicationContext,
                "Your device doesn't support Speech Recognition",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            speechText.setText(spokenText) // set the speech text into EditText
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate the acceleration change
            val accelerationChange = Math.sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH

            // Check for a significant motion; you can adjust the value '1.0' based on testing
            if (accelerationChange > 1.0) {
                openMapForLanguage(selectedLanguage)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun openMapForLanguage(language: String) {
        val locationUri = when (language) {
            "English" -> {
                val locations = arrayOf(
                    "geo:0,0?q=London",
                    "geo:40.7128,-74.0060?q=New York"
                ) // London and New York
                locations.random()
            }

            "French" -> {
                val locations = arrayOf(
                    "geo:0,0?q=Paris",
                    "geo:45.5017,-73.5673?q=Montreal"
                ) // Paris and Montreal
                locations.random()
            }

            "Chinese" -> {
                val locations = arrayOf(
                    "geo:0,0?q=Beijing",
                    "geo:31.2304,121.4737?q=Shanghai"
                ) // Beijing and Shanghai
                locations.random()
            }

            "Spanish" -> {
                val locations = arrayOf(
                    "geo:0,0?q=Madrid",
                    "geo:41.3851,2.1734?q=Barcelona"
                ) // Madrid and Barcelona
                locations.random()
            }

            else -> "geo:0,0"
        }
        val audioResourceId = when (language) {
            "English" -> R.raw.hello_en
            "French" -> R.raw.hello_fr
            "Chinese" -> R.raw.hello_cn
            "Spanish" -> R.raw.hello_es
            else -> null
        }

        audioResourceId?.let {
            val mediaPlayer = MediaPlayer.create(this, it)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.start()
        }

        // map
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(locationUri))
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Google Maps not available", Toast.LENGTH_SHORT).show()
        }
    }
}
