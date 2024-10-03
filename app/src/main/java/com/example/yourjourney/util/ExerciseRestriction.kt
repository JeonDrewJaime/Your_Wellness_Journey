package com.example.yourjourney.util

val restrictedExercisesByDiseaseAndAge = mapOf(
    "Acute Lymphoblastic Leukemia" to mapOf(
        4..5 to listOf("vincristine", "aspirin"),
        2..6 to listOf("ibuprofen", "vincristine" )
    ),
    "diseaseB" to mapOf(
        3..5 to listOf("paracetamol")
    )
)
