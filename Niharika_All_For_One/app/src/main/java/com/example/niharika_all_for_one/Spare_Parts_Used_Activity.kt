package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Spare_Parts_Used_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Header elements (top bar views)
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView

    // Job ID textView
    private lateinit var tvJOBID: TextView

    // Add/Remove toggle button
    private lateinit var btnAddRemove: Button

    // TableLayout for dynamic spare parts rows
    private lateinit var sparePartsTable: TableLayout

    // Current edit/view mode toggle
    private var isEditable = false

    // Job ID passed via intent from previous page
    private var jobId: String? = null

    // Store the parts from stock for dropdown and validation
    private val stockPartsList = mutableListOf<String>() // Format: "SP001 - MCR Controller"

    // Track all dynamic rows: Pair of Spinner for ItemCode-Name and EditText for Quantity
    private val itemFields = mutableListOf<Pair<Spinner, EditText>>()

    // Store previously saved parts from Firestore to calculate stock diffs on save
    private var previouslySavedParts = mutableMapOf<String, Int>() // itemCode -> quantity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_used)

        auth = FirebaseAuth.getInstance()

        // === Header View Initialization ===
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // === UI Elements Initialization ===
        tvJOBID = findViewById(R.id.tvJOBID)
        btnAddRemove = findViewById(R.id.btnAddRemove)
        sparePartsTable = findViewById(R.id.sparePartsTable)

        // === Get Job ID from Intent ===
        jobId = intent.getStringExtra("jobId")
        if (jobId.isNullOrEmpty()) {
            Toast.makeText(this, "Job ID not received!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Show Job ID at top
        tvJOBID.text = "Job ID: $jobId"

        // Load role and name for header
        checkUser()

        // Logout button
//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        // Back button
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Load stock parts for dropdown spinner & prefill used parts if present
        loadStockParts()

        // Add/Remove toggle button: toggles edit mode and saves updates with stock adjustments
        btnAddRemove.setOnClickListener {
            if (isEditable) {
                // Save the updates when Done is clicked
                saveUsedParts()
            } else {
                // Enter edit mode
                isEditable = true
                btnAddRemove.text = "Done"
                loadUsedParts()
            }
        }
    }

    // === Fetch Role & Name and Display in Header ===
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null) ?: return
//        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
//            .addOnSuccessListener {
//                val role = it.getString("role") ?: ""
//                val name = it.getString("fullName") ?: ""
//                RoleTitle.text = "$role: $name"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone() ?: return

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                RoleTitle.text = "$role: $name"
            }
    }

    // === Load all parts from stock collection to build dropdown options ===
    private fun loadStockParts() {
        val db = FirebaseFirestore.getInstance()
        db.collection("Spare_Parts_Stock")
            .get()
            .addOnSuccessListener { snapshot ->
                stockPartsList.clear()
                for (doc in snapshot.documents) {
                    val code = doc.id
                    val name = doc.getString("itemName") ?: "Unknown"
                    stockPartsList.add("$code - $name")
                }
                loadUsedParts() // Load used parts only after stock parts loaded
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load stock parts: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // === Load existing used parts for this job from Firestore and display in table ===
    private fun loadUsedParts() {
        sparePartsTable.removeViews(1, sparePartsTable.childCount - 1) // Remove all except header row
        itemFields.clear()
        previouslySavedParts.clear()

        val db = FirebaseFirestore.getInstance()
        val docId = jobId ?: return

        val engineerName = RoleTitle.text.toString()
        db.collection("Jobs").document("job.$docId")
            .collection("Spare_Parts_Used")
            .document(engineerName)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val partsList = doc.get("parts") as? List<Map<String, Any>> ?: emptyList()
                    if (partsList.isEmpty() && isEditable) {
                        addUsedPartRow("", "", "1") // Add empty row for edit
                    } else {
                        for (part in partsList) {
                            val code = part["itemCode"] as? String ?: ""
                            val name = part["itemName"] as? String ?: ""
                            val qty = (part["quantity"]?.toString() ?: "1").toIntOrNull() ?: 1
                            addUsedPartRow(code, name, qty.toString())
                            // Aggregate quantity if multiple rows of same code exist
                            previouslySavedParts[code] = (previouslySavedParts[code] ?: 0) + qty
                        }
                        if (isEditable) addUsedPartRow("", "", "1") // Allow adding new row in edit mode
                    }
                } else {
                    if (isEditable) addUsedPartRow("", "", "1")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load used parts: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // === Add one row with Spinner for item and EditText for qty ===
    private fun addUsedPartRow(selectedCodeName: String, selectedName: String, quantity: String) {
        val row = TableRow(this)

        // Spinner for itemCode - name, disabled if not editable
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stockPartsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.isEnabled = isEditable
        val spinnerParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.65f)
        spinner.layoutParams = spinnerParams

        // Set selection if exists
        val selectedIndex = stockPartsList.indexOfFirst { it.startsWith(selectedCodeName) }
        if (selectedIndex >= 0) spinner.setSelection(selectedIndex)

        // Item Name TextView (non-editable, updated from spinner selection)
        val tvName = TextView(this).apply {
            text = if (selectedCodeName.isNotEmpty()) selectedCodeName.substringAfter("-").trim() else selectedName
            setTextColor(resources.getColor(android.R.color.black))
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 5.85f)
        }

        // Quantity EditText, enabled only in edit mode
        val qtyEdit = EditText(this).apply {
            setText(if (quantity.isEmpty()) "1" else quantity)
            inputType = InputType.TYPE_CLASS_NUMBER
            isEnabled = isEditable
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.5f)
        }

        // Update item name text when spinner selection changes
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long
            ) {
                val selected = stockPartsList[position]
                val name = selected.substringAfter("-").trim()
                tvName.text = name
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        row.addView(spinner)
        row.addView(tvName)
        row.addView(qtyEdit)

        // Long press to remove row only in edit mode (also update stock by adding back qty of removed item)
        if (isEditable) {
            row.setOnLongClickListener {
                // Get item code and qty before removal
                val removedCodeName = spinner.selectedItem?.toString() ?: ""
                val removedQtyStr = qtyEdit.text.toString().trim()
                val removedQty = removedQtyStr.toIntOrNull() ?: 0

                if (removedCodeName.isNotEmpty() && removedQty > 0) {
                    val itemCode = removedCodeName.substringBefore("-").trim()
                    val itemName = removedCodeName.substringAfter("-").trim()
                    addBackToStock(itemCode, itemName, removedQty)
                }

                sparePartsTable.removeView(row)
                itemFields.remove(Pair(spinner, qtyEdit))
                true
            }
        }

        sparePartsTable.addView(row)
        itemFields.add(Pair(spinner, qtyEdit))
    }

    // === Save updated used parts back to Firestore and update stock quantities ===
    private fun saveUsedParts() {
        val docId = jobId ?: return
        val engineerName = RoleTitle.text.toString()
        val db = FirebaseFirestore.getInstance()

        val partsToSave = mutableListOf<Map<String, Any>>()
        val currentPartsMap = mutableMapOf<String, Int>() // itemCode -> qty (aggregated)

        // Gather all filled rows - skip rows with empty selection or zero quantity
        for ((spinner, qtyEdit) in itemFields) {
            val selectedItem = spinner.selectedItem?.toString() ?: ""
            if (selectedItem.isEmpty()) continue
            val qtyStr = qtyEdit.text.toString().trim()
            val qty = qtyStr.toIntOrNull() ?: 0
            if (qty <= 0) continue

            val itemCode = selectedItem.substringBefore("-").trim()
            val itemName = selectedItem.substringAfter("-").trim()

            // Aggregate quantity for same itemCode
            currentPartsMap[itemCode] = (currentPartsMap[itemCode] ?: 0) + qty
        }

        if (currentPartsMap.isEmpty()) {
            Toast.makeText(this, "No parts to save!", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare partsToSave list with aggregated quantities for Firestore
        for ((code, qty) in currentPartsMap) {
            val itemName = stockPartsList.find { it.startsWith(code) }?.substringAfter("-")?.trim() ?: "Unknown"
            partsToSave.add(
                mapOf(
                    "itemCode" to code,
                    "itemName" to itemName,
                    "quantity" to qty
                )
            )
        }

        // Calculate stock differences (same logic as before)
        val stockUpdates = mutableMapOf<String, Pair<String, Int>>() // itemCode -> (itemName, qtyChange)

        previouslySavedParts.forEach { (code, oldQty) ->
            val newQty = currentPartsMap[code] ?: 0
            val diff = oldQty - newQty
            if (diff != 0) {
                val itemName = stockPartsList.find { it.startsWith(code) }?.substringAfter("-")?.trim() ?: "Unknown"
                stockUpdates[code] = Pair(itemName, diff)
            }
        }

        for ((code, newQty) in currentPartsMap) {
            if (!previouslySavedParts.containsKey(code)) {
                val itemName = stockPartsList.find { it.startsWith(code) }?.substringAfter("-")?.trim() ?: "Unknown"
                stockUpdates[code] = Pair(itemName, -newQty)
            }
        }

        // 1) Save individual engineer parts data and update stock accordingly
        db.collection("Jobs").document("job.$docId")
            .collection("Spare_Parts_Used")
            .document(engineerName)
            .set(mapOf("parts" to partsToSave))
            .addOnSuccessListener {
                var updatesDone = 0
                val totalUpdates = stockUpdates.size
                if (totalUpdates == 0) {
                    updateFinalBill(db, docId) // Update final bill doc after save
                } else {
                    stockUpdates.forEach { (code, pair) ->
                        val (itemName, qtyChange) = pair
                        updateStockQuantity(code, itemName, qtyChange) {
                            updatesDone++
                            if (updatesDone == totalUpdates) {
                                updateFinalBill(db, docId) // Update final bill doc after all stock updates done
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save parts: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // New helper function to update the "Spare_Parts_final_bill" document by aggregating all engineers' parts
    private fun updateFinalBill(db: FirebaseFirestore, docId: String) {
        // Read all documents inside Spare_Parts_Used collection (per engineer)
        db.collection("Jobs").document("job.$docId")
            .collection("Spare_Parts_Used")
            .get()
            .addOnSuccessListener { snapshot ->
                val aggregateParts = mutableMapOf<String, Pair<String, Int>>() // itemCode -> (itemName, totalQty)

                for (doc in snapshot.documents) {
                    if (doc.id == "Spare_Parts_final_bill") continue // skip final bill itself
                    val partsList = doc.get("parts") as? List<Map<String, Any>> ?: continue
                    for (part in partsList) {
                        val code = part["itemCode"] as? String ?: continue
                        val name = part["itemName"] as? String ?: "Unknown"
                        val qty = (part["quantity"]?.toString()?.toIntOrNull()) ?: 0
                        if (qty <= 0) continue

                        val existing = aggregateParts[code]
                        if (existing == null) {
                            aggregateParts[code] = Pair(name, qty)
                        } else {
                            aggregateParts[code] = Pair(name, existing.second + qty)
                        }
                    }
                }

                // Prepare list for Firestore
                val partsToSave = aggregateParts.map { (code, pair) ->
                    mapOf(
                        "itemCode" to code,
                        "itemName" to pair.first,
                        "quantity" to pair.second
                    )
                }

                // Save or update final bill document
                db.collection("Jobs").document("job.$docId")
                    .collection("Spare_Parts_Used")
                    .document("Spare_Parts_final_bill")
                    .set(mapOf("parts" to partsToSave))
                    .addOnSuccessListener {
                        Toast.makeText(this, "Spare parts usage saved and final bill updated!", Toast.LENGTH_SHORT).show()
                        isEditable = false
                        btnAddRemove.text = "Add/ Remove"
                        loadUsedParts() // Reload UI after save
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update final bill: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to read parts for final bill: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Helper: update stock quantity for given itemCode by qtyChange (positive = add stock, negative = reduce)
    private fun updateStockQuantity(itemCode: String, itemName: String, qtyChange: Int, onComplete: () -> Unit) {
        if (qtyChange == 0) {
            onComplete()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val stockDoc = db.collection("Spare_Parts_Stock").document(itemCode)

        stockDoc.get().addOnSuccessListener { snap ->
            val existingQty = snap.getString("stockQuantity")?.toIntOrNull() ?: 0
            val newQty = (existingQty + qtyChange).coerceAtLeast(0) // prevent negative stock

            val now = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a").format(java.util.Date())
            val data = mapOf(
                "itemName" to itemName,
                "lastUpdatedBy" to RoleTitle.text.toString(),
                "lastUpdatedOn" to now,
                "stockQuantity" to newQty.toString()
            )

            stockDoc.set(data)
                .addOnSuccessListener { onComplete() }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to update stock for $itemCode: ${it.message}", Toast.LENGTH_SHORT).show()
                    onComplete()
                }
        }.addOnFailureListener {
            // If no doc, create new with qtyChange as stockQuantity (if positive)
            if (qtyChange > 0) {
                val now = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a").format(java.util.Date())
                val data = mapOf(
                    "itemName" to itemName,
                    "lastUpdatedBy" to RoleTitle.text.toString(),
                    "lastUpdatedOn" to now,
                    "stockQuantity" to qtyChange.toString()
                )
                stockDoc.set(data)
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to create stock for $itemCode: ${it.message}", Toast.LENGTH_SHORT).show()
                        onComplete()
                    }
            } else {
                // Can't reduce stock for non-existing item, just continue
                onComplete()
            }
        }
    }

    // When removing an item via long press, add back that quantity to stock immediately
    private fun addBackToStock(itemCode: String, itemName: String, quantity: Int) {
        if (quantity <= 0) return
        updateStockQuantity(itemCode, itemName, quantity) {
            Toast.makeText(this, "Added back $quantity of $itemCode to stock", Toast.LENGTH_SHORT).show()
        }
    }

    // Called after successful save and stock updates
    private fun onSaveSuccess() {
        Toast.makeText(this, "Spare parts usage saved and stock updated!", Toast.LENGTH_SHORT).show()
        isEditable = false
        btnAddRemove.text = "Add/ Remove"
        loadUsedParts()
    }
}
