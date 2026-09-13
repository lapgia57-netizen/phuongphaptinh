package com.multipay.pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.multipay.pro.databinding.ItemRecordBinding

class RecordAdapter(
    private val onDelete: (IncomeRecord) -> Unit
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    private val items = mutableListOf<IncomeRecord>()

    fun submitList(newItems: List<IncomeRecord>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class RecordViewHolder(private val binding: ItemRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IncomeRecord) {
            binding.textJobTitle.text = item.jobTitle.ifEmpty { "Công việc" }
            binding.textTime.text = Utils.formatDateVN(item.timeStr)
            binding.textDetail.text = item.detailText.ifEmpty { "—" }
            binding.textTotal.text = Utils.formatVND(item.total)

            if (item.note.isNotEmpty()) {
                binding.textNote.visibility = View.VISIBLE
                binding.textNote.text = item.note
            } else {
                binding.textNote.visibility = View.GONE
            }

            if (item.type == "MONTHLY") {
                binding.textTypeBadge.text = "Lương Tháng"
                binding.textTypeBadge.setBackgroundResource(R.drawable.bg_badge_blue)
                binding.textTypeBadge.setTextColor(
                    binding.root.context.getColor(R.color.brand_blue)
                )
            } else {
                binding.textTypeBadge.text = "Part-time"
                binding.textTypeBadge.setBackgroundResource(R.drawable.bg_badge_purple)
                binding.textTypeBadge.setTextColor(
                    binding.root.context.getColor(R.color.brand_purple)
                )
            }

            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }
}
