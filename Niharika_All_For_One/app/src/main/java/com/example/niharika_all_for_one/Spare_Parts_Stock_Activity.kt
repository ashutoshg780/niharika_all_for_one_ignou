package com.example.niharika_all_for_one

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.niharika_all_for_one.network.AppPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class Spare_Parts_Stock_Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var sparePartsTable: TableLayout
    private lateinit var btnAddRemove: Button
    private lateinit var btnPartsOrder: Button
    private lateinit var btnPriceList: Button
    private var isEditable = false
    private val itemFields = mutableListOf<Triple<EditText, EditText, EditText>>()
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_spare_parts_stock)

        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        sparePartsTable = findViewById(R.id.sparePartsTable)
        btnAddRemove = findViewById(R.id.btnAddRemove)
        btnPartsOrder = findViewById(R.id.btnPartsOrder)
        btnPriceList = findViewById(R.id.btnPriceList)
        recycler = findViewById(R.id.recyclerOrderCards)

        checkUser()
        loadSpareParts()
        loadRecentOrders()

        Back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
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

        btnAddRemove.setOnClickListener {
            if (isEditable) saveStockToFirestore()
            else {
                isEditable = true
                btnAddRemove.text = "Done"
                loadSpareParts()
            }
        }

        btnPartsOrder.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Order_Activity::class.java))
        }

        btnPriceList.setOnClickListener {
            startActivity(Intent(this, Spare_Parts_Price_Activity::class.java))
        }
    }

//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null)
//        if (phoneNumber.isNullOrEmpty()) {
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
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


    private fun loadSpareParts() {
        sparePartsTable.removeViews(1, sparePartsTable.childCount - 1)
        itemFields.clear()

        val db = FirebaseFirestore.getInstance()

        // Read all documents in Spare_Parts_Stock collection
        db.collection("Spare_Parts_Stock")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    val code = doc.id // Document ID is itemCode
                    val name = doc.getString("itemName") ?: "Unknown"
                    val qty = doc.getString("stockQuantity") ?: "0"
                    addPartRow(code, name, qty)
                }

                // Add an empty row if editing is enabled
                if (isEditable) addPartRow("", "", "")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load stock: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPartRow(code: String, name: String, qty: String) {
        val row = TableRow(this)
        val codeEdit = makeEditableEditText(code, "Code", 1.65f)
        val nameEdit = makeEditableEditText(name, "Item", 5.85f)
        val qtyEdit = makeEditableEditText(qty, "Qty", 2.5f)

        row.addView(codeEdit)
        row.addView(nameEdit)
        row.addView(qtyEdit)

        if (isEditable) {
            row.setOnLongClickListener {
                sparePartsTable.removeView(row)
                itemFields.remove(Triple(codeEdit, nameEdit, qtyEdit))
                true
            }
        }

        sparePartsTable.addView(row)
        itemFields.add(Triple(codeEdit, nameEdit, qtyEdit))
    }

    private fun makeEditableEditText(value: String, hint: String, weight: Float) =
        EditText(this).apply {
            setText(value)
            this.hint = hint
            isEnabled = isEditable
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            gravity = Gravity.CENTER
        }

    private fun saveStockToFirestore() {
        val updatedList = mutableListOf<String>()
        for (i in 1 until sparePartsTable.childCount) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            val code = (row.getChildAt(0) as EditText).text.toString().trim()
            val name = (row.getChildAt(1) as EditText).text.toString().trim()
            val qty = (row.getChildAt(2) as EditText).text.toString().trim().ifEmpty { "0" }
            if (code.isNotEmpty() && name.isNotEmpty()) updatedList.add("$code: $name: $qty")
        }
        FirebaseFirestore.getInstance().collection("Spare_Parts_Stock")
            .document("master")
            .update("stockList", updatedList)
            .addOnSuccessListener {
                Toast.makeText(this, "Stock updated!", Toast.LENGTH_SHORT).show()
                isEditable = false
                btnAddRemove.text = "Add/ Remove"
                loadSpareParts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update stock: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadRecentOrders() {
        FirebaseFirestore.getInstance()
            .collection("Spare_Parts_Orders")
            .orderBy("orderedOn", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "Pending"
                    if (status != "Completed") mapOf(
                        "orderId" to doc.id,
                        "orderedBy" to (doc.getString("orderedBy") ?: "Unknown"),
                        "orderDate" to (doc.getString("orderedOn") ?: "--"),
                        "status" to status,
                        "parts" to (doc.get("orderedParts") as? List<*> ?: listOf<Any>())
                    ) else null
                }
                recycler.layoutManager = LinearLayoutManager(this)
                recycler.adapter = OrderCardAdapter(this, orders) {
                    loadRecentOrders()
                    loadSpareParts()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load orders!", Toast.LENGTH_SHORT).show()
                Log.e("OrderLoad", "${it.message}")
            }
    }

}
