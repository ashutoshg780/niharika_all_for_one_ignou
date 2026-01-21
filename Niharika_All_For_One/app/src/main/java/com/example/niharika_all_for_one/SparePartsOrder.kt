package com.example.niharika_all_for_one

data class SparePartsOrder(
    val orderId: String = "",
    val orderedBy: String = "",
    val orderedOn: String = "",
    val status: String = "",
    val orderedParts: List<String> = emptyList()
)
