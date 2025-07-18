package com.dimusicplay

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer // Ya no necesitamos Observer directo, LiveData lo maneja mejor con lambda
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.dimusicplay.databinding.ActivityMainBinding
import com.google.common.util.concurrent.ListenableFuture
import com.bumptech.glide.Glide // Importar Glide

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Contrato para solicitar permisos
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permiso de almacenamiento concedido.", Toast.LENGTH_SHORT).show()
                connectToMediaSession()
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado. No se podrá escanear música.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        requestStoragePermission()

        binding.miniPlayerBar.setOnClickListener {
            // Lanza la actividad NowPlayingActivity
            val intent = Intent(this, NowPlayingActivity::class.java)
            startActivity(intent)
        }

        binding.miniPlayerPlayPause.setOnClickListener {
            mediaController?.let { controller ->
                if (controller.isPlaying) {
                    controller.pause()
                } else {
                    controller.play()
                }
            } ?: Toast.makeText(this, "Servicio de música no conectado", Toast.LENGTH_SHORT).show()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_library -> {
                    // Escanear y si hay canciones, empezar a reproducir la primera si no hay nada sonando
                    mediaController?.let { controller ->
                        val songs = MusicRepository(applicationContext).getAllAudioFiles()
                        if (songs.isNotEmpty()) {
                            Toast.makeText(this, "Encontradas ${songs.size} canciones!", Toast.LENGTH_SHORT).show()
                            if (!controller.isPlaying && controller.currentMediaItem == null) {
                                (application as MainApplication).playSongInService(songs[0])
                            }
                        } else {
                            Toast.makeText(this, "No se encontraron canciones en el dispositivo.", Toast.LENGTH_LONG).show()
                        }
                    } ?: Toast.makeText(this, "Servicio de música no conectado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_search -> {
                    Toast.makeText(this, "Search selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_settings -> {
                    Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (ContextCompat.checkSelfPermission(this, getRequiredPermission()) == PackageManager.PERMISSION_GRANTED) {
            connectToMediaSession()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
            mediaController = null // Limpiar la referencia
        }
    }

    private fun requestStoragePermission() {
        val permission = getRequiredPermission()
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun connectToMediaSession() {
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
            observeMediaController()
            // Iniciar el servicio explícitamente después de conectar para asegurar que esté en primer plano
            // Esto también se hace en MainApplication.onCreate, pero un extra aquí no daña.
            ContextCompat.startForegroundService(this, Intent(this, MusicService::class.java))
        }, ContextCompat.getMainExecutor(this))
    }

    private fun observeMediaController() {
        mediaController?.addListener(object : androidx.media3.common.Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val song = (application as MainApplication).allSongsInService.find { it.data == mediaItem?.mediaId }
                updateMiniPlayerUI(song)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton(isPlaying)
            }
        })

        // Inicializar la UI con el estado actual
        mediaController?.let { controller ->
            val currentMediaItem = controller.currentMediaItem
            val song = (application as MainApplication).allSongsInService.find { it.data == currentMediaItem?.mediaId }
            updateMiniPlayerUI(song)
            updatePlayPauseButton(controller.isPlaying)
        }
    }

    private fun updateMiniPlayerUI(song: Song?) {
        if (song != null) {
            binding.miniPlayerSongTitle.text = song.title
            binding.miniPlayerArtist.text = song.artist
            Glide.with(this)
                .load(song.albumArtUri)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(binding.miniPlayerAlbumArt)
        } else {
            binding.miniPlayerSongTitle.text = "No hay canción"
            binding.miniPlayerArtist.text = "DIMUSIC Play"
            binding.miniPlayerAlbumArt.setImageResource(R.drawable.ic_launcher_background)
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        if (isPlaying) {
            binding.miniPlayerPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            binding.miniPlayerPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }
    }
}
