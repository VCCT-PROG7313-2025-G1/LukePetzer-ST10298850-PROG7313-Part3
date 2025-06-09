package com.example.lukepetzer_st10298850_prog7313_part3.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lukepetzer_st10298850_prog7313_part3.R
import com.example.lukepetzer_st10298850_prog7313_part3.data.Category
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.DialogAddCategoryBinding
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.FragmentTransactionCategoriesBinding
import com.example.lukepetzer_st10298850_prog7313_part3.databinding.ItemCategoryBinding
import com.example.lukepetzer_st10298850_prog7313_part3.repositories.CategoryRepository
import com.example.lukepetzer_st10298850_prog7313_part3.viewmodels.TransactionCategoriesViewModel
import com.google.firebase.auth.FirebaseAuth
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
    private val viewModel: TransactionCategoriesViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter

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

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Get current user ID from Firebase Auth
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
        val userId = sharedPref.getString("USER_ID", "")
        if (userId == "" || userId == null) {
            findNavController().navigate(R.id.action_transactionCategoriesFragment_to_loginFragment)
            return
        }

        viewModel.loadCategories(userId)
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(emptyList())
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
        }
    }

    private fun setupObservers() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryAdapter.updateCategories(categories)
            updateTotalBudget(categories)
        }
    }

    private fun updateTotalBudget(categories: List<Category>) {
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

            val sharedPref = requireActivity().getPreferences(Context.MODE_PRIVATE)
            val userId = sharedPref.getString("USER_ID", "")
//            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            if (categoryName.isNotEmpty() && budgetAmount != null && userId != null) {
                val newCategory = Category(
                    name = categoryName,
                    userId = userId,
                    budgetAmount = budgetAmount
                )
                viewModel.addCategory(newCategory)
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

    inner class CategoryAdapter(private var categories: List<Category>) :
        RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

        fun updateCategories(newCategories: List<Category>) {
            categories = newCategories
            notifyDataSetChanged()
        }

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