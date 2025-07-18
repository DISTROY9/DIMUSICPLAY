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
    private val onSongClickListener: (Song) -> Unit
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.song_item, parent, false)
        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
        holder.itemView.setOnClickListener {
            onSongClickListener(song)
        }
    }

    override fun getItemCount(): Int = songs.size

    inner class SongViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.songTitleTextView)
        private val artistTextView: TextView = itemView.findViewById(R.id.songArtistTextView)
        // Corrección para songAlbumArtImageView
        private val albumArtImageView: ImageView = itemView.findViewById(R.id.songAlbumArtImageView) // Asumo que este es el ID en song_item.xml

        fun bind(song: Song) {
            titleTextView.text = song.title
            artistTextView.text = song.artist
            Glide.with(itemView.context)
                .load(song.albumArtUri)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(albumArtImageView) // Usar el nombre correcto de la variable
        }
    }
}
