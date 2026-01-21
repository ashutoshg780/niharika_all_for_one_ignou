package com.example.niharika_all_for_one

data class Job(
    val jobId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val assignedEngineer: String = "",
    val complaintDate: String = "",
    val completionDate: String = "",
    val status: String = "",
    val createdDateTime: String = "",
    val createdAtTimestamp: Long = 0L
)
