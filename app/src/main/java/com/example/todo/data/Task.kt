package com.example.todo.data

import java.util.Date

data class Task(
    val id: String? = null,
    val title: String,
    val deadline: Date,
    val userId: String
) {
    constructor() : this(null, "", Date(), "")
}