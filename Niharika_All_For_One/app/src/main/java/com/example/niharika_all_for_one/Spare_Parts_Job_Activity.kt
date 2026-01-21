package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Spare_Parts_Job_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView

    private lateinit var tvJOBID: TextView
    private lateinit var btnAddRemove: Button
    private lateinit var btnPriceList: Button
    private lateinit var btnStock: Button
    private lateinit var sparePartsTable: TableLayout

    // Table rows: Spinner, Name, Quantity fields (per row)
    private val itemFields = mutableListOf<Triple<Spinner, TextView, EditText>>()

    private var isEditable = false
    private var jobId: String? = null

    // All available parts (code - name) for spinner
    private var stockPartsList = listOf<String>()
    // Holds the last loaded parts state for diffing when saving
    private var prevPartsList = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_job)
        Log.d("SPJOB", "onCreate: Activity started.")

        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        tvJOBID = findViewById(R.id.tvJOBID)
        btnAddRemove = findViewById(R.id.btnAddRemove)
        btnPriceList = findViewById(R.id.btnPriceList)
        btnStock = findViewById(R.id.btnStock)
        sparePartsTable = findViewById(R.id.sparePartsTable)

        jobId = intent.getStringExtra("jobId")
        if (jobId.isNullOrEmpty()) {
            Log.e("SPJOB", "Job ID not received!")
            Toast.makeText(this, "Job ID not received!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        tvJOBID.text = "Job ID: $jobId"
        Log.d("SPJOB", "Job ID set: $jobId")

        checkUser()
        Log.d("SPJOB", "Loading spinner stock parts list.")
        loadStockPartsList {
            Log.d("SPJOB", "Stock parts loaded, loading job's used parts table.")
            loadUsedPartsTable()
        }

//        LogOut.setOnClickListener {
//            Log.d("SPJOB", "Logout button clicked.")
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            Log.d("SPJOB", "Logout button clicked.")
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        Back.setOnClickListener {
            Log.d("SPJOB", "Back button clicked.")
            onBackPressedDispatcher.onBackPressed()
        }
        btnPriceList.setOnClickListener {
            Log.d("SPJOB", "Navigating to Spare_Parts_Price_Activity.")
            startActivity(Intent(this, Spare_Parts_Price_Activity::class.java))
        }
        btnStock.setOnClickListener {
            Log.d("SPJOB", "Navigating to Spare_Parts_Stock_Activity.")
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }
        btnAddRemove.setOnClickListener {
            if (!isEditable) {
                Log.d("SPJOB", "Edit mode enabled.")
                isEditable = true
                btnAddRemove.text = "Done"
                loadUsedPartsTable()
            } else {
                Log.d("SPJOB", "Done clicked, saving parts list.")
                isEditable = false
                btnAddRemove.text = "Add/ Remove"
                saveUsedParts()
            }
        }
    }

    // === Header Role/Name for UI ===
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null) ?: ""
//        if (phoneNumber.isEmpty()) {
//            Log.e("SPJOB", "Phone number not found in SharedPreferences!")
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
//        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
//            .addOnSuccessListener {
//                val role = it.getString("role") ?: ""
//                val name = it.getString("fullName") ?: ""
//                RoleTitle.text = "$role: $name"
//                Log.d("SPJOB", "Role and name loaded: $role: $name")
//            }
//    }

    // Replace checkUser() function:
    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            Log.e("SPJOB", "Phone number not found in AppPreferences!")
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber).get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                RoleTitle.text = "$role: $name"
                Log.d("SPJOB", "Role and name loaded: $role: $name")
            }
    }

    // === Load all available stock parts for spinner ===
    private fun loadStockPartsList(onLoaded: () -> Unit) {
        FirebaseFirestore.getInstance().collection("Spare_Parts_Stock")
            .get()
            .addOnSuccessListener { snapshot ->
                val parts = mutableListOf<String>()
                for (doc in snapshot.documents) {
                    val code = doc.id
                    val name = doc.getString("itemName") ?: ""
                    if (code.isNotEmpty() && name.isNotEmpty()) {
                        parts.add("$code - $name")
                    }
                }
                stockPartsList = parts
                Log.d("SPJOB", "Stock spinner items loaded: $stockPartsList")
                onLoaded()
            }
            .addOnFailureListener {
                Log.e("SPJOB", "Failed to load stock items! ${it.message}")
                Toast.makeText(this, "Failed to load stock items!", Toast.LENGTH_SHORT).show()
                onLoaded()
            }
    }

    // === Loads the table from "Spare_Parts_final_bill" doc (no per-role doc here!) ===
    private fun loadUsedPartsTable() {
        sparePartsTable.removeViews(1, sparePartsTable.childCount - 1)
        itemFields.clear()
        prevPartsList.clear()
        Log.d("SPJOB", "Loading used parts table for job $jobId.")

        FirebaseFirestore.getInstance()
            .collection("Jobs")
            .document("job.$jobId")
            .collection("Spare_Parts_Used")
            .document("Spare_Parts_final_bill")
            .get()
            .addOnSuccessListener { doc ->
                val partsList = doc.get("parts") as? List<Map<String, Any>> ?: emptyList()
                prevPartsList = partsList.map { HashMap(it) }.toMutableList()
                Log.d("SPJOB", "Loaded previous parts: $prevPartsList")
                for (part in partsList) {
                    val code = part["itemCode"] as? String ?: ""
                    val name = part["itemName"] as? String ?: ""
                    val qty = (part["quantity"] as? Long)?.toString()
                        ?: (part["quantity"] as? Int)?.toString()
                        ?: (part["quantity"] as? String) ?: ""
                    addPartRow(code, name, qty)
                }
                if (isEditable) addPartRow("", "", "") // Add blank row for editing
            }
            .addOnFailureListener {
                Log.e("SPJOB", "Failed to load job parts! ${it.message}")
                if (isEditable) addPartRow("", "", "")
            }
    }

    // === Adds a new row: spinner, item name, qty field. ===
    private fun addPartRow(code: String, name: String, qty: String) {
        val row = TableRow(this)

        // Spinner for item code/name
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stockPartsList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Set spinner selection to match code-name if found
        val selIndex = stockPartsList.indexOfFirst { it.startsWith(code) && it.contains(name) }
        if (selIndex >= 0) spinner.setSelection(selIndex)
        spinner.isEnabled = isEditable

        // TextView shows selected name
        val nameView = TextView(this).apply {
            text = name
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 5.85f)
        }

        // Keep nameView in sync with spinner selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val partString = stockPartsList.getOrNull(pos) ?: ""
                nameView.text = partString.split(" - ", limit = 2).getOrNull(1) ?: ""
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Quantity field
        val qtyEdit = EditText(this).apply {
            setText(qty)
            hint = "Qty"
            gravity = Gravity.CENTER
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.5f)
            isEnabled = isEditable
        }

        // If editing, allow long-press to remove row and update stock
        if (isEditable) {
            row.setOnLongClickListener {
                Log.d("SPJOB", "Row long-pressed, will remove row and add stock back (if any).")
                val oldCode = getSelectedCode(spinner)
                val oldQty = qtyEdit.text.toString().trim().toIntOrNull() ?: 0
                if (oldCode.isNotEmpty() && oldQty > 0) {
                    adjustStock(oldCode, nameView.text.toString(), oldQty)
                }
                sparePartsTable.removeView(row)
                itemFields.remove(Triple(spinner, nameView, qtyEdit))
                true
            }
        }
        // Table columns: Spinner (weight 1.65), ItemName (5.85), Qty (2.5)
        spinner.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.65f)
        row.addView(spinner)
        row.addView(nameView)
        row.addView(qtyEdit)
        sparePartsTable.addView(row)
        itemFields.add(Triple(spinner, nameView, qtyEdit))
        Log.d("SPJOB", "Added row: $code - $name x$qty")
    }

    // Helper to get code from spinner's selection ("SP001 - MCR Controller" => "SP001")
    private fun getSelectedCode(spinner: Spinner): String {
        val sel = spinner.selectedItem?.toString() ?: ""
        return sel.split(" - ", limit = 2).getOrNull(0) ?: ""
    }

    // Helper to get name from spinner's selection
    private fun getSelectedName(spinner: Spinner): String {
        val sel = spinner.selectedItem?.toString() ?: ""
        return sel.split(" - ", limit = 2).getOrNull(1) ?: ""
    }

    // === Save used parts: AGGREGATES ALL ROWS by itemCode to avoid duplicates in bill ===
    private fun saveUsedParts() {
        Log.d("SPJOB", "Starting saveUsedParts()")

        val db = FirebaseFirestore.getInstance()
        // Will use this to sum all quantities per itemCode
        val aggregatePartsMap = mutableMapOf<String, Pair<String, Int>>() // code -> Pair<itemName, qty>

        // Build from UI: aggregate any duplicate codes (sum quantities)
        for ((spinner, nameView, qtyEdit) in itemFields) {
            val sel = spinner.selectedItem?.toString() ?: continue
            val code = getSelectedCode(spinner)
            val name = getSelectedName(spinner)
            val qty = qtyEdit.text.toString().trim().toIntOrNull() ?: 0
            if (code.isNotEmpty() && name.isNotEmpty() && qty > 0) {
                val prev = aggregatePartsMap[code]
                if (prev == null) aggregatePartsMap[code] = Pair(name, qty)
                else aggregatePartsMap[code] = Pair(name, prev.second + qty)
            }
        }

        // Build final array for Firestore
        val partsToSave = aggregatePartsMap.map { (code, pair) ->
            mapOf("itemCode" to code, "itemName" to pair.first, "quantity" to pair.second)
        }

        Log.d("SPJOB", "partsToSave (aggregated): $partsToSave")

        // Map previous total qty per code for diffing
        val prevQtyMap = mutableMapOf<String, Int>()
        for (part in prevPartsList) {
            val code = part["itemCode"] as? String ?: continue
            val qty = (part["quantity"] as? Long)?.toInt()
                ?: (part["quantity"] as? Int)
                ?: (part["quantity"] as? String)?.toIntOrNull()
                ?: 0
            prevQtyMap[code] = prevQtyMap.getOrDefault(code, 0) + qty
        }
        Log.d("SPJOB", "prevQtyMap: $prevQtyMap")

        // Diff each part code: new-used, update stock accordingly
        for (code in aggregatePartsMap.keys.union(prevQtyMap.keys)) {
            val newQty = aggregatePartsMap[code]?.second ?: 0
            val prevQty = prevQtyMap[code] ?: 0
            val diff = newQty - prevQty
            Log.d("SPJOB", "For $code, newQty=$newQty, prevQty=$prevQty, diff=$diff")
            if (diff != 0) {
                val name = aggregatePartsMap[code]?.first ?: prevPartsList.find { (it["itemCode"] as? String) == code }?.get("itemName") as? String
                adjustStock(code, name, -diff)
            }
        }

        // Save final bill doc with aggregated unique codes
        db.collection("Jobs")
            .document("job.$jobId")
            .collection("Spare_Parts_Used")
            .document("Spare_Parts_final_bill")
            .set(mapOf("parts" to partsToSave))
            .addOnSuccessListener {
                Log.d("SPJOB", "Parts saved to Spare_Parts_final_bill successfully!")
                Toast.makeText(this, "Parts saved!", Toast.LENGTH_SHORT).show()
                // Update prevPartsList so future edits work correctly
                prevPartsList = partsToSave.map { HashMap(it) }.toMutableList()
                loadUsedPartsTable()
            }
            .addOnFailureListener {
                Log.e("SPJOB", "Failed to save parts! ${it.message}")
                Toast.makeText(this, "Failed to save parts: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // === Stock adjuster: add/subtract qty for code (if name provided, update, else fetch from db) ===
    private fun adjustStock(itemCode: String, itemName: String?, diff: Int) {
        if (diff == 0) return
        val db = FirebaseFirestore.getInstance()
        val stockDoc = db.collection("Spare_Parts_Stock").document(itemCode)
        stockDoc.get()
            .addOnSuccessListener { snap ->
                val existingQty = snap.getString("stockQuantity")?.toIntOrNull() ?: 0
                val newQty = existingQty + diff
                if (newQty < 0) {
                    Log.e("SPJOB", "Warning: Stock for $itemCode would go negative! Not updating.")
                    Toast.makeText(this, "Stock for $itemCode cannot go negative!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val updateMap = mutableMapOf<String, Any>(
                    "stockQuantity" to newQty.toString()
                )
                if (itemName != null) updateMap["itemName"] = itemName
                stockDoc.update(updateMap)
                    .addOnSuccessListener {
                        Log.d("SPJOB", "Stock for $itemCode updated by $diff to $newQty.")
                    }
                    .addOnFailureListener {
                        Log.e("SPJOB", "Failed to update stock for $itemCode! ${it.message}")
                    }
            }
            .addOnFailureListener {
                Log.e("SPJOB", "Stock doc for $itemCode not found! ${it.message}")
            }
    }
}
