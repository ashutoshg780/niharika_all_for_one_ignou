package com.example.niharika_all_for_one

import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.pdf.PdfDocument
import com.example.niharika_all_for_one.network.AppPreferences

class Detailed_Bill_Activity : AppCompatActivity() {

    // 🔧 Firebase & Database
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    // 🔧 Views
    private lateinit var RoleTitle: TextView
    private lateinit var LogOut: ImageView
    private lateinit var Back: ImageView
    private lateinit var tvJOBID: TextView
    private lateinit var sparePartsTable: TableLayout
    private lateinit var btnUpdateBill: Button
    private lateinit var btnMismatch: Button
    private lateinit var btnDiscount: Button
    private lateinit var btnFinalizeBill: Button
    private lateinit var tvTotalBill: TextView
    private lateinit var tvDiscount: TextView
    private lateinit var tvFinalBill: TextView

    // 🔧 Job data
    private var jobId = ""
    private var isEditMode = false
    private var discount = 0

    // For PDF
    private var pdfDocumentToSave: PdfDocument? = null
    private val CREATE_PDF_REQUEST_CODE = 1001

    // --- Store price list in memory for instant access
    private var itemPriceMap = mutableMapOf<String, Int>() // code -> price

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detailed_bill)

        // 🏷️ View bindings
        auth = FirebaseAuth.getInstance()
        RoleTitle = findViewById(R.id.profileRole)
        LogOut = findViewById(R.id.logoutButton)
        Back = findViewById(R.id.backButton)
        tvJOBID = findViewById(R.id.tvJOBID)
        sparePartsTable = findViewById(R.id.sparePartsTable)
        btnUpdateBill = findViewById(R.id.btnUpdateBill)
        btnMismatch = findViewById(R.id.btnMismatch)
        btnDiscount = findViewById(R.id.btnDiscount)
        btnFinalizeBill = findViewById(R.id.btnFinalizeBill)
        tvTotalBill = findViewById(R.id.TotalBill)
        tvDiscount = findViewById(R.id.Discount)
        tvFinalBill = findViewById(R.id.FinalBill)

        // 🔄 Check user login
        checkUser()

        // ⬅️ Back Button
        Back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // 🔑 Logout Button
//        LogOut.setOnClickListener {
//            FirebaseAuth.getInstance().signOut()
//            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply()
//            startActivity(Intent(this, Start_Screen_Activity::class.java).apply {
//                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            })
//            finish()
//        }

        LogOut.setOnClickListener {
            val prefs = AppPreferences(this)
            prefs.clearPreferences()
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Start_Screen_Activity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        // 📄 Job ID from intent
        jobId = intent.getStringExtra("jobId") ?: ""
        tvJOBID.text = "Job ID: $jobId"

        // 🔄 Load Price list first, then Bill data
        loadPriceList { loadBillData() }

        // Edit Mode toggle (Mismatch Button)
        btnMismatch.setOnClickListener {
            isEditMode = !isEditMode
            toggleEditMode(isEditMode)

            if (isEditMode) {
                addBillRow("", "", "1", "0")
                Toast.makeText(this, "Edit Mode ON. Empty row added for new entry.", Toast.LENGTH_SHORT).show()
            } else {
                removeEmptyRows()
                Toast.makeText(this, "Edit Mode OFF. Empty rows removed.", Toast.LENGTH_SHORT).show()
            }
        }

        // 💾 Save Updates to Firestore
        btnUpdateBill.setOnClickListener {
            removeEmptyRows()
            saveBillUpdates()
        }

        // 💰 Discount Dialog
        btnDiscount.setOnClickListener { showDiscountDialog() }

        // 🏁 Finalize Bill: Save updates & create PDF
        btnFinalizeBill.setOnClickListener {
            removeEmptyRows()
            saveBillUpdates()
            createBillPdf()
        }
    }

    // 🔄 Check user authentication & load profile
//    private fun checkUser() {
//        val phoneNumber = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("phone", null)
//        if (phoneNumber.isNullOrEmpty()) {
//            FirebaseAuth.getInstance().signOut()
//            startActivity(Intent(this, Start_Screen_Activity::class.java))
//            finish()
//            return
//        }
//
//        db.collection("Users").document(phoneNumber).get()
//            .addOnSuccessListener { doc ->
//                val role = doc.getString("role")
//                val name = doc.getString("fullName")
//                RoleTitle.text = "$role: $name"
//            }
//            .addOnFailureListener {
//                FirebaseAuth.getInstance().signOut()
//                startActivity(Intent(this, Start_Screen_Activity::class.java))
//                finish()
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

        db.collection("Users").document(phoneNumber).get()
            .addOnSuccessListener { doc ->
                val role = doc.getString("role")
                val name = doc.getString("fullName")
                RoleTitle.text = "$role: $name"
            }
            .addOnFailureListener {
                prefs.clearPreferences()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, Start_Screen_Activity::class.java))
                finish()
            }
    }

    // 🔄 Load all item prices from Spare_Parts_Prices
    private fun loadPriceList(onLoaded: () -> Unit) {
        db.collection("Spare_Parts_Prices")
            .get()
            .addOnSuccessListener { snap ->
                itemPriceMap.clear()
                for (doc in snap.documents) {
                    val code = doc.id
                    val price = doc.getString("price")?.toIntOrNull() ?: 0
                    itemPriceMap[code] = price
                }
                onLoaded()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load prices!", Toast.LENGTH_SHORT).show()
                onLoaded()
            }
    }

    // 🔄 Load existing bill data from Spare_Parts_final_bill and fill prices automatically
    private fun loadBillData() {
        sparePartsTable.removeViews(1, sparePartsTable.childCount - 4) // Keep header and total rows

        db.collection("Jobs").document("job.$jobId")
            .collection("Spare_Parts_Used")
            .document("Spare_Parts_final_bill")
            .get()
            .addOnSuccessListener { doc ->
                val partsList = doc.get("parts") as? List<Map<String, Any>> ?: emptyList()
                if (partsList.isEmpty()) {
                    addBillRow("", "", "1", "0")
                } else {
                    for (part in partsList) {
                        val code = part["itemCode"] as? String ?: ""
                        val name = part["itemName"] as? String ?: ""
                        val qty = (part["quantity"] as? Long)?.toString()
                            ?: (part["quantity"] as? Int)?.toString()
                            ?: (part["quantity"] as? String) ?: "1"
                        val cost = itemPriceMap[code]?.toString() ?: "0"
                        addBillRow(code, name, qty, cost)
                    }
                }
                calculateBill()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load bill data!", Toast.LENGTH_SHORT).show()
                addBillRow("", "", "1", "0")
                calculateBill()
            }
    }

    // ➕ Add a bill row dynamically
    private fun addBillRow(itemCode: String, itemName: String, qty: String, cost: String) {
        val row = TableRow(this)

        val codeView = createEditText(itemCode, 1.25f)
        val nameView = createEditText(itemName, 3.5f)
        val qtyView = createEditText(qty, 2f)
        val costView = createEditText(cost, 1.25f)
        val totalView = createEditText("0", 2f).apply { isEnabled = false }

        // 🧮 Watch for qty & cost changes to recalculate
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { calculateBill() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        qtyView.addTextChangedListener(watcher)
        costView.addTextChangedListener(watcher)

        row.addView(codeView)
        row.addView(nameView)
        row.addView(qtyView)
        row.addView(costView)
        row.addView(totalView)

        // 🗑️ Long press to delete row
        row.setOnLongClickListener {
            if (isEditMode && sparePartsTable.childCount > 5) {
                sparePartsTable.removeView(row)
                calculateBill()
            }
            true
        }

        // 🚫 Initially disable editing
        codeView.isEnabled = false
        nameView.isEnabled = false
        qtyView.isEnabled = false
        costView.isEnabled = false

        sparePartsTable.addView(row, sparePartsTable.childCount - 3)
    }

    private fun removeEmptyRows() {
        for (i in sparePartsTable.childCount - 4 downTo 1) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            val code = (row.getChildAt(0) as EditText).text.toString().trim()
            val name = (row.getChildAt(1) as EditText).text.toString().trim()
            val qty = (row.getChildAt(2) as EditText).text.toString().trim()
            val cost = (row.getChildAt(3) as EditText).text.toString().trim()
            if (code.isEmpty() && name.isEmpty() && (qty == "0" || qty.isEmpty()) && (cost == "0" || cost.isEmpty())) {
                sparePartsTable.removeView(row)
            }
        }
    }

    // 🖊️ Create dynamic EditText for table cells
    private fun createEditText(text: String, weight: Float): EditText {
        return EditText(this).apply {
            setText(text)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
            layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    // 🔄 Toggle edit mode for Mismatch logic
    private fun toggleEditMode(enabled: Boolean) {
        for (i in 1 until sparePartsTable.childCount - 3) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            for (j in 0 until 4) {
                row.getChildAt(j).isEnabled = enabled
            }
        }
    }

    // 🧮 Recalculate totals for bill
    private fun calculateBill() {
        var total = 0
        for (i in 1 until sparePartsTable.childCount - 3) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            val qty = row.getChildAt(2) as EditText
            val cost = row.getChildAt(3) as EditText
            val totalView = row.getChildAt(4) as EditText

            val qtyVal = qty.text.toString().toIntOrNull() ?: 0
            val costVal = cost.text.toString().toIntOrNull() ?: 0
            val totalItem = qtyVal * costVal
            totalView.setText(totalItem.toString())
            total += totalItem
        }

        tvTotalBill.text = total.toString()
        tvDiscount.text = discount.toString()
        tvFinalBill.text = (total - discount).toString()
    }

    // 💾 Save current bill updates to Firestore
    private fun saveBillUpdates() {
        val updates = mutableListOf<Map<String, Any>>()
        for (i in 1 until sparePartsTable.childCount - 3) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            val itemCode = (row.getChildAt(0) as EditText).text.toString()
            val itemName = (row.getChildAt(1) as EditText).text.toString()
            val qty = (row.getChildAt(2) as EditText).text.toString().toIntOrNull() ?: 1
            val cost = (row.getChildAt(3) as EditText).text.toString().toIntOrNull() ?: 0

            val item = mapOf(
                "itemCode" to itemCode,
                "itemName" to itemName,
                "quantity" to qty,
                "costPerItem" to cost
            )
            updates.add(item)
        }

        val detailedBillRef = db.collection("Jobs").document("job.$jobId").collection("detailedBill")
        detailedBillRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                detailedBillRef.document(doc.id).delete()
            }
            for (item in updates) {
                detailedBillRef.add(item)
            }
            Toast.makeText(this, "Bill updated in Firestore", Toast.LENGTH_SHORT).show()
        }
    }

    // 💰 Show discount dialog
    private fun showDiscountDialog() {
        val input = EditText(this).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Discount")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                discount = input.text.toString().toIntOrNull() ?: 0
                calculateBill()
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    // 🏁 Finalize bill: Save to Firestore & generate PDF in Downloads using Storage Access Framework
    private fun createBillPdf() {
        // --- Prepare PDF content ---
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textSize = 18f
        paint.textSize = 14f

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40
        canvas.drawText("NIHARIKA ENTERPRISES", 180f, y.toFloat(), titlePaint)
        y += 35
        canvas.drawText("Detailed Bill", 230f, y.toFloat(), titlePaint)
        y += 25
        canvas.drawText("Job ID: $jobId", 40f, y.toFloat(), paint)
        y += 25

        // Table header
        val header = listOf("Code", "Name", "Qty", "Cost", "Total")
        val xList = listOf(40, 110, 340, 400, 470)
        for (i in header.indices) {
            canvas.drawText(header[i], xList[i].toFloat(), y.toFloat(), paint)
        }
        y += 20
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), paint)
        y += 18

        // Table rows
        var totalSum = 0
        for (i in 1 until sparePartsTable.childCount - 3) {
            val row = sparePartsTable.getChildAt(i) as TableRow
            val itemCode = (row.getChildAt(0) as EditText).text.toString()
            val itemName = (row.getChildAt(1) as EditText).text.toString()
            val qty = (row.getChildAt(2) as EditText).text.toString()
            val cost = (row.getChildAt(3) as EditText).text.toString()
            val total = (row.getChildAt(4) as EditText).text.toString()
            totalSum += total.toIntOrNull() ?: 0

            val rowData = listOf(itemCode, itemName, qty, cost, total)
            for (j in rowData.indices) {
                canvas.drawText(rowData[j], xList[j].toFloat(), y.toFloat(), paint)
            }
            y += 18
            if (y > 800) break // avoid overflow
        }

        y += 10
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), paint)
        y += 25
        canvas.drawText("Total: $totalSum", 410f, y.toFloat(), paint)
        y += 20
        canvas.drawText("Discount: $discount", 410f, y.toFloat(), paint)
        y += 20
        canvas.drawText("Final Bill: ${totalSum - discount}", 410f, y.toFloat(), paint)
        pdfDocument.finishPage(page)

        // --- Save PDF using Storage Access Framework (Downloads) ---
        this.pdfDocumentToSave = pdfDocument
        val filename = "Detailed_Bill_${jobId}_${SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault()).format(Date())}.pdf"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        startActivityForResult(intent, CREATE_PDF_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_PDF_REQUEST_CODE && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null && pdfDocumentToSave != null) {
                try {
                    val out = contentResolver.openOutputStream(uri)
                    pdfDocumentToSave!!.writeTo(out)
                    out?.close()
                    Toast.makeText(this, "PDF saved to Downloads!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
                pdfDocumentToSave?.close()
                pdfDocumentToSave = null
            }
        }
    }
}
