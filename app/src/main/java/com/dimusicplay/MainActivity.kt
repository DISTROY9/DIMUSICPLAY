package com.dimusicplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dimusicplay.databinding.ActivityMainBinding // Importa la clase de binding

class MainActivity : AppCompatActivity() {

    // Variable para el View Binding
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Infla el layout usando View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ocultar la barra de acción por defecto si tu tema la muestra
        supportActionBar?.hide()

        // --- Configuración inicial de la UI ---
        // Aquí podrías configurar listeners para los botones, etc.
        // Por ahora, solo tenemos los elementos visuales.

        // Ejemplo: Click listener para el mini-reproductor (simular expandir)
        binding.miniPlayerBar.setOnClickListener {
            // En una fase posterior, aquí abriremos el reproductor a pantalla completa
            // Toast.makeText(this, "Mini-Reproductor Clicked!", Toast.LENGTH_SHORT).show()
        }

        // Configuración de la Bottom Navigation View (por ahora sin funcionalidad real)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Cargar fragmento de inicio
                    // Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_library -> {
                    // Cargar fragmento de biblioteca
                    // Toast.makeText(this, "Library selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_search -> {
                    // Cargar fragmento de búsqueda
                    // Toast.makeText(this, "Search selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_settings -> {
                    // Cargar fragmento de ajustes
                    // Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }
}
