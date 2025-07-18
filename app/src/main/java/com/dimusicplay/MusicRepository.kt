package com.dimusicplay

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.net.Uri

class MusicRepository(private val context: Context) {

    fun getAllAudioFiles(): List<Song> {
        val songs = mutableListOf<Song>()
        val collection =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            // MediaStore.Audio.Media.DATA, // Deprecated, mejor usar _ID para URI
            MediaStore.Audio.Media.ALBUM_ID
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            // val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA) // No usamos DATA directamente

            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown Title"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                // Usar Content URI para la data de la canción
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val dataPath = contentUri.toString() // Usamos la URI como el 'data' de la canción

                val albumId = cursor.getLong(albumIdColumn)
                val albumArtUri = if (albumId != 0L) {
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                } else {
                    null
                }

                if (duration > 30000) {
                    songs.add(
                        Song(id, title, artist, album, duration, dataPath, albumArtUri) // Usamos dataPath
                    )
                }
            }
        }
        return songs
    }
}
