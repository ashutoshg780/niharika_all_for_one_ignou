package com.example.niharika_all_for_one

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class OrderCardAdapter(
    private val context: Context,
    private val orderList: List<Map<String, Any>>,
    private val reloadCallback: () -> Unit
) : RecyclerView.Adapter<OrderCardAdapter.OrderViewHolder>() {

    // ViewHolder binds order data to the card layout
    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvOrderedBy: TextView = itemView.findViewById(R.id.tvOrderedBy)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val tvPartsList: TextView = itemView.findViewById(R.id.tvOrderedList)
        val btnApprove: Button = itemView.findViewById(R.id.btnArrived)
        val btnPartial: Button = itemView.findViewById(R.id.btnPartial)
        val btnNotArrived: Button = itemView.findViewById(R.id.btnNotArrived)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_card, parent, false)
        return OrderViewHolder(view)
    }

    override fun getItemCount(): Int = orderList.size

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]
        val orderId = order["orderId"] as? String ?: "Unknown"
        val orderedBy = order["orderedBy"] as? String ?: "Unknown"
        val orderDate = order["orderDate"] as? String ?: "--"
        val partsList = order["parts"] as? List<*> ?: emptyList<Any>()

        holder.tvOrderId.text = "Order ID: $orderId"
        holder.tvOrderedBy.text = "Ordered By: $orderedBy"
        holder.tvOrderDate.text = "Ordered On: $orderDate"
        holder.tvPartsList.text = formatPartsList(partsList)

        holder.btnApprove.setOnClickListener {
            // Full order received, update all stocks and mark as completed
            updateOrderStatus(orderId, "Completed", partsList, null)
        }
        holder.btnNotArrived.setOnClickListener {
            // Order not arrived, just update status
            updateOrderStatus(orderId, "Not Arrived", null, null)
        }
        holder.btnPartial.setOnClickListener {
            // Show dialog for partial quantities
            showPartialDialog(orderId, partsList)
        }
    }

    // Formats ordered parts as "SP001 - Controller (x2), SP002 - Motor (x5)" for display
    private fun formatPartsList(partsList: List<*>): String {
        return partsList.joinToString(", ") { it?.toString() ?: "" }
    }

    // Dialog for partial arrival: prompts for each item how many were received
    private fun showPartialDialog(orderId: String, items: List<*>) {
        val dialogLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val inputMap = mutableMapOf<String, EditText>()
        for (item in items) {
            val itemStr = item?.toString() ?: continue // Format: "SP001 - Controller: 10"
            val displayStr = itemStr.substringBeforeLast(":").trim()
            val qtyHint = itemStr.substringAfterLast(":").trim()
            val label = TextView(context).apply { text = "Received qty for: $displayStr (Ordered: $qtyHint)" }
            val input = EditText(context).apply { hint = "Qty received (<= $qtyHint)" }
            inputMap[itemStr] = input
            dialogLayout.addView(label)
            dialogLayout.addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("Partial Arrival Confirmation")
            .setView(dialogLayout)
            .setPositiveButton("Confirm") { _, _ ->
                // Gather quantities entered by user
                val receivedParts = mutableListOf<Pair<String, Int>>() // Pair of itemStr, qtyReceived
                for ((itemStr, editText) in inputMap) {
                    val qtyReceived = editText.text.toString().trim().toIntOrNull() ?: 0
                    if (qtyReceived > 0) {
                        receivedParts.add(Pair(itemStr, qtyReceived))
                    }
                }
                // Proceed with partial update only if some items were received
                if (receivedParts.isNotEmpty()) {
                    updateOrderStatus(orderId, "Partially Arrived", null, receivedParts)
                } else {
                    Toast.makeText(context, "No quantity entered.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Updates the order and stock in Firestore:
     * - For "Completed", moves all items to stock and marks order as done.
     * - For "Not Arrived", only updates status.
     * - For "Partially Arrived", adds received qty to stock, updates remaining order qty, and removes items if completed.
     *
     * @param orderId Firestore Order doc ID
     * @param status New status for order ("Completed", "Partially Arrived", etc)
     * @param fullPartsList For "Completed", the full parts list (each as "SP001 - Controller: 10")
     * @param partialReceived List of pairs (itemString, qtyReceived) for partial arrival
     */
    private fun updateOrderStatus(
        orderId: String,
        status: String,
        fullPartsList: List<*>?, // used for completed orders
        partialReceived: List<Pair<String, Int>>? // used for partial arrival
    ) {
        val db = FirebaseFirestore.getInstance()
        val now = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        val updates = mutableMapOf<String, Any>("status" to status)

        // Update status in Spare_Parts_Orders
        db.collection("Spare_Parts_Orders").document(orderId)
            .update(updates)
            .addOnSuccessListener {
                // Add entry in order history
                val logEntry = hashMapOf(
                    "orderId" to orderId,
                    "status" to status,
                    "timestamp" to now
                )
                db.collection("Spare_Parts_Order_History")
                    .document("$orderId-$status-$now")
                    .set(logEntry)

                if (status == "Completed" && fullPartsList != null) {
                    // Add ALL items to stock and remove order from pending
                    processStockUpdate(fullPartsList.mapNotNull { part ->
                        parseOrderPartString(part?.toString() ?: "")
                    })
                    // Remove order from pending list
                    db.collection("Spare_Parts_Orders").document(orderId)
                        .update("orderedParts", listOf<String>())
                        .addOnSuccessListener { reloadCallback() }
                } else if (status == "Partially Arrived" && partialReceived != null) {
                    // Fetch order doc, update stock, and update remaining parts in order
                    db.collection("Spare_Parts_Orders").document(orderId)
                        .get().addOnSuccessListener { orderSnap ->
                            val currentParts = (orderSnap.get("orderedParts") as? List<*>)?.toMutableList() ?: mutableListOf()
                            val updatedParts = mutableListOf<String>()

                            // Map of itemCode to Pair(name, qty in order)
                            val partsMap = mutableMapOf<String, Pair<String, Int>>()
                            for (rawPart in currentParts) {
                                val p = parseOrderPartString(rawPart?.toString() ?: "")
                                if (p != null) {
                                    partsMap[p.first] = Pair(p.second, p.third)
                                }
                            }

                            // For each received item, update its stock and reduce from order
                            for ((itemStr, qtyReceived) in partialReceived) {
                                val part = parseOrderPartString(itemStr)
                                if (part != null) {
                                    val code = part.first
                                    val name = part.second
                                    val prevQty = part.third
                                    val qtyToAdd = qtyReceived.coerceAtMost(prevQty)

                                    // Update STOCK for this itemCode by qtyReceived
                                    updateStockDocument(code, name, qtyToAdd)

                                    // Subtract received from order quantity, keep if pending
                                    val remaining = (prevQty - qtyToAdd).coerceAtLeast(0)
                                    if (remaining > 0) {
                                        updatedParts.add("$code - $name: $remaining")
                                    }
                                    // else: do not add, meaning this part is completed
                                }
                            }

                            // For items NOT in partialReceived (not arrived at all), retain as is
                            val receivedCodes = partialReceived.mapNotNull { parseOrderPartString(it.first)?.first }.toSet()
                            for ((code, pair) in partsMap) {
                                if (code !in receivedCodes) {
                                    updatedParts.add("$code - ${pair.first}: ${pair.second}")
                                }
                            }

                            // Write back updated pending parts
                            db.collection("Spare_Parts_Orders").document(orderId)
                                .update("orderedParts", updatedParts)
                                .addOnSuccessListener {
                                    reloadCallback()
                                    Toast.makeText(context, "Order and stock updated!", Toast.LENGTH_SHORT).show()
                                }
                        }
                } else {
                    reloadCallback()
                    Toast.makeText(context, "Order marked as $status", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update order: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Parse a part string "SP001 - Controller: 10" to Triple(itemCode, itemName, quantity)
    private fun parseOrderPartString(str: String): Triple<String, String, Int>? {
        // Defensive checks for null/empty strings
        if (!str.contains("-") || !str.contains(":")) return null
        val codeAndRest = str.split("-", limit = 2)
        if (codeAndRest.size < 2) return null
        val itemCode = codeAndRest[0].trim()
        val nameAndQty = codeAndRest[1].split(":", limit = 2)
        if (nameAndQty.size < 2) return null
        val itemName = nameAndQty[0].trim()
        val qty = nameAndQty[1].trim().toIntOrNull() ?: 0
        return Triple(itemCode, itemName, qty)
    }

    // Bulk update: adds quantities to stock for each item (full approval)
    private fun processStockUpdate(parts: List<Triple<String, String, Int>>) {
        for ((itemCode, itemName, qty) in parts) {
            updateStockDocument(itemCode, itemName, qty)
        }
    }

    // Updates the Spare_Parts_Stock for a single itemCode: adds 'qty' to stock, sets lastUpdatedOn/By
    private fun updateStockDocument(itemCode: String, itemName: String, qtyToAdd: Int) {
        val db = FirebaseFirestore.getInstance()
        val now = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        val stockDoc = db.collection("Spare_Parts_Stock").document(itemCode)

        stockDoc.get().addOnSuccessListener { snap ->
            val existingQty = snap.getString("stockQuantity")?.toIntOrNull() ?: 0
            val newQty = existingQty + qtyToAdd
            val data = hashMapOf(
                "itemName" to itemName,
                "lastUpdatedBy" to "System",
                "lastUpdatedOn" to now,
                "stockQuantity" to newQty.toString()
            )
            stockDoc.set(data)
        }
    }
}
