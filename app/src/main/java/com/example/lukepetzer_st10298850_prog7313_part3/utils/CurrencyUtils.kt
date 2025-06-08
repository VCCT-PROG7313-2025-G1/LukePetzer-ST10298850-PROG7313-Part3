// File: CurrencyUtils.kt
package com.example.lukepetzer_st10298850_prog7313_part3.utils

import java.text.NumberFormat
import java.util.*

fun Double.toCurrency(): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return format.format(this)
}