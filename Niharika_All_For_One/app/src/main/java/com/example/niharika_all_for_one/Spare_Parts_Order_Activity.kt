package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Spare_Parts_Order_Activity : AppCompatActivity() {

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Header Views
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView

    // Buttons
    private lateinit var btnAddRemove: Button
    private lateinit var btnPriceList: Button
    private lateinit var btnPartsOrder: Button
    private lateinit var btnPartsOrderHistory: Button
    private lateinit var btnPartsStock: Button

    // TableLayout for dynamic rows
    private lateinit var sparePartsTable: TableLayout

    // Editable mode toggle
    private var isEditable = false

    // List to track Spinner and Quantity EditText per row
    private val itemFields = mutableListOf<Pair<Spinner, EditText>>()

    // List of part names from Firestore (e.g., "SP001 - MCR Controller")
    private var partNameList = listOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_order)

        auth = FirebaseAuth.getInstance()

        // Initialize Header Views
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // Initialize Buttons
        btnAddRemove = findViewById(R.id.btnAddRemove)
        btnPartsOrder = findViewById(R.id.btnPartsOrder)
        btnPriceList = findViewById(R.id.btnPriceList)
        btnPartsOrderHistory = findViewById(R.id.btnPartsOrderHistory)
        btnPartsStock = findViewById(R.id.btnPartsLeft)

        // Initialize TableLayout
        sparePartsTable = findViewById(R.id.sparePartsTable)

        // Load user role and name
        checkUser()

        // Fetch spare parts list from Firestore
        fetchSparePartsList()

        // Logout functionality
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

        // Back navigation
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Toggle Editable Mode
//        btnAddRemove.setOnClickListener {
//            isEditable = !isEditable
//            btnAddRemove.text = if (isEditable) "Done" else "Add/ Remove"
//            loadSpareParts()
//        }


        btnAddRemove.setOnClickListener {
            isEditable = !isEditable
            btnAddRemove.text = if (isEditable) "Done" else "Add/ Remove"

            if (!isEditable) {
                // When switching to view mode, remove empty rows
                val rowsToRemove = mutableListOf<TableRow>()
                for (i in 1 until sparePartsTable.childCount) { // skip header
                    val row = sparePartsTable.getChildAt(i) as? TableRow ?: continue
                    val spinner = row.getChildAt(0) as Spinner
                    val qtyEdit = row.getChildAt(1) as EditText

                    val selectedItem = spinner.selectedItem?.toString()?.trim() ?: ""
                    val qty = qtyEdit.text.toString().trim()

                    // If no item selected or quantity empty → mark for removal
                    if (selectedItem.isEmpty() || qty.isEmpty()) {
                        rowsToRemove.add(row)
                    } else {
                        // Otherwise, disable editing
                        spinner.isEnabled = false
                        qtyEdit.isEnabled = false
                    }
                }

                // Remove empty rows
                for (row in rowsToRemove) {
                    sparePartsTable.removeView(row)
                    itemFields.removeIf { it.first == row.getChildAt(0) && it.second == row.getChildAt(1) }
                }

                Toast.makeText(this, "Editable mode OFF. Empty rows removed.", Toast.LENGTH_SHORT).show()
            } else {
                // Switching to editable mode
                addPartRow("", "")
                Toast.makeText(this, "Editable mode ON. You can add/remove rows.", Toast.LENGTH_SHORT).show()
            }
        }


        // Navigate to Price List Page
        btnPriceList.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Price_Activity::class.java))
        }

        // Navigate to Stock Page
        btnPartsStock.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

        // Navigate to Order History Page
        btnPartsOrderHistory.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Order_History_Activity::class.java))
        }

        // Create Order Button
        btnPartsOrder.setOnClickListener {
            createSparePartsOrder()
        }
    }

    // Load user role and name
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null)
//        if (phoneNumber.isNullOrEmpty()) {
//            Toast.makeText(this, "Phone number not found!", Toast.LENGTH_SHORT).show()
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
//
//        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber)
//            .get()
//            .addOnSuccessListener {
//                val role = it.getString("role") ?: ""
//                val name = it.getString("fullName") ?: ""
//                RoleTitle.text = "$role: $name"
//            }
//    }

    private fun checkUser() {
        val prefs = AppPreferences(this)
        val phoneNumber = prefs.getPhone()

        if (phoneNumber.isNullOrEmpty()) {
            Toast.makeText(this, "Phone number not found!", Toast.LENGTH_SHORT).show()
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
            return
        }

        FirebaseFirestore.getInstance().collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                RoleTitle.text = "$role: $name"
            }
    }


    // Fetch Spare Parts List from Firestore
    private fun fetchSparePartsList() {
        FirebaseFirestore.getInstance()
            .collection("Spare_Parts_Prices")
            .get()
            .addOnSuccessListener { result ->
                partNameList = result.documents.mapNotNull { doc ->
                    val itemName = doc.getString("itemName")
                    if (itemName != null) "${doc.id} - $itemName" else null
                }
                loadSpareParts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load parts list", Toast.LENGTH_SHORT).show()
            }
    }

    // Load TableLayout with dynamic rows
    private fun loadSpareParts() {
        // Remove all rows except the header (assumed to be the first row)
        while (sparePartsTable.childCount > 1) {
            sparePartsTable.removeViewAt(1)
        }
        itemFields.clear()

        // Add one initial empty row if in edit mode
        if (isEditable) {
            addPartRow("", "")
        }
    }

    // Add a row with Spinner and Quantity EditText
    private fun addPartRow(selectedItem: String, qty: String) {
        val tableRow = TableRow(this)

        // Spinner for item list
        val itemSpinner = Spinner(this).apply {
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.65f)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, partNameList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        itemSpinner.adapter = adapter

        // Set selected item if present
        val selectedIndex = partNameList.indexOfFirst { it.startsWith(selectedItem) }
        if (selectedIndex >= 0) itemSpinner.setSelection(selectedIndex)

        itemSpinner.isEnabled = isEditable

        // Quantity EditText
        val qtyEdit = EditText(this).apply {
            setText(qty)
            hint = "Qty"
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
            isEnabled = isEditable
        }

        // Long click to remove row
        if (isEditable) {
            tableRow.setOnLongClickListener {
                sparePartsTable.removeView(tableRow)
                itemFields.remove(Pair(itemSpinner, qtyEdit))
                true
            }
        }

        // Add views to TableRow
        tableRow.addView(itemSpinner)
        tableRow.addView(qtyEdit)

        // Add TableRow to TableLayout
        sparePartsTable.addView(tableRow)

        // Track these fields for saving
        itemFields.add(Pair(itemSpinner, qtyEdit))
    }

    // Create and save order to Firestore
    private fun createSparePartsOrder() {
        val orderList = mutableListOf<String>()
        for ((spinner, qtyField) in itemFields) {
            val selectedItem = spinner.selectedItem?.toString()?.trim() ?: continue
            val qty = qtyField.text.toString().trim().ifEmpty { "1" }

            if (selectedItem.isNotEmpty() && qty.isNotEmpty()) {
                orderList.add("$selectedItem: $qty")
            }
        }

        if (orderList.isEmpty()) {
            Toast.makeText(this, "Please add items before placing order", Toast.LENGTH_SHORT).show()
            return
        }

        // === Get current date/time and format as ddMMyy.hhmm ===
        val dateFormat = SimpleDateFormat("ddMMyy.HHmm", Locale.getDefault())
        val formattedDateTime = dateFormat.format(Date()) // e.g., 02042025.1224

        // === Build document ID and data ===
        val docId = "SPODR.$formattedDateTime"
        val displayDateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val displayDateStr = displayDateFormat.format(Date())

        val orderData = mapOf(
            "orderedBy" to RoleTitle.text.toString(),
            "orderedOn" to displayDateStr,
            "status" to "Pending",
            "orderedParts" to orderList
        )

        // === Save with custom doc ID ===
        FirebaseFirestore.getInstance()
            .collection("Spare_Parts_Orders")
            .document(docId)
            .set(orderData)
            .addOnSuccessListener {
                Toast.makeText(this, "Order created successfully!", Toast.LENGTH_SHORT).show()
                loadSpareParts() // Reset table
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create order: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
