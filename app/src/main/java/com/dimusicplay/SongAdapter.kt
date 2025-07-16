package com.dimusicplay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// El constructor ahora acepta una función lambda para manejar los clics
class SongAdapter(
    private val songs: List<Song>,
    private val onItemClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.titleTextView.text = song.title
        holder.artistTextView.text = song.artist

        // Configuramos el listener para que llame a nuestra función lambda cuando se toque la vista
        holder.itemView.setOnClickListener {
            onItemClicked(song)
        }
    }
}
