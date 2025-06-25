package com.example.todo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todo.data.Task
import com.example.todo.databinding.ItemHeaderBinding
import com.example.todo.databinding.ItemTaskBinding
import com.google.api.JwtLocation.InCase.HEADER
import java.text.SimpleDateFormat
import java.util.Locale

class TaskGroupAdapter(
    private val taskMap: MutableMap<String, MutableList<Task>>,
    private val onTaskClick: (Task) -> Unit,
    private val onTaskDelete: (Task) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val HEADER = 0
    private val ITEM = 1
    private var keys = taskMap.keys.sortedBy {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it)
    }

    fun updateData() {
        keys = taskMap.keys.sortedBy {
            SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).parse(it)
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        var pos = position
        for (key in keys) {
            if (pos == 0) return HEADER
            pos--
            val tasks = taskMap[key]!!
            if (pos < tasks.size) return ITEM
            pos -= tasks.size
        }
        return HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == HEADER) {
            HeaderViewHolder(ItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            TaskViewHolder(ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false), onTaskClick, onTaskDelete)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var pos = position
        for (key in keys) {
            if (pos == 0) {
                (holder as HeaderViewHolder).bind(key)
                return
            }
            pos--

            val tasks = taskMap[key]!!
            if (pos < tasks.size) {
                (holder as TaskViewHolder).bind(tasks[pos])
                return
            }
            pos -= tasks.size
        }
    }

    override fun getItemCount(): Int {
        var count = keys.size
        taskMap.values.forEach { tasks -> count += tasks.size }
        return count
    }

    class HeaderViewHolder(private val binding: ItemHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(date: String) {
            binding.dateText.text = date
        }
    }

    class TaskViewHolder(
        private val binding: ItemTaskBinding,
        private val onTaskClick: (Task) -> Unit,
        private val onTaskDelete: (Task) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.taskText.text = task.title

            binding.checkBox.setOnCheckedChangeListener(null)
            binding.checkBox.isChecked = false
            binding.checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    onTaskDelete(task)
                }
            }

            binding.root.setOnClickListener {
                onTaskClick(task)
            }
        }
    }
}