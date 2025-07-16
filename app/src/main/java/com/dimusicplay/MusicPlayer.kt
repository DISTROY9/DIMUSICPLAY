package com.dimusicplay

import android.media.MediaPlayer
import java.io.IOException

// Un 'object' en Kotlin es un Singleton: solo existirá una instancia de él en toda la app.
object MusicPlayer {

    var mediaPlayer: MediaPlayer? = null
    var currentSong: Song? = null
    private var songs: List<Song> = emptyList()
    private var currentSongIndex = -1
    
    var onSongChanged: (() -> Unit)? = null

    fun setSongList(songList: List<Song>) {
        this.songs = songList
    }

    fun playSong(song: Song) {
        currentSong = song
        currentSongIndex = songs.indexOf(song)
        
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(song.path)
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                    onSongChanged?.invoke()
                }
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
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        currentSong = null
    }
}
