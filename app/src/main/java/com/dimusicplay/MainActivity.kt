package com.dimusicplay

import android.Manifest
import android.app.ActivityOptions
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    private lateinit var controlsContainer: CardView
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingArtist: TextView
    private lateinit var playPauseButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var nowPlayingAlbumArt: ImageView

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        controlsContainer = findViewById(R.id.controlsContainer)
        nowPlayingTitle = findViewById(R.id.nowPlayingTitle)
        nowPlayingArtist = findViewById(R.id.nowPlayingArtist)
        playPauseButton = findViewById(R.id.playPauseButton)
        seekBar = findViewById(R.id.seekBar)
        nowPlayingAlbumArt = findViewById(R.id.nowPlayingAlbumArt)
        
        checkAndRequestPermissions()
        
        MusicPlayer.onSongChanged = {
            updateBottomBarUI()
        }
    }

    override fun onResume() {
        super.onResume()
        updateBottomBarUI()
    }
    
    private fun updateBottomBarUI() {
        MusicPlayer.currentSong?.let { song ->
            controlsContainer.visibility = View.VISIBLE
            nowPlayingTitle.text = song.title
            nowPlayingArtist.text = song.artist
            
            Glide.with(this)
                .load(song.albumArtUri)
                .placeholder(R.mipmap.ic_launcher)
                .into(nowPlayingAlbumArt)
                
            initializeSeekBar()
        }
    }

    private fun setupControls() {
        // Al tocar la barra de controles, abrimos la pantalla Now Playing
        // PERO NO con una transición, ya que la carátula de la barra no es la misma
        controlsContainer.setOnClickListener {
            val intent = Intent(this, NowPlayingActivity::class.java)
            startActivity(intent)
        }

        playPauseButton.setOnClickListener {
            MusicPlayer.pauseOrResume()
        }
        
        findViewById<ImageButton>(R.id.nextButton).setOnClickListener { MusicPlayer.playNextSong() }
        findViewById<ImageButton>(R.id.previousButton).setOnClickListener { MusicPlayer.playPreviousSong() }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MusicPlayer.mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }
    
    private fun initializeSeekBar() {
        MusicPlayer.mediaPlayer?.let {
            seekBar.max = it.duration
        }
        runnable?.let { handler.removeCallbacks(it) }
        runnable = Runnable {
            MusicPlayer.mediaPlayer?.let {
                if (it.isPlaying) {
                     playPauseButton.setImageResource(R.drawable.ic_pause)
                } else {
                     playPauseButton.setImageResource(R.drawable.ic_play)
                }
                seekBar.progress = it.currentPosition
            }
            handler.postDelayed(runnable!!, 1000)
        }
        handler.postDelayed(runnable!!, 1000)
    }

    private fun loadSongs() {
        val songs = scanForMusic()
        MusicPlayer.setSongList(songs)
        val recyclerView: RecyclerView = findViewById(R.id.songsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val adapter = SongAdapter(songs) { clickedSong, albumArtView ->
            MusicPlayer.playSong(clickedSong)
            
            val intent = Intent(this, NowPlayingActivity::class.java)
            val options = ActivityOptions.makeSceneTransitionAnimation(
                this,
                albumArtView,
                "album_art_transition"
            )
            startActivity(intent, options.toBundle())
        }
        recyclerView.adapter = adapter
        
        // Llamamos a setupControls aquí, después de que el adapter está listo
        setupControls()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        runnable?.let { handler.removeCallbacks(it) }
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

    private fun scanForMusic(): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, MediaStore.Audio.Media.TITLE + " ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(pathColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                songList.add(Song(id, title, artist, duration, path, albumArtUri))
            }
        }
        return songList
    }
}
