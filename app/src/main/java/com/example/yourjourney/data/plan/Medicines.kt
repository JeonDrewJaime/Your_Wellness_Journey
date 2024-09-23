package com.example.yourjourney.data.plan

data class Medicines(
    val name: String,
    val dosage: String,
    val imageUrl: String,
    val isDone: Boolean = false // Default to false if not marked as done
)