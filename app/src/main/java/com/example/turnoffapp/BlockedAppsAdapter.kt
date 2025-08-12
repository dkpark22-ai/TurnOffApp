package com.example.turnoffapp

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BlockedAppsAdapter(
    private val onRemoveApp: (String) -> Unit
) : RecyclerView.Adapter<BlockedAppsAdapter.ViewHolder>() {

    private var blockedApps: List<String> = emptyList()

    fun updateApps(apps: Set<String>) {
        blockedApps = apps.toList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_blocked_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageName = blockedApps[position]
        holder.bind(packageName, onRemoveApp)
    }

    override fun getItemCount(): Int = blockedApps.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val removeButton: Button = itemView.findViewById(R.id.btn_remove)

        fun bind(packageName: String, onRemoveApp: (String) -> Unit) {
            val context = itemView.context
            val packageManager = context.packageManager

            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                appIcon.setImageDrawable(packageManager.getApplicationIcon(appInfo))
                appName.text = packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                appName.text = packageName
            }

            removeButton.setOnClickListener {
                onRemoveApp(packageName)
            }
        }
    }
}