package com.example.miniperplexity

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SourceAdapter(private val items: List<WebLink>) :
    RecyclerView.Adapter<SourceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.source_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_source, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.itemView.setOnClickListener {
            val ctx = holder.itemView.context
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
            ctx.startActivity(i)
        }
    }

    override fun getItemCount(): Int = items.size
}
