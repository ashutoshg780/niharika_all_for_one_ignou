package com.example.niharika_all_for_one

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class JobAdapter(
    private val jobList: List<Job>,
    private val onJobClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {


    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val jobDetails: TextView = itemView.findViewById(R.id.tvJobDetails)
        val notificationIcon: ImageView = itemView.findViewById(R.id.notificationIcon)
        val phoneIcon: ImageView = itemView.findViewById(R.id.phoneIcon)
        val jobCard: CardView = itemView.findViewById(R.id.jobCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun getItemCount(): Int = jobList.size

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobList[position]

        holder.jobDetails.text = """
        Complaint No: ${job.jobId}
        Assigned to: ${job.assignedEngineer}
        Complaint Date: ${job.complaintDate}
        Completion Date: ${job.completionDate}
    """.trimIndent()

        // Set card background color based on status logic
        holder.jobCard.setCardBackgroundColor(determineCardColor(job))

        // ✅ Handle item click and pass job object
        holder.itemView.setOnClickListener {
            onJobClick(job)
        }
    }

    /**
     * Determines the color of the job card based on job status and complaint date.
     *
     * Color logic:
     *  - Completed: Green (#4CAF50)
     *  - New: Yellow (#FFC107)
     *  - Pending: Orange (#FF5722)
     *  - If status is New or Pending and complaint date is more than 7 days old: Red (#F44336)
     *  - Ended: Grey (#9E9E9E)
     *
     * @param job The job to determine the color for.
     * @return The color code for the job card.
     */
    private fun determineCardColor(job: Job): Int {
        val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val currentTimeMillis = System.currentTimeMillis()

        val complaintDateMillis = try {
            sdf.parse(job.complaintDate)?.time ?: currentTimeMillis
        } catch (e: Exception) {
            currentTimeMillis // default to current time if parse fails
        }

        val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
        val isOlderThanWeek = (currentTimeMillis - complaintDateMillis) > oneWeekMillis

        return when {
            job.status.equals("completed", ignoreCase = true) -> Color.parseColor("#4CAF50") // Green
            (job.status.equals("new", ignoreCase = true) || job.status.equals("pending", ignoreCase = true)) && isOlderThanWeek ->
                Color.parseColor("#F44336") // Red if new/pending and older than a week
            job.status.equals("new", ignoreCase = true) -> Color.parseColor("#FFC107") // Yellow
            job.status.equals("pending", ignoreCase = true) -> Color.parseColor("#FF5722") // Orange
            else -> Color.parseColor("#9E9E9E") // Default Grey
        }
    }

}
