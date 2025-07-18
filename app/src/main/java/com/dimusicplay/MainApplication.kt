package com.dimusicplay

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope // Importar lifecycleScope si no está
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch // Importar launch para corrutinas

class MainApplication : Application() {

    var allSongsInService: List<Song> = emptyList() // Lista de canciones accesible globalmente

    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null // Hacerlo accesible si lo necesitas en otras partes

    override fun onCreate() {
        super.onCreate()

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        mediaControllerFuture?.addListener({
            mediaController = mediaControllerFuture?.get()
        }, { it.run() })

        // Esto es un ejemplo de cómo podrías usar ProcessLifecycleOwner,
        // pero no es estrictamente necesario para el MusicService si siempre está en primer plano.
        // Lo dejo por si lo necesitas para otras lógicas de ciclo de vida de la aplicación.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            // Lógica que depende del ciclo de vida del proceso de la aplicación
        }

        // Iniciar el servicio al inicio de la aplicación para que esté listo.
        val serviceIntent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    fun playSongInService(song: Song) {
        MusicService.instance?.playSongFromList(song)
    }

    override fun onTerminate() {
        super.onTerminate()
        mediaControllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
