package com.dimusicplay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(private val songs: List<Song>) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    // Esta clase interna representa la vista de cada fila de nuestra lista
    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
    }

    // Este método se llama para crear una nueva vista de fila cuando es necesario
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    // Este método devuelve la cantidad total de elementos en nuestra lista
    override fun getItemCount(): Int {
        return songs.size
    }

    // Este método se llama para rellenar los datos de una canción en una vista de fila específica
    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.titleTextView.text = song.title
        holder.artistTextView.text = song.artist
    }
}
