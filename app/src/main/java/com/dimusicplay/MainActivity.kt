package com.dimusicplay // Asegúrate que este paquete coincida con el tuyo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // La forma de definir una constante en Kotlin
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    // Así se sobrescribe el método onCreate en Kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // La lógica es la misma: al crear la app, verificamos los permisos
        checkAndRequestPermissions()
    }

    // Así se define una función en Kotlin
    private fun checkAndRequestPermissions() {
        // Verificamos si el permiso ya está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Si no está concedido, lo solicitamos. La sintaxis del array cambia.
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            // Si el permiso ya fue concedido, continuamos
            Toast.makeText(this, "Permiso ya concedido. ¡Listo para buscar música!", Toast.LENGTH_SHORT).show()
            // Aquí más adelante llamaremos al método para escanear la música.
        }
    }

    // Así se sobrescribe el método que maneja el resultado del permiso en Kotlin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El usuario concedió el permiso
                Toast.makeText(this, "¡Permiso concedido! Gracias.", Toast.LENGTH_SHORT).show()
                // Ahora que tenemos el permiso, podemos empezar a buscar la música.
            } else {
                // El usuario denegó el permiso
                Toast.makeText(this, "Permiso denegado. La app no puede funcionar sin acceso a los archivos de audio.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
