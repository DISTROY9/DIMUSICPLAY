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
import com.gauravk.audiovisualizer.visualizer.BlastVisualizer

class NowPlayingActivity : AppCompatActivity() {

    private lateinit var albumArtImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var blastVisualizer: BlastVisualizer

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
        blastVisualizer = findViewById(R.id.blastVisualizer)

        setupControls()

        // Configuramos un listener para actualizar la UI si la canción cambia
        MusicPlayer.onSongChanged = {
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Cada vez que la pantalla se muestra, actualizamos la UI y el actualizador del SeekBar
        updateUI()
    }

    override fun onPause() {
        super.onPause()
        // Detenemos el actualizador del seekbar cuando la pantalla no está visible
        stopSeekBarUpdate()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberamos el visualizador para evitar fugas de memoria
        blastVisualizer.release()
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
            
            MusicPlayer.mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                         blastVisualizer.setAudioSessionId(it.audioSessionId)
                    }
                } catch (e: Exception) {
                    // Ignorar si hay un error al enlazar el visualizador
                }
            }
            
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
    
    private fun updatePlayPauseButton() {
        MusicPlayer.mediaPlayer?.let {
            if (it.isPlaying) {
                 playPauseButton.setImageResource(R.drawable.ic_pause)
            } else {
                 playPauseButton.setImageResource(R.drawable.ic_play)
            }
        }
    }

    private fun initializeSeekBar() {
        MusicPlayer.mediaPlayer?.let {
            seekBar.max = it.duration
            startSeekBarUpdate()
        }
    }

    private fun startSeekBarUpdate() {
        stopSeekBarUpdate() // Detenemos cualquier actualizador anterior
        runnable = Runnable {
            MusicPlayer.mediaPlayer?.let {
                seekBar.progress = it.currentPosition
                updatePlayPauseButton() // Actualizamos el botón de play/pausa
            }
            handler.postDelayed(runnable!!, 1000)
        }
        handler.postDelayed(runnable!!, 1000)
    }

    private fun stopSeekBarUpdate() {
        runnable?.let { handler.removeCallbacks(it) }
    }
}
