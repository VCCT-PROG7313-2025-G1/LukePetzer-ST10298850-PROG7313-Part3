package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.data.Transaction
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentAddTransactionBinding
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.AddTransactionViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*



class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private lateinit var viewModel: AddTransactionViewModel
    private var currentUserId: Long = -1
    private var categories: List<Category> = emptyList()

    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var currentReceiptPath: String? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            photoUri?.let { uri ->
                binding.ivReceiptPreview.setImageURI(uri)
                binding.ivReceiptPreview.visibility = View.VISIBLE
                currentReceiptPath = currentPhotoPath
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Copy the image to local storage
                val localFile = createImageFile()
                context?.contentResolver?.openInputStream(uri)?.use { input ->
                    localFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                binding.ivReceiptPreview.setImageURI(Uri.fromFile(localFile))
                binding.ivReceiptPreview.visibility = View.VISIBLE
                currentReceiptPath = localFile.absolutePath
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(this)[AddTransactionViewModel::class.java]

        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("USER_ID", -1)

        if (currentUserId == -1L) {
            Toast.makeText(context, "Please log in to add a transaction", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.action_addTransactionFragment_to_loginFragment)
            return
        }

        setupViews()
        setupListeners()
        observeCategories()
        viewModel.loadCategories(currentUserId)
    }

    private fun observeCategories() {
        viewModel.categories.observe(viewLifecycleOwner) { categoryList ->
            categories = categoryList
            val categoryNames = categoryList.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategory.adapter = adapter
        }
    }

    private fun setupViews() {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        binding.etDate.setText(sdf.format(Date()))
    }

    private fun setupListeners() {
        binding.btnAddTransaction.setOnClickListener {
            addTransaction()
        }

        binding.btnAddReceipt.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.etDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun addTransaction() {
        val type = if (binding.toggleGroup.checkedButtonId == R.id.btnIncome) "Income" else "Expense"
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val categoryPosition = binding.spinnerCategory.selectedItemPosition
        val category = categories.getOrNull(categoryPosition)?.name ?: return
        val dateString = binding.etDate.text.toString()
        val description = binding.etDescription.text.toString()

        if (amount == null) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        // Convert String to Date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = try {
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid date format", Toast.LENGTH_SHORT).show()
            return
        }

        val transaction = Transaction(
            userId = currentUserId,
            type = type,
            amount = amount,
            category = category,
            date = date, // Now passing a Date object
            description = description.ifEmpty { null },
            receiptUri = currentReceiptPath
        )

        lifecycleScope.launch {
            try {
                database.transactionDao().insertTransaction(transaction)
                Toast.makeText(context, "Transaction added successfully", Toast.LENGTH_SHORT).show()

                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_add, true)
                    .build()
                findNavController().navigate(R.id.navigation_home, null, navOptions)
            } catch (e: Exception) {
                Toast.makeText(context, "Error adding transaction: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.etDate.setText(dateFormat.format(selectedDate.time))
        }, year, month, day).show()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Photo!")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> takePhoto()
                options[item] == "Choose from Gallery" -> chooseFromGallery()
                options[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun takePhoto() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(context, "Error creating image file", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            val photoURI = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().applicationContext.packageName}.provider",
                it
            )
            photoUri = photoURI
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }
            takePictureLauncher.launch(intent)
        }
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
            currentReceiptPath = absolutePath
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}