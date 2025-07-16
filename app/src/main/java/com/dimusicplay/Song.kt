package com.dimusicplay

import android.net.Uri

// Este archivo ahora contiene la definición central de lo que es una "Canción".
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val path: String,
    val albumArtUri: Uri
)
