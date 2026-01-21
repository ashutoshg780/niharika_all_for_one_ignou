//package com.example.niharika_all_for_one
//
//import android.content.Context
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.appcompat.app.AlertDialog
//import androidx.recyclerview.widget.RecyclerView
//import com.google.firebase.firestore.FirebaseFirestore
//import java.text.SimpleDateFormat
//import java.util.*
//
//class OrderCardAdapter(
//    private val context: Context,
//    private val orderList: List<Map<String, Any>>,
//    private val reloadCallback: () -> Unit
//) : RecyclerView.Adapter<OrderCardAdapter.OrderViewHolder>() {
//
//    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
//        val tvOrderedBy: TextView = itemView.findViewById(R.id.tvOrderedBy)
//        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
//        val tvPartsList: TextView = itemView.findViewById(R.id.tvOrderedList)
//        val btnApprove: Button = itemView.findViewById(R.id.btnArrived)
//        val btnPartial: Button = itemView.findViewById(R.id.btnPartial)
//        val btnNotArrived: Button = itemView.findViewById(R.id.btnNotArrived)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_order_card, parent, false)
//        return OrderViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
//        val order = orderList[position]
//        val orderId = order["orderId"] as? String ?: "Unknown"
//        val orderedBy = order["orderedBy"] as? String ?: "Unknown"
//        val orderDate = order["orderDate"] as? String ?: "--"
//        val partsList = order["parts"] as? List<*> ?: emptyList<Any>()
//
//        holder.tvOrderId.text = "Order ID: $orderId"
//        holder.tvOrderedBy.text = "Ordered By: $orderedBy"
//        holder.tvOrderDate.text = "Ordered On: $orderDate"
//        holder.tvPartsList.text = "Parts: ${partsList.joinToString()}"
//
//        holder.btnApprove.setOnClickListener { updateOrderStatus(orderId, "Completed", null) }
//        holder.btnNotArrived.setOnClickListener { updateOrderStatus(orderId, "Not Arrived", null) }
//        holder.btnPartial.setOnClickListener { showPartialDialog(orderId, partsList) }
//    }
//
//    override fun getItemCount(): Int = orderList.size
//
//    private fun showPartialDialog(orderId: String, items: List<*>) {
//        val dialogLayout = LinearLayout(context).apply {
//            orientation = LinearLayout.VERTICAL
//            setPadding(32, 32, 32, 32)
//        }
//
//        val inputMap = mutableMapOf<String, EditText>()
//        for (item in items) {
//            val itemStr = item.toString()
//            val label = TextView(context).apply { text = "Received qty for: $itemStr" }
//            val input = EditText(context).apply { hint = "Qty" }
//            inputMap[itemStr] = input
//            dialogLayout.addView(label)
//            dialogLayout.addView(input)
//        }
//
//        AlertDialog.Builder(context)
//            .setTitle("Partial Arrival Confirmation")
//            .setView(dialogLayout)
//            .setPositiveButton("Confirm") { _, _ ->
//                val receivedParts = mutableListOf<String>()
//                for ((itemStr, editText) in inputMap) {
//                    val qty = editText.text.toString().trim()
//                    if (qty.isNotEmpty()) receivedParts.add("$itemStr = $qty")
//                }
//                updateOrderStatus(orderId, "Partially Arrived", receivedParts)
//            }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun updateOrderStatus(orderId: String, status: String, receivedParts: List<String>?) {
//        val db = FirebaseFirestore.getInstance()
//        val now = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
//
//        val updates = mutableMapOf<String, Any>("status" to status)
//
//        db.collection("Spare_Parts_Orders").document(orderId)
//            .update(updates)
//            .addOnSuccessListener {
//                // Log entry for history
//                val logEntry = hashMapOf(
//                    "orderId" to orderId,
//                    "status" to status,
//                    "timestamp" to now
//                )
//                if (!receivedParts.isNullOrEmpty()) logEntry["receivedParts"] = receivedParts.joinToString(", ")
//
//                db.collection("Spare_Parts_Order_History")
//                    .document("$orderId-$status-$now")
//                    .set(logEntry)
//
//                // Update stock - new implementation (each part → document)
//                if (status != "Not Arrived") {
//                    val partsToUpdate = receivedParts ?: orderList.firstOrNull { it["orderId"] == orderId }?.get("parts") as? List<*> ?: emptyList<Any>()
//                    for (partEntry in partsToUpdate) {
//                        val partStr = if (receivedParts != null) {
//                            val parts = partEntry.split("=")
//                            parts[0].trim()
//                        } else {
//                            partEntry.toString().split(":")[0].trim()
//                        }
//
//                        val itemCodeName = partStr.split(" - ")
//                        if (itemCodeName.size >= 2) {
//                            val code = itemCodeName[0].trim()
//                            val name = itemCodeName[1].trim()
//                            val qty = if (receivedParts != null) {
//                                partEntry.split("=")[1].trim()
//                            } else {
//                                partEntry.toString().split(":")[1].trim()
//                            }
//
//                            val stockDoc = db.collection("Spare_Parts_Stock").document(code)
//                            val stockData = hashMapOf(
//                                "itemName" to name,
//                                "lastUpdatedBy" to "System",
//                                "lastUpdatedOn" to now,
//                                "stockQuantity" to qty
//                            )
//                            stockDoc.get().addOnSuccessListener { snapshot ->
//                                if (snapshot.exists()) {
//                                    // Update existing doc: add qty to existing
//                                    val existingQty = snapshot.getString("stockQuantity")?.toIntOrNull() ?: 0
//                                    val newQty = existingQty + (qty.toIntOrNull() ?: 0)
//                                    stockDoc.update(
//                                        "stockQuantity", newQty.toString(),
//                                        "lastUpdatedBy", "System",
//                                        "lastUpdatedOn", now
//                                    )
//                                } else {
//                                    // Create new doc
//                                    stockDoc.set(stockData)
//                                }
//                            }
//                        }
//                    }
//                }
//
//                Toast.makeText(context, "Order marked as $status", Toast.LENGTH_SHORT).show()
//                reloadCallback()
//            }
//            .addOnFailureListener {
//                Toast.makeText(context, "Failed to update order: ${it.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
//}
