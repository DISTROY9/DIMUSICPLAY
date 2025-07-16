package com.dimusicplay

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var songs: List<Song>

    // Variables para los controles de UI
    private lateinit var controlsContainer: CardView
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingArtist: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    
    // Handler para actualizar la UI (el SeekBar) cada segundo
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializamos las vistas de los controles
        controlsContainer = findViewById(R.id.controlsContainer)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist)
        playPauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seekBar)

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
        songs = scanForMusic()
        val recyclerView: RecyclerView = findViewById(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = SongAdapter(songs) { song ->
            playSong(song)
        }
        recyclerView.adapter = adapter
    }

    private fun playSong(song: Song) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        
        // Hacemos visible el panel de controles
        controlsContainer.visibility = View.VISIBLE
        nowPlayingTitle.text = song.title
        nowPlayingArtist.text = song.artist
        playPauseButton.setImageResource(R.drawable.ic_pause) // Cambiamos el icono a "pausa"

        mediaPlayer = MediaPlayer().apply {
            setDataSource(song.path)
            prepare()
            start()
        }
        
        setupControls()
        initializeSeekBar()
    }
    
    private fun setupControls() {
        playPauseButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                mediaPlayer?.start()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initializeSeekBar() {
        seekBar.max = mediaPlayer?.duration ?: 0
        
        runnable = Runnable {
            seekBar.progress = mediaPlayer?.currentPosition ?: 0
            handler.postDelayed(runnable, 1000) // Se actualiza cada segundo
        }
        handler.postDelayed(runnable, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacks(runnable) // Detenemos el actualizador del seekbar
    }

    private fun scanForMusic(): List<Song> {
        // El código de esta función no cambia, es el mismo que ya tienes
        val songList = mutableListOf<Song>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                songList.add(Song(cursor.getLong(idColumn), cursor.getString(titleColumn), cursor.getString(artistColumn), cursor.getLong(durationColumn), cursor.getString(pathColumn)))
            }
        }
        return songList
    }
}
