package com.dimusicplay

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Importa esto

@Parcelize // Anotación para Parcelable
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // en milisegundos
    val data: String, // Ahora es la URI del contenido
    val albumArtUri: Uri? // URI para la carátula del álbum
) : Parcelable // Implementa Parcelable
