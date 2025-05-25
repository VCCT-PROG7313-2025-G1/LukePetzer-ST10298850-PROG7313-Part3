package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.data.AppDatabase
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.DialogAddCategoryBinding
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentTransactionCategoriesBinding
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.ItemCategoryBinding
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TransactionCategoriesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TransactionCategoriesFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentTransactionCategoriesBinding? = null
    private val binding get() = _binding!!
    private lateinit var categoryRepository: CategoryRepository
    private var currentUserId: Long = -1 // Default to an invalid ID
    private lateinit var categoryAdapter: CategoryAdapter
    private val categories = mutableListOf<Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        categoryRepository = CategoryRepository(database.categoryDao())

        // Retrieve the user ID from SharedPreferences
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        currentUserId = sharedPref.getLong("USER_ID", -1)

        if (currentUserId == -1L) {
            // No user is logged in, handle this case (e.g., navigate to login screen)
            findNavController().navigate(R.id.action_transactionCategoriesFragment_to_loginFragment)
            return
        }

        setupRecyclerView()
        loadCategories()
        setupUI()
        setupListeners()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(categories)
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val loadedCategories = categoryRepository.getCategoriesForUser(currentUserId)
            categories.clear()
            categories.addAll(loadedCategories)
            categoryAdapter.notifyDataSetChanged()
            updateTotalBudget()
        }
    }

    private fun updateTotalBudget() {
        val totalBudget = categories.sumOf { it.budgetAmount }
        binding.tvTotalBudgetAmount.text = String.format("R%.2f", totalBudget)
    }

    private fun setupUI() {
        // TODO: Load and display categories from the database
        // For now, we'll just show a placeholder total budget
        binding.tvTotalBudgetAmount.text = "R2,500"
    }

    private fun setupListeners() {
        binding.fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSaveChanges.setOnClickListener {
            val categoryName = dialogBinding.etCategoryName.text.toString()
            val budgetAmount = dialogBinding.etBudgetAmount.text.toString().toDoubleOrNull()

            if (categoryName.isNotEmpty() && budgetAmount != null) {
                lifecycleScope.launch {
                    val newCategory = Category(
                        name = categoryName,
                        userId = currentUserId,
                        budgetAmount = budgetAmount
                    )
                    val categoryId = categoryRepository.addCategory(newCategory)
                    if (categoryId > 0) {
                        Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
                        loadCategories() // Reload categories to update the UI
                    } else {
                        Toast.makeText(context, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class CategoryAdapter(private val categories: List<Category>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        inner class CategoryViewHolder(private val binding: ItemCategoryBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(category: Category) {
                binding.tvCategoryName.text = category.name
                binding.tvBudgetInfo.text = "Budget: R${category.budgetAmount} Spent: R0 Remaining: R${category.budgetAmount}"
                // TODO: Implement edit and delete functionality
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
            val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CategoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
            holder.bind(categories[position])
        }

        override fun getItemCount() = categories.size
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment TransactionCategoriesFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TransactionCategoriesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}