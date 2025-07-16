package com.dimusicplay

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class NowPlayingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_now_playing)

        // Por ahora, esta actividad solo muestra el layout.
        // En el futuro, recibirá la información de la canción y sincronizará los controles.
    }
}
