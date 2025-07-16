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
    private var currentSongIndex = -1

    // Variables para los controles de UI
    private lateinit var controlsContainer: CardView
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingArtist: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var seekBar: SeekBar
    
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlsContainer = findViewById(R.id.controlsContainer)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist)
        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        previousButton = findViewById(R.id.previousButton)
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
        val adapter = SongAdapter(songs) { clickedSong ->
            currentSongIndex = songs.indexOf(clickedSong)
            playSong(clickedSong)
        }
        recyclerView.adapter = adapter
    }

    private fun playSong(song: Song) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        
        controlsContainer.visibility = View.VISIBLE
        nowPlayingTitle.text = song.title
        nowPlayingArtist.text = song.artist
        playPauseButton.setImageResource(R.drawable.ic_pause)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.path)
                // Usamos prepareAsync() que es más seguro y no bloquea la app
                setOnPreparedListener { mp ->
                    // La música está lista para sonar, ahora la iniciamos
                    mp.start()
                    initializeSeekBar() // Movemos la inicialización del SeekBar aquí
                }
                prepareAsync() // Prepara la reproducción en segundo plano
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al reproducir la canción.", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

            setOnCompletionListener {
                playNextSong()
            }
        }
        
        setupControls()
    }
    
    private fun playNextSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            playSong(songs[currentSongIndex])
        }
    }

    private fun playPreviousSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
            playSong(songs[currentSongIndex])
        }
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
        
        nextButton.setOnClickListener { playNextSong() }
        previousButton.setOnClickListener { playPreviousSong() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun initializeSeekBar() {
        seekBar.max = mediaPlayer?.duration ?: 0
        runnable?.let { handler.removeCallbacks(it) }
        runnable = Runnable {
            try {
                seekBar.progress = mediaPlayer?.currentPosition ?: 0
                handler.postDelayed(runnable!!, 1000)
            } catch (e: Exception) {
                // Evita crashes si el mediaplayer se libera mientras el runnable está activo
            }
        }
        handler.postDelayed(runnable!!, 1000)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        runnable?.let { handler.removeCallbacks(it) }
    }

    private fun scanForMusic(): List<Song> {
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
