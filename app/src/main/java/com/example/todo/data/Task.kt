package com.example.todo.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Task(
    val id: String? = null,
    val title: String,
    val deadline: Date,
    val userId: String
) {
    constructor() : this(null, "", Date(), "")
}