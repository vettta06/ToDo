package com.example.todo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.databinding.ActivityMainBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlin.jvm.java
import com.example.todo.LoginActivity
import com.google.firebase.firestore.FieldValue

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val taskList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = auth.currentUser
        if (currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
            Toast.makeText(this, "Пользователь не зарегистрирован", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val userId = auth.currentUser?.uid
        userId?.let {
            db.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    val email = document.getString("email")
                    Toast.makeText(this, "Добро пожаловать, $email", Toast.LENGTH_SHORT).show()
                    //список задач
                    val tasks = document.get("tasks") as? List<String> ?: emptyList()
                    taskList.addAll(tasks)
                    adapter.notifyDataSetChanged()
                }
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, taskList)
        binding.listView.adapter = adapter

        binding.addButton.setOnClickListener {
            val task = binding.editText.text.toString()
            if (task.isNotBlank()) {
                taskList.add(task)
                adapter.notifyDataSetChanged()
                binding.editText.text.clear()

                //обновление пользователя
                val userId = auth.currentUser?.uid
                userId?.let{uid ->
                    db.collection("users").document(uid)
                        .update("tasks", FieldValue.arrayUnion(task))
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

        }

        binding.listView.setOnItemClickListener { _, _, position, _ ->
            val taskToRemove = taskList[position]
            taskList.removeAt(position)
            adapter.notifyDataSetChanged()

            val userId = auth.currentUser?.uid
            userId?.let { uid ->
                db.collection("users").document(uid)
                    .update("tasks", FieldValue.arrayRemove(taskToRemove))
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                        taskList.add(position, taskToRemove)
                        adapter.notifyDataSetChanged()
                    }
            }
        }

    }
}