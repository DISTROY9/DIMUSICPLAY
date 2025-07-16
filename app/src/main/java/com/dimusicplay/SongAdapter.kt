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
    // La función ahora recibe la canción Y la vista de la carátula
    private val onItemClicked: (Song, View) -> Unit 
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
        val albumArtImageView: ImageView = itemView.findViewById(R.id.albumArtImageView)
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

        Glide.with(holder.itemView.context)
            .load(song.albumArtUri)
            .placeholder(R.mipmap.ic_launcher)
            .error(R.mipmap.ic_launcher)
            .into(holder.albumArtImageView)

        holder.itemView.setOnClickListener {
            // Pasamos la vista de la carátula junto con la canción
            onItemClicked(song, holder.albumArtImageView)
        }
    }
}
