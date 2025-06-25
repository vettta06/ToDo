package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todo.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.jvm.java
import com.example.todo.adapters.TaskGroupAdapter
import com.example.todo.data.Task
import com.example.todo.databinding.DialogAddTaskBinding
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val taskMap = mutableMapOf<String, MutableList<Task>>()
    private lateinit var adapter: TaskGroupAdapter
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = TaskGroupAdapter(
            taskMap,
            onTaskClick = { task -> showEditTaskDialog(task) },
            onTaskDelete = { task -> deleteTask(task) } // Добавлен обработчик удаления
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener {
            showAddTaskDialog()
        }

        loadTasks()
        migrateOldTasks() // Добавьте миграцию старых задач
    }

    private fun migrateOldTasks() {
        val userId = auth.currentUser!!.uid
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val oldTasks = document.get("tasks") as? List<String> ?: emptyList()

                if (oldTasks.isNotEmpty()) {
                    // Добавить новые задачи с дедлайном "сегодня"
                    val batch = db.batch()
                    val now = Date()

                    oldTasks.forEach { title ->
                        val task = Task(
                            title = title,
                            deadline = now,
                            userId = userId
                        )
                        val ref = db.collection("tasks").document()
                        batch.set(ref, task)
                    }

                    // Удалить старые задачи
                    batch.update(
                        db.collection("users").document(userId),
                        "tasks", FieldValue.delete()
                    )

                    batch.commit().addOnSuccessListener {
                        Log.d("Migration", "Old tasks migrated successfully")
                        loadTasks() // Перезагрузить задачи после миграции
                    }
                }
            }
    }

    private fun loadTasks() {
        val userId = auth.currentUser!!.uid
        Log.d("LoadTasks", "Загрузка задач для пользователя: $userId")

        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreError", "Ошибка загрузки задач", error)
                    return@addSnapshotListener
                }

                Log.d("TasksLoaded", "Количество документов: ${snapshot?.size()}")
                taskMap.clear()
                snapshot?.documents?.forEach { doc ->
                    val task = doc.toObject(Task::class.java)!!.copy(id = doc.id)
                    Log.d("TaskDetails", "Загружена задача: $task")
                    val dateKey = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(task.deadline)

                    if (!taskMap.containsKey(dateKey)) {
                        taskMap[dateKey] = mutableListOf()
                    }
                    taskMap[dateKey]?.add(task)
                }

                // Сортировка по дате
                val sortedMap = taskMap.toSortedMap(compareBy<String> {
                    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it)
                })
                taskMap.clear()
                taskMap.putAll(sortedMap)
                adapter.updateData()
            }
    }

    private fun showAddTaskDialog() {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Добавить задачу")
            .setView(dialogBinding.root)
            .setPositiveButton("Добавить") { _, _ ->
                val title = dialogBinding.taskEditText.text.toString()
                val date = dialogBinding.datePicker.dayOfMonth
                val month = dialogBinding.datePicker.month
                val year = dialogBinding.datePicker.year

                val calendar = Calendar.getInstance().apply {
                    set(year, month, date)
                }

                if (title.isNotBlank()) {
                    addTask(Task(
                        title = title,
                        deadline = calendar.time,
                        userId = auth.currentUser!!.uid
                    ))
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun addTask(task: Task) {
        db.collection("tasks")
            .add(task)
            .addOnSuccessListener {
                Log.d("AddTask", "Задача успешно добавлена. userId: ${task.userId}")
                Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("AddTaskError", "Ошибка добавления задачи", e)
            }
    }


    private fun showEditTaskDialog(task: Task) {
        val dialogBinding = DialogAddTaskBinding.inflate(layoutInflater)
        dialogBinding.taskEditText.setText(task.title)

        val calendar = Calendar.getInstance().apply { time = task.deadline }
        dialogBinding.datePicker.updateDate(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        AlertDialog.Builder(this)
            .setTitle("Редактировать задачу")
            .setView(dialogBinding.root)
            .setPositiveButton("Сохранить") { _, _ ->
                val newTitle = dialogBinding.taskEditText.text.toString()
                val newDate = Calendar.getInstance().apply {
                    set(
                        dialogBinding.datePicker.year,
                        dialogBinding.datePicker.month,
                        dialogBinding.datePicker.dayOfMonth
                    )
                }.time

                updateTask(task.copy(title = newTitle, deadline = newDate))
            }
            .setNegativeButton("Удалить") { _, _ ->
                deleteTask(task)
            }
            .setNeutralButton("Отмена", null)
            .show()
    }

    private fun updateTask(task: Task) {
        if (task.id == null) return

        db.collection("tasks").document(task.id)
            .set(task)
            .addOnSuccessListener {
                Toast.makeText(this, "Задача обновлена", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteTask(task: Task) {
        if (task.id == null) return

        db.collection("tasks").document(task.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Задача удалена", Toast.LENGTH_SHORT).show()
            }
    }
}