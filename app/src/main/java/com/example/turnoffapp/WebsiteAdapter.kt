package com.example.turnoffapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WebsiteAdapter(
    private val onDeleteWebsite: (String) -> Unit
) : RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder>() {

    private var websites = mutableListOf<String>()

    fun updateWebsites(newWebsites: List<String>) {
        websites.clear()
        websites.addAll(newWebsites)
        notifyDataSetChanged()
    }

    fun addWebsite(website: String) {
        if (!websites.contains(website)) {
            websites.add(website)
            notifyItemInserted(websites.size - 1)
        }
    }

    fun removeWebsite(website: String) {
        val index = websites.indexOf(website)
        if (index != -1) {
            websites.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getWebsites(): List<String> = websites.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebsiteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_website, parent, false)
        return WebsiteViewHolder(view)
    }

    override fun onBindViewHolder(holder: WebsiteViewHolder, position: Int) {
        holder.bind(websites[position])
    }

    override fun getItemCount(): Int = websites.size

    inner class WebsiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvWebsiteUrl: TextView = itemView.findViewById(R.id.tv_website_url)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        fun bind(website: String) {
            tvWebsiteUrl.text = website
            btnDelete.setOnClickListener {
                onDeleteWebsite(website)
            }
        }
    }
}