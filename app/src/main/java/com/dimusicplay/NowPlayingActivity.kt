package com.dimusicplay

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // Importar ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture

class NowPlayingActivity : AppCompatActivity() {

    private lateinit var albumArtImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var artistTextView: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var playPauseButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var previousButton: ImageButton
    // Eliminado: private lateinit var blastVisualizer: BlastVisualizer

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

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
        // Eliminado: blastVisualizer = findViewById(R.id.blastVisualizer)

        setupControls()
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            // Escuchar cambios de estado del MediaController
            mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                    updateUI()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }

                override fun onPositionDiscontinuity(
                    oldPosition: androidx.media3.common.Player.PositionInfo,
                    newPosition: androidx.media3.common.Player.PositionInfo,
                    reason: Int
                ) {
                    // Esto es útil para actualizar el seekbar con saltos bruscos (ej. seek)
                    startSeekBarUpdate()
                }
            })
            updateUI() // Actualizar UI una vez conectado
            startSeekBarUpdate() // Iniciar actualización del seekbar
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        super.onStop()
        stopSeekBarUpdate()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
            mediaController = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Eliminado: blastVisualizer.release()
    }

    private fun updateUI() {
        mediaController?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val song = (application as MainApplication).allSongsInService.find { it.data == currentMediaItem?.mediaId }

            if (song != null) {
                titleTextView.text = song.title
                artistTextView.text = song.artist
                Glide.with(this)
                    .load(song.albumArtUri)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(albumArtImageView)

                seekBar.max = controller.duration.toInt()
                seekBar.progress = controller.currentPosition.toInt() // Actualiza la posición inicial
            } else {
                titleTextView.text = "Título de la Canción"
                artistTextView.text = "Artista"
                albumArtImageView.setImageResource(R.drawable.ic_launcher_background)
                seekBar.max = 0
                seekBar.progress = 0
            }
            updatePlayPauseButton(controller.isPlaying)
        }
    }

    private fun setupControls() {
        playPauseButton.setOnClickListener {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            }
        }
        nextButton.setOnClickListener {
            mediaController?.seekToNextMediaItem()
        }
        previousButton.setOnClickListener {
            mediaController?.seekToPreviousMediaItem()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaController?.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_pause)
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play)
        }
    }

    private fun startSeekBarUpdate() {
        stopSeekBarUpdate()
        runnable = Runnable {
            mediaController?.let { controller ->
                if (controller.duration > 0) { // Solo actualiza si la duración es válida
                    seekBar.progress = controller.currentPosition.toInt()
                }
                updatePlayPauseButton(controller.isPlaying)
            }
            handler.postDelayed(runnable!!, 1000)
        }
        handler.postDelayed(runnable!!, 1000)
    }

    private fun stopSeekBarUpdate() {
        runnable?.let { handler.removeCallbacks(it) }
    }
}
