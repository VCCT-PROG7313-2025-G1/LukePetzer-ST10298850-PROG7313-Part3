package com.example.lukepetzer_st10298850_prog7313_part3.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.ItemTransactionBinding
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val onReceiptClick: (String) -> Unit) :
    ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(transaction: Transaction) {
            binding.apply {
                tvTransactionType.text = transaction.type
                tvTransactionAmount.text = String.format("R%.2f", transaction.amount)
                tvTransactionCategory.text = transaction.category
                tvTransactionDate.text = dateFormat.format(transaction.date)
                tvTransactionDescription.text = transaction.description ?: "No description"

                val context = binding.root.context
                val color = if (transaction.type == "Income") {
                    context.getColor(android.R.color.holo_green_dark)
                } else {
                    context.getColor(android.R.color.holo_red_dark)
                }
                tvTransactionAmount.setTextColor(color)

                if (!transaction.receiptUri.isNullOrEmpty()) {
                    ivReceiptIndicator.visibility = View.VISIBLE
                    ivReceiptIndicator.setOnClickListener {
                        onReceiptClick(transaction.receiptUri)
                    }
                } else {
                    ivReceiptIndicator.visibility = View.GONE
                }
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}