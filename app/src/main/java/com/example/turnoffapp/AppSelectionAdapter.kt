package com.example.turnoffapp

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private var apps: List<ApplicationInfo>,
    private var blockedApps: Set<String>,
    private val onAppSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    private val selectedApps = blockedApps.toMutableSet()

    fun updateApps(newApps: List<ApplicationInfo>, newBlockedApps: Set<String>) {
        apps = newApps
        blockedApps = newBlockedApps
        selectedApps.clear()
        selectedApps.addAll(newBlockedApps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, selectedApps.contains(app.packageName)) { isSelected ->
            if (isSelected) {
                selectedApps.add(app.packageName)
            } else {
                selectedApps.remove(app.packageName)
            }
            onAppSelectionChanged(app.packageName, isSelected)
        }
    }

    override fun getItemCount(): Int = apps.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIcon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        private val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        private val checkBox: CheckBox = itemView.findViewById(R.id.cb_app_selected)

        fun bind(app: ApplicationInfo, isSelected: Boolean, onSelectionChanged: (Boolean) -> Unit) {
            val context = itemView.context
            val packageManager = context.packageManager

            try {
                appIcon.setImageDrawable(packageManager.getApplicationIcon(app))
                appName.text = packageManager.getApplicationLabel(app).toString()
            } catch (e: Exception) {
                appName.text = app.packageName
            }
            
            checkBox.isChecked = isSelected

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChanged(isChecked)
            }

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }
}