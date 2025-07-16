package com.dimusicplay

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import java.io.IOException

object MusicPlayer {

    var mediaPlayer: MediaPlayer? = null
    var currentSong: Song? = null
    private var songs: List<Song> = emptyList()
    private var currentSongIndex = -1
    
    var onSongChanged: (() -> Unit)? = null

    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.start()
                onSongChanged?.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer?.pause()
                    onSongChanged?.invoke()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                mediaPlayer?.stop()
                release()
                onSongChanged?.invoke()
            }
        }
    }

    fun initialize(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun requestAudioFocus(): Boolean {
        val result: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                setOnAudioFocusChangeListener(onAudioFocusChangeListener)
                build()
            }
            result = audioManager?.requestAudioFocus(audioFocusRequest!!) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            result = audioManager?.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }
    
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(onAudioFocusChangeListener)
        }
    }

    fun setSongList(songList: List<Song>) {
        this.songs = songList
    }

    fun playSong(song: Song) {
        if (!requestAudioFocus()) {
            return
        }

        currentSong = song
        currentSongIndex = songs.indexOf(song)
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.path)
                setOnPreparedListener {
                    it.start()
                    // Notificamos a la UI que la canción ha cambiado y está lista
                    onSongChanged?.invoke()
                }
                prepareAsync()
                setOnCompletionListener {
                    playNextSong()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun playNextSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % songs.size
            playSong(songs[currentSongIndex])
        }
    }

    fun playPreviousSong() {
        if (songs.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex - 1 < 0) songs.size - 1 else currentSongIndex - 1
            playSong(songs[currentSongIndex])
        }
    }

    fun pauseOrResume() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
            // Notificamos a la UI para que actualice el botón de play/pausa
            onSongChanged?.invoke()
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentSong = null
        abandonAudioFocus()
    }
}
