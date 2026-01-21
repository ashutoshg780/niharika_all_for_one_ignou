package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Spare_Parts_Price_Activity : AppCompatActivity() {

    // Firebase Auth and Firestore
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    // Header UI elements
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView

    // Table and buttons
    private lateinit var sparePartsTable: TableLayout
    private lateinit var btnAddNewSparePart: Button
    private lateinit var btnPriceList: Button
    private lateinit var btnPartsStock: Button
    private lateinit var btnPartsOrder: Button
    private lateinit var btnPartsPriceHistory: Button

    // Editable field storage (Triple: Item Code, Item Name, Price)
    private val itemFields = mutableListOf<Triple<EditText, EditText, EditText>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_price)

        auth = FirebaseAuth.getInstance()

        // === Header UI ===
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)

        // === Buttons ===
        btnAddNewSparePart = findViewById(R.id.btnAddNewSparePart)
        btnPriceList = findViewById(R.id.btnPriceList)
        btnPartsStock = findViewById(R.id.btnPartsLeft)
        btnPartsOrder = findViewById(R.id.btnPartsOrder)
        btnPartsPriceHistory = findViewById(R.id.btnPartsPriceHistory)

        // === TableLayout initialization ===
        sparePartsTable = findViewById(R.id.sparePartsTable)

        // === Fetch user role + name ===
        checkUser()

        // === Load existing price list ===
        loadPriceList()

        // === Logout logic ===
//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//        }

        LogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, Start_Screen_Activity::class.java))
            finish()
        }

        // === Back logic ===
        Back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // === Add new part row ===
        btnAddNewSparePart.setOnClickListener {
            // Adds a blank editable row
            addPartRow("", "", "", editableCode = true, editableName = true)
        }

        // === Update prices (also save new spare parts if added) ===
        btnPriceList.setOnClickListener {
            updateOrAddParts()
        }

        // === Spare Parts Stock Page ===
        btnPartsStock.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Stock_Activity::class.java))
        }

        // === Spare Parts Order Page ===
        btnPartsOrder.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Order_Activity::class.java))
        }

        // === Spare Parts Price History Page ===
        btnPartsPriceHistory.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Price_History_Activity::class.java))
        }
    }

    // === Show role and name in header ===
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
//        db.collection("Users").document(phoneNumber)
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

        db.collection("Users").document(phoneNumber)
            .get()
            .addOnSuccessListener {
                val role = it.getString("role") ?: ""
                val name = it.getString("fullName") ?: ""
                RoleTitle.text = "$role: $name"
            }
    }


    // === Load all spare parts prices from Firestore ===
    private fun loadPriceList() {
        db.collection("Spare_Parts_Prices").get()
            .addOnSuccessListener { result ->
                // Remove all rows except the header (assumed to be the first row)
                val childCount = sparePartsTable.childCount
                if (childCount > 1) {
                    sparePartsTable.removeViews(1, childCount - 1)
                }
                itemFields.clear()

                // Populate table with existing parts
                for (document in result.documents) {
                    val itemCode = document.id
                    val itemName = document.getString("itemName") ?: ""
                    val price = document.getString("price") ?: ""
                    addPartRow(itemCode, itemName, price, editableCode = false, editableName = false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load prices!", Toast.LENGTH_SHORT).show()
            }
    }

    // === Create row with 3 fields (code, name, price) ===
    private fun addPartRow(code: String, name: String, price: String, editableCode: Boolean, editableName: Boolean) {
        val row = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        val codeEdit = EditText(this).apply {
            setText(code)
            hint = "Code"
            isEnabled = editableCode
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.65f)
        }

        val nameEdit = EditText(this).apply {
            setText(name)
            hint = "Item"
            isEnabled = editableName
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 5.85f)
        }

        val priceEdit = EditText(this).apply {
            setText(price)
            hint = "Price"
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2.5f)
        }

        row.addView(codeEdit)
        row.addView(nameEdit)
        row.addView(priceEdit)

        sparePartsTable.addView(row)
        itemFields.add(Triple(codeEdit, nameEdit, priceEdit))
    }

    // === Update existing prices or add new spare parts in Firestore ===
    private fun updateOrAddParts() {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        val timestamp = sdf.format(Date())

        for ((codeField, nameField, priceField) in itemFields) {
            val code = codeField.text.toString().trim()
            val name = nameField.text.toString().trim()
            val price = priceField.text.toString().trim()

            if (code.isNotEmpty() && name.isNotEmpty() && price.isNotEmpty()) {
                val data = mapOf(
                    "itemName" to name,
                    "price" to price,
                    "lastUpdatedBy" to RoleTitle.text.toString(),
                    "lastUpdatedOn" to timestamp
                )

                // Save or update in Spare_Parts_Prices
                db.collection("Spare_Parts_Prices").document(code).set(data)

                // Log price change in Spare_Parts_Price_History
                val historyData = mapOf(
                    "itemName" to name,
                    "price" to price,
                    "updatedBy" to RoleTitle.text.toString(),
                    "updatedOn" to timestamp
                )
                db.collection("Spare_Parts_Price_History")
                    .document("${code}_SPH_$timestamp")
                    .set(historyData)
            }
        }

        Toast.makeText(this, "Prices and new parts updated!", Toast.LENGTH_SHORT).show()
        loadPriceList() // Reload updated data
    }
}
