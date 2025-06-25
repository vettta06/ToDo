package com.example.todo.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.todo.databinding.ItemTaskBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue


class TaskAdapter(context: Context, private val taskList: MutableList<String>) :
    ArrayAdapter<String>(context, 0, taskList) {

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding: ItemTaskBinding

        if (convertView == null) {
            binding = ItemTaskBinding.inflate(LayoutInflater.from(context), parent, false)
        } else {
            binding = convertView.tag as ItemTaskBinding
        }

        val task = taskList[position]

        binding.taskText.text = task
        binding.checkBox.isChecked = false

        binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val userId = auth.currentUser?.uid
                userId?.let { uid ->
                    db.collection("users").document(uid)
                        .update("tasks", FieldValue.arrayRemove(task))
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.checkBox.isChecked = false
                        }
                }

                taskList.removeAt(position)
                notifyDataSetChanged()
            }
        }

        binding.root.setOnLongClickListener { _ ->
            showEditTaskDialog(task, position, binding)
            true
        }

        binding.root.tag = binding
        return binding.root
    }

    private fun showEditTaskDialog(oldTask: String, position: Int, binding: ItemTaskBinding) {
        val context = binding.root.context
        val editText = EditText(context)
        editText.setText(oldTask)

        AlertDialog.Builder(context)
            .setTitle("Редактировать задачу")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val newTask = editText.text.toString().trim()
                if (newTask.isNotBlank()) {
                    val userId = auth.currentUser?.uid
                    userId?.let { uid ->
                        db.collection("users").document(uid)
                            .update("tasks", FieldValue.arrayRemove(oldTask))
                            .addOnSuccessListener {
                                db.collection("users").document(uid)
                                    .update("tasks", FieldValue.arrayUnion(newTask))
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Ошибка обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Ошибка удаления старой задачи: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    taskList[position] = newTask
                    binding.taskText.text = newTask
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}