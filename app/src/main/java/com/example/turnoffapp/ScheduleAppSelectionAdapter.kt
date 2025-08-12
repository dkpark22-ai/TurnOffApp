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

class ScheduleAppSelectionAdapter(
    private var apps: List<ApplicationInfo>,
    private var selectedApps: Set<String>,
    private val onAppSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<ScheduleAppSelectionAdapter.ViewHolder>() {

    private val currentlySelectedApps = selectedApps.toMutableSet()

    fun updateApps(newApps: List<ApplicationInfo>, newSelectedApps: Set<String>) {
        apps = newApps
        selectedApps = newSelectedApps
        currentlySelectedApps.clear()
        currentlySelectedApps.addAll(newSelectedApps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, currentlySelectedApps.contains(app.packageName)) { isSelected ->
            if (isSelected) {
                currentlySelectedApps.add(app.packageName)
            } else {
                currentlySelectedApps.remove(app.packageName)
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
                appIcon.setImageDrawable(null)
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