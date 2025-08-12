package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedWebsitesAdapter(
    private val onRemoveWebsite: (String) -> Unit
) : RecyclerView.Adapter<BlockedWebsitesAdapter.ViewHolder>() {

    private var blockedWebsites: List<String> = emptyList()

    fun updateWebsites(websites: Set<String>) {
        blockedWebsites = websites.toList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_website, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val website = blockedWebsites[position]
        holder.bind(website, onRemoveWebsite)
    }

    override fun getItemCount(): Int = blockedWebsites.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val websiteUrl: TextView = itemView.findViewById(R.id.tv_website_url)
        private val removeButton: Button = itemView.findViewById(R.id.btn_remove)

        fun bind(url: String, onRemoveWebsite: (String) -> Unit) {
            websiteUrl.text = url

            removeButton.setOnClickListener {
                onRemoveWebsite(url)
            }
        }
    }
}