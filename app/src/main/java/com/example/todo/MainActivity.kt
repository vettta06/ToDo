package com.example.todo

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.todo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val taskList = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Устанавливаем только binding.root

        // Настройка edge-to-edge
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Настройка адаптера
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, taskList)
        binding.listView.adapter = adapter

        // Добавление задачи
        binding.addButton.setOnClickListener {
            val task = binding.editText.text.toString()
            if (task.isNotBlank()) {
                taskList.add(task)
                adapter.notifyDataSetChanged()
                binding.editText.text.clear()
            }
        }

        // Удаление задачи
        binding.listView.setOnItemClickListener { _, _, position, _ ->
            taskList.removeAt(position)
            adapter.notifyDataSetChanged()
        }
    }
}