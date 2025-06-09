package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentAddTransactionBinding
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.AddTransactionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddTransactionViewModel

    private var receiptUri: Uri? = null
    private var receiptPath: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            receiptUri?.let {
                binding.ivReceiptPreview.setImageURI(it)
                binding.ivReceiptPreview.visibility = View.VISIBLE
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.ivReceiptPreview.setImageURI(uri)
            binding.ivReceiptPreview.visibility = View.VISIBLE
            receiptUri = uri
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[AddTransactionViewModel::class.java]

        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }

        binding.etDate.setOnClickListener { openDatePicker() }
        binding.btnAddReceipt.setOnClickListener { openImageOptions() }
        binding.btnAddTransaction.setOnClickListener { submitTransaction() }

        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        binding.etDate.setText(dateStr)

        viewModel.loadCategories()
    }

    private fun openDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val selected = Calendar.getInstance()
            selected.set(y, m, d)
            binding.etDate.setText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selected.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun openImageOptions() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Attach Receipt")
            .setItems(options) { _, i ->
                when (i) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }.show()
    }

    private fun launchCamera() {
        val file = File.createTempFile("IMG_", ".jpg", requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        receiptPath = file.absolutePath
        receiptUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, receiptUri)
        cameraLauncher.launch(intent)
    }

    private fun submitTransaction() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val category = binding.spinnerCategory.selectedItem?.toString()
        val type = if (binding.toggleGroup.checkedButtonId == binding.btnIncome.id) "Income" else "Expense"
        val dateText = binding.etDate.text.toString()
        val description = binding.etDescription.text.toString().takeIf { it.isNotBlank() }
        val userId = auth.currentUser?.uid ?: return

        if (amount == null || category.isNullOrBlank()) {
            Toast.makeText(context, "Enter valid amount and select category", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = hashMapOf(
            "amount" to amount,
            "category" to category,
            "type" to type,
            "date" to dateText,
            "description" to description,
            "timestamp" to System.currentTimeMillis()
        )

        if (receiptUri != null) {
            val receiptRef = storage.reference.child("receipts/${userId}/${UUID.randomUUID()}.jpg")
            receiptRef.putFile(receiptUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    receiptRef.downloadUrl
                }.addOnSuccessListener { downloadUri ->
                    transaction["receiptUrl"] = downloadUri.toString()
                    saveTransaction(userId, transaction)
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to upload receipt", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveTransaction(userId, transaction)
        }
    }

    private fun saveTransaction(userId: String, transaction: Map<String, Any?>) {
        db.collection("users").document(userId)
            .collection("transactions")
            .add(transaction)
            .addOnSuccessListener {
                Toast.makeText(context, "Transaction added", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save transaction", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}