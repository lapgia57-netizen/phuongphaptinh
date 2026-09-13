package com.multipay.pro

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.multipay.pro.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: RecordAdapter

    private var activeTab = "MONTHLY" // MONTHLY | PARTTIME
    private var activeUnit = "HOURLY" // HOURLY | PRODUCT

    private var selectedMonth = "" // yyyy-MM for monthly time input
    private var selectedDate = ""  // yyyy-MM-dd for parttime time input

    private val allRecords = mutableListOf<IncomeRecord>()
    private var valueEventListener: ValueEventListener? = null
    private lateinit var userRecordsRef: com.google.firebase.database.DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }
        binding.textUserEmail.text = user.email

        setupRecyclerView()
        setupTabs()
        setupUnitDropdown()
        setupTimePickers()
        setupFilters()
        setupSaveButton()
        setupSwipeRefresh()

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        userRecordsRef = database.getReference("users/${user.uid}/income_records")
        listenToData()

        // Default values
        setDefaultMonth()
        setDefaultDate()
    }

    override fun onDestroy() {
        super.onDestroy()
        valueEventListener?.let { userRecordsRef.removeEventListener(it) }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    // ---------- Setup UI ----------

    private fun setupRecyclerView() {
        adapter = RecordAdapter { record -> confirmDelete(record) }
        binding.recyclerRecords.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecords.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                activeTab = if (tab.position == 0) "MONTHLY" else "PARTTIME"
                updateFormVisibility()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        updateFormVisibility()
    }

    private fun updateFormVisibility() {
        val isMonthly = activeTab == "MONTHLY"
        binding.layoutTimeMonthly.visibility = if (isMonthly) View.VISIBLE else View.GONE
        binding.layoutTimeParttime.visibility = if (isMonthly) View.GONE else View.VISIBLE
        binding.groupMonthly.visibility = if (isMonthly) View.VISIBLE else View.GONE
        binding.groupParttime.visibility = if (isMonthly) View.GONE else View.VISIBLE
    }

    private fun setupUnitDropdown() {
        val units = arrayOf("⏱️ Tính Theo Số Giờ Làm Việc", "📦 Tính Theo Số Lượng Sản Phẩm/Đơn")
        val adapterUnit = ArrayAdapter(this, android.R.layout.simple_list_item_1, units)
        binding.inputUnit.setAdapter(adapterUnit)
        binding.inputUnit.setText(units[0], false)

        binding.inputUnit.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                activeUnit = "HOURLY"
                binding.layoutQty.hint = "Số Giờ Làm"
                binding.layoutRate.hint = "Đơn Giá / Giờ (VNĐ)"
            } else {
                activeUnit = "PRODUCT"
                binding.layoutQty.hint = "Số Sản Phẩm / Đơn"
                binding.layoutRate.hint = "Đơn Giá / Sản Phẩm (VNĐ)"
            }
        }
    }

    private fun setupTimePickers() {
        binding.inputTimeMonthly.setOnClickListener { showMonthPicker() }
        binding.inputTimeParttime.setOnClickListener { showDatePicker() }
    }

    private fun setDefaultMonth() {
        val cal = Calendar.getInstance()
        selectedMonth = "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        binding.inputTimeMonthly.setText("Tháng ${cal.get(Calendar.MONTH) + 1}/${cal.get(Calendar.YEAR)}")
    }

    private fun setDefaultDate() {
        val cal = Calendar.getInstance()
        selectedDate = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
        )
        binding.inputTimeParttime.setText(
            "%02d/%02d/%04d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
        )
    }

    private fun showMonthPicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, _ ->
                selectedMonth = "%04d-%02d".format(year, month + 1)
                binding.inputTimeMonthly.setText("Tháng ${month + 1}/$year")
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1
        )
        // Ẩn phần chọn ngày không hoàn toàn khả dụng trên mọi thiết bị; người dùng chỉ cần chọn tháng/năm
        dialog.show()
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                binding.inputTimeParttime.setText("%02d/%02d/%04d".format(day, month + 1, year))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    private fun setupFilters() {
        val types = arrayOf("Tất cả hình thức", "Chỉ Lương Cố Định", "Chỉ Part-time / Theo giờ")
        binding.spinnerFilterType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerFilterType.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                renderRecords()
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        refreshMonthFilterOptions()
    }

    private fun refreshMonthFilterOptions() {
        val months = allRecords.map { it.monthKey }.filter { it.isNotEmpty() }.toSortedSet().sortedDescending()
        val options = mutableListOf("Tất cả các tháng")
        options.addAll(months.map { "Tháng $it" })
        binding.spinnerFilterMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        binding.spinnerFilterMonth.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                renderRecords()
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            // Realtime DB đã tự đồng bộ; dừng animation sau khoảng ngắn
            binding.swipeRefresh.postDelayed({ binding.swipeRefresh.isRefreshing = false }, 600)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener { saveRecord() }
    }

    // ---------- Firebase ----------

    private fun listenToData() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allRecords.clear()
                for (child in snapshot.children) {
                    val record = child.getValue(IncomeRecord::class.java) ?: continue
                    record.id = child.key.orEmpty()
                    allRecords.add(record)
                }
                refreshMonthFilterOptions()
                renderRecords()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Lỗi kết nối dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        userRecordsRef.addValueEventListener(valueEventListener!!)
    }

    private fun saveRecord() {
        val jobTitle = binding.inputJobTitle.text?.toString()?.trim().orEmpty()
        val note = binding.inputNote.text?.toString()?.trim().orEmpty()

        if (jobTitle.isEmpty()) {
            binding.inputJobTitle.error = "Vui lòng nhập tên công việc"
            return
        }

        val record = IncomeRecord(
            jobTitle = jobTitle,
            type = activeTab,
            note = note,
            createdAt = System.currentTimeMillis()
        )

        if (activeTab == "MONTHLY") {
            val base = binding.inputMonthlyBase.text?.toString()?.toDoubleOrNull() ?: 0.0
            val extra = binding.inputMonthlyExtra.text?.toString()?.toDoubleOrNull() ?: 0.0

            record.timeStr = selectedMonth
            record.monthKey = selectedMonth
            record.baseIncome = base
            record.extraIncome = extra
            record.total = base + extra
            record.detailText = if (extra > 0)
                "Lương gốc: ${Utils.formatVND(base)} + Thưởng/OT: ${Utils.formatVND(extra)}"
            else "Lương cố định"
        } else {
            val qty = binding.inputQty.text?.toString()?.toDoubleOrNull() ?: 0.0
            val rate = binding.inputRate.text?.toString()?.toDoubleOrNull() ?: 0.0
            val total = qty * rate

            record.timeStr = selectedDate
            record.monthKey = selectedDate.substring(0, 7)
            record.unit = activeUnit
            record.qty = qty
            record.rate = rate
            record.total = total
            record.detailText = if (activeUnit == "HOURLY")
                "$qty giờ x ${Utils.formatVND(rate)}/h"
            else "$qty SP x ${Utils.formatVND(rate)}/sp"
        }

        userRecordsRef.push().setValue(record.toMap())
            .addOnSuccessListener {
                Toast.makeText(this, "Đã ghi nhận thu nhập thành công!", Toast.LENGTH_SHORT).show()
                clearInputFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi lưu dữ liệu: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearInputFields() {
        binding.inputNote.setText("")
        if (activeTab == "MONTHLY") {
            binding.inputMonthlyBase.setText("")
            binding.inputMonthlyExtra.setText("")
        } else {
            binding.inputQty.setText("")
            binding.inputRate.setText("")
        }
    }

    private fun confirmDelete(record: IncomeRecord) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa bản ghi")
            .setMessage("Bạn có chắc chắn muốn xóa bản ghi này?")
            .setPositiveButton("Xóa") { _, _ ->
                userRecordsRef.child(record.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã xóa bản ghi.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Không thể xóa: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ---------- Rendering ----------

    private fun renderRecords() {
        val typePos = binding.spinnerFilterType.selectedItemPosition
        val selectedType = when (typePos) {
            1 -> "MONTHLY"
            2 -> "PARTTIME"
            else -> "ALL"
        }

        val monthPos = binding.spinnerFilterMonth.selectedItemPosition
        val monthAdapter = binding.spinnerFilterMonth.adapter as? ArrayAdapter<*>
        val selectedMonthLabel = if (monthAdapter != null && monthPos >= 0 && monthPos < monthAdapter.count) {
            monthAdapter.getItem(monthPos).toString()
        } else "Tất cả các tháng"
        val selectedMonthKey = if (selectedMonthLabel == "Tất cả các tháng") "ALL" else selectedMonthLabel.removePrefix("Tháng ").trim()

        val filtered = allRecords.filter { item ->
            val matchType = selectedType == "ALL" || item.type == selectedType
            val matchMonth = selectedMonthKey == "ALL" || item.monthKey == selectedMonthKey
            matchType && matchMonth
        }.sortedWith(compareByDescending<IncomeRecord> { it.timeStr }.thenByDescending { it.createdAt })

        adapter.submitList(filtered)
        binding.textEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerRecords.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

        var sumMonthly = 0.0
        var sumParttime = 0.0
        allRecords.forEach {
            if (it.type == "MONTHLY") sumMonthly += it.total else sumParttime += it.total
        }
        binding.statMonthly.text = Utils.formatVND(sumMonthly)
        binding.statParttime.text = Utils.formatVND(sumParttime)
        binding.statTotal.text = Utils.formatVND(sumMonthly + sumParttime)
    }
}
