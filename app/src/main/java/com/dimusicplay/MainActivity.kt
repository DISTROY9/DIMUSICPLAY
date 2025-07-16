package com.dimusicplay

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String
)

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    // Variable para guardar nuestra instancia de MediaPlayer
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            loadSongs()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongs()
            } else {
                Toast.makeText(this, "Permiso denegado. La app no puede funcionar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSongs() {
        val songs = scanForMusic()
        val recyclerView: RecyclerView = findViewById(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Al crear el adaptador, le pasamos la función que se ejecutará al hacer clic
        val adapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        recyclerView.adapter = adapter
    }

    private fun playSong(song: Song) {
        // Detenemos y liberamos cualquier reproducción anterior
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        Toast.makeText(this, "Reproduciendo: ${song.title}", Toast.LENGTH_SHORT).show()

        // Creamos una nueva instancia y la configuramos
        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare() // Prepara el archivo para la reproducción
            start()   // ¡Inicia la reproducción!
        }
    }

    // Es muy importante liberar los recursos del MediaPlayer cuando la app se destruye
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun scanForMusic(): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )
        cursor?.use { c ->
            val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val id = c.getLong(idColumn)
                val title = c.getString(titleColumn)
                val artist = c.getString(artistColumn)
                val duration = c.getLong(durationColumn)
                val path = c.getString(pathColumn)
                songList.add(Song(id, title, artist, duration, path))
            }
        }
        return songList
    }
}
