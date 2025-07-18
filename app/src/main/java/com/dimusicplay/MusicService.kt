package com.dimusicplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

// Constantes
const val NOTIFICATION_CHANNEL_ID = "dimusic_play_channel"
const val NOTIFICATION_ID = 101

class MusicService : MediaSessionService() {

    private lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var musicRepository: MusicRepository
    private var allSongs: List<Song> = emptyList()
    private var currentSongIndex: Int = -1

    companion object {
        var instance: MusicService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        createNotificationChannel()

        musicRepository = MusicRepository(applicationContext)
        allSongs = musicRepository.getAllAudioFiles()
        (application as? MainApplication)?.allSongsInService = allSongs // Compartir la lista de canciones

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        val mediaItems = allSongs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.data) // Usamos la ruta como ID único
                .setUri(song.data)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }
        exoPlayer.setMediaItems(mediaItems)
        exoPlayer.prepare()

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setId("DIMusicPlaySession")
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentSongIndex = exoPlayer.currentMediaItemIndex
                updateNotification()
            }

            // *** CORRECCIÓN CRÍTICA AQUÍ: ELIMINAR EL 'fun' DUPLICADO ***
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    // ExoPlayer ya maneja la transición a la siguiente si hay más items
                }
            }
        })

        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaSession = mediaSession

    fun playSongFromList(song: Song) {
        val index = allSongs.indexOf(song)
        if (index != -1) {
            exoPlayer.seekToDefaultPosition(index)
            exoPlayer.play()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Reproducción de Música DIMusic Play"
            val descriptionText = "Notificaciones para el reproductor de música de DIMusic Play"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val currentMediaItem = exoPlayer.currentMediaItem
        val mainApplicationInstance = application as MainApplication
        val song = if (currentMediaItem != null) {
            mainApplicationInstance.allSongsInService.find { it.data == currentMediaItem.mediaId }
        } else {
            null
        }

        val playPauseIcon = if (exoPlayer.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction("ACTION_PREVIOUS"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction("ACTION_PLAY_PAUSE"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction("ACTION_NEXT"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MusicService::class.java).setAction("ACTION_STOP"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var albumArtBitmap: Bitmap? = null
        val albumArtUri = currentMediaItem?.mediaMetadata?.artworkUri ?: song?.albumArtUri
        albumArtUri?.let { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    albumArtBitmap = BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
            }
        }
        if (albumArtBitmap == null) {
            albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_background)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(song?.title ?: "No hay canción")
            .setContentText(song?.artist ?: "DIMUSIC Play")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(albumArtBitmap)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media3.app.NotificationCompat.MediaStyle(mediaSession)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(android.R.drawable.ic_media_previous, "Anterior", prevIntent)
            .addAction(playPauseIcon, "Play/Pause", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Siguiente", nextIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)

        return notificationBuilder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!exoPlayer.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        instance = null
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                "ACTION_PREVIOUS" -> exoPlayer.seekToPreviousMediaItem()
                "ACTION_PLAY_PAUSE" -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                "ACTION_NEXT" -> exoPlayer.seekToNextMediaItem()
                "ACTION_STOP" -> stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}
