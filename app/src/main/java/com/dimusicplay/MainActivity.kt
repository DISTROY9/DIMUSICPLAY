package com.dimusicplay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

// --- Molde para nuestras canciones ---
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String
)

class MainActivity : AppCompatActivity() {

    // Constante para identificar nuestra solicitud de permiso
    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Al iniciar la app, verificamos si tenemos permiso para leer el audio
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        // Verificamos si el permiso ya está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Si no está concedido, lo solicitamos
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            // Si el permiso ya fue concedido, procedemos a cargar las canciones
            Toast.makeText(this, "Permiso concedido. Cargando música...", Toast.LENGTH_SHORT).show()
            loadSongs()
        }
    }

    // Esta función se llama después de que el usuario responde al diálogo de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // El usuario concedió el permiso
                Toast.makeText(this, "¡Permiso concedido! Cargando música...", Toast.LENGTH_SHORT).show()
                loadSongs()
            } else {
                // El usuario denegó el permiso
                Toast.makeText(this, "Permiso denegado. La app no puede funcionar sin acceso a los archivos de audio.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Función que escanea la música y configura el RecyclerView
    private fun loadSongs() {
        val songs = scanForMusic()
        Log.d("DIMUSICPLAY_DEBUG", "Canciones encontradas: ${songs.size}")

        // 1. Encontramos el RecyclerView en nuestro layout por su ID
        val recyclerView: RecyclerView = findViewById(R.id.songsRecyclerView)

        // 2. Le decimos cómo mostrar los elementos (en una lista vertical)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 3. Creamos nuestro adaptador con la lista de canciones
        val adapter = SongAdapter(songs)

        // 4. Conectamos el RecyclerView con nuestro adaptador para que muestre los datos
        recyclerView.adapter = adapter
    }

    private fun scanForMusic(): List<Song> {
        val songList = mutableListOf<Song>()
        
        // Las columnas de información que queremos obtener para cada canción
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA // La ruta del archivo
        )
        
        // Le decimos que solo queremos archivos de música
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        // Hacemos la consulta al MediaStore
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC" // Ordenamos las canciones alfabéticamente
        )
        
        cursor?.use { c ->
            // Obtenemos los índices de las columnas
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            // Recorremos todos los resultados
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val title = c.getString(titleColumn)
                val artist = c.getString(artistColumn)
                val duration = c.getLong(durationColumn)
                val path = c.getString(pathColumn)
                
                // Creamos nuestro objeto Song y lo añadimos a la lista
                songList.add(Song(id, title, artist, duration, path))
            }
        }
        
        // Devolvemos la lista de canciones encontradas
        return songList
    }
}
