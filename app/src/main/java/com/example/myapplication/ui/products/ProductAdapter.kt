package com.example.myapplication.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.data.model.Product
import com.example.myapplication.databinding.ItemProductBinding

/**
 * Адаптер для отображения списка продуктов
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val products = mutableListOf<Product>()

    fun updateProducts(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProductClick(products[position])
                }
            }
        }

        fun bind(product: Product) {
            binding.textProductName.text = product.name
            binding.textProductBrand.text = product.brand ?: "Неизвестный бренд"
            
            // Загружаем изображение продукта, если доступно
            if (product.imageUrl != null) {
                Glide.with(binding.root.context)
                    .load(product.imageUrl)
                    .centerCrop()
                    .into(binding.imageProduct)
            } else {
                binding.imageProduct.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
} 