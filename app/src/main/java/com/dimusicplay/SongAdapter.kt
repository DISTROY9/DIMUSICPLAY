package com.dimusicplay

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class SongAdapter(
    private val songs: List<Song>,
    private val onItemClicked: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
        val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView) // Referencia al ImageView
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

        // --- USAMOS GLIDE PARA CARGAR LA IMAGEN ---
        Glide.with(holder.itemView.context)
            .load(song.albumArtUri) // La URI de la carátula
            .placeholder(R.mipmap.ic_launcher) // Una imagen de reserva mientras carga
            .error(R.mipmap.ic_launcher) // Una imagen de reserva si hay un error
            .into(holder.albumArtImageView) // El ImageView donde se mostrará

        holder.itemView.setOnClickListener {
            onItemClicked(song)
        }
    }
}
