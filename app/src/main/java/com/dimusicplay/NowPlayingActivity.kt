package com.dimusicplay

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class NowPlayingActivity : AppCompatActivity() {

    private lateinit var albumArtImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        albumArtImageView = findViewById(R.id.albumArtImageViewLarge)
        titleTextView = findViewById(R.id.nowPlayingTitleLarge)
        artistTextView = findViewById(R.id.nowPlayingArtistLarge)
        seekBar = findViewById(R.id.seekBarLarge)
        playPauseButton = findViewById(R.id.playPauseButtonLarge)
        nextButton = findViewById(R.id.nextButtonLarge)
        previousButton = findViewById(R.id.previousButtonLarge)

        setupControls()

        // Configuramos un listener para actualizar la UI si la canción cambia
        MusicPlayer.onSongChanged = {
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que la pantalla se muestra, actualizamos la UI
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        // Detenemos el actualizador del seekbar cuando la pantalla no está visible
        runnable?.let { handler.removeCallbacks(it) }
    }

    private fun updateUI() {
        MusicPlayer.currentSong?.let { song ->
            titleTextView.text = song.title
            artistTextView.text = song.artist
            Glide.with(this)
                .load(song.albumArtUri)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(albumArtImageView)
            
            initializeSeekBar()
        }
    }

    private fun setupControls() {
        playPauseButton.setOnClickListener {
            MusicPlayer.pauseOrResume()
        }
        nextButton.setOnClickListener {
            MusicPlayer.playNextSong()
        }
        previousButton.setOnClickListener {
            MusicPlayer.playPreviousSong()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) MusicPlayer.mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }

    private fun initializeSeekBar() {
        seekBar.max = MusicPlayer.mediaPlayer?.duration ?: 0
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
}
