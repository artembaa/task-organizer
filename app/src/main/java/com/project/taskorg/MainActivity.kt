package com.project.taskorg

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

private const val TASKLIST_TYPE_TODO = 0
private const val TASKLIST_TYPE_DOING = 1
private const val TASKLIST_TYPE_DONE = 2
private const val NUM_TASKLIST_PAGES = 3
private const val ARG_TASKLIST_TYPE = "tasklist_type"
private const val KEY_TODO_JSON = "todo_json"
private const val KEY_DOING_JSON = "doing_json"
private const val KEY_DONE_JSON = "done_json"

class MainActivity : AppCompatActivity(), TasklistFragment.Callbacks {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var taskViewModel: TaskViewModel

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
        R.id.action_new_task -> {
            val currentTasklistType = viewPager.currentItem

            val currentTasklist = when(currentTasklistType){
                TASKLIST_TYPE_TODO -> taskViewModel.todoTaskList
                TASKLIST_TYPE_DOING -> taskViewModel.doingTaskList
                TASKLIST_TYPE_DONE -> taskViewModel.doneTaskList
                else -> throw Exception("Unknown tasklist type")
            }
            val currentFragment = (supportFragmentManager.fragments[currentTasklistType] as TasklistFragment)
            addTaskToViewModel(Task(), currentTasklistType)
            currentFragment.taskRecyclerView.scrollToPosition(currentTasklist.size - 1)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        readSharedPrefsToViewModel()

        viewPager = findViewById(R.id.view_pager)
        val viewPagerAdapter = TasklistFragmentStateAdapter(this)
        viewPager.apply {
            adapter = viewPagerAdapter
            offscreenPageLimit = 2
        }

        tabLayout = findViewById(R.id.tab_layout)
        TabLayoutMediator(tabLayout, viewPager) {tab, position ->
            tab.text = when(position){
                0 -> getString(R.string.tab_label_todo)
                1 -> getString(R.string.tab_label_doing)
                else -> getString(R.string.tab_label_done)
            }
        }.attach()
    }

    override fun onStop() {
        super.onStop()
        writeViewModelToSharedPrefs()
    }

    private inner class TasklistFragmentStateAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa){
        override fun createFragment(position: Int): Fragment {
            val tasklistFragmentArguments = Bundle().apply{
                putInt(ARG_TASKLIST_TYPE, position)
            }
            return TasklistFragment().apply{
                arguments = tasklistFragmentArguments
            }
        }

        override fun getItemCount(): Int = NUM_TASKLIST_PAGES
    }

    //TasklistFragment.Callbacks

    override fun addTaskToViewModel(task: Task, destinationTasklistType: Int) {
        val destinationFragment = (supportFragmentManager.fragments[destinationTasklistType] as TasklistFragment)
        val taskList = getTaskListFromViewModel(destinationTasklistType)
        taskList.add(task)

        destinationFragment.taskRecyclerView.adapter?.notifyItemInserted(taskList.size)
    }

    override fun getTaskListFromViewModel(tasklistType: Int): LinkedList<Task> =
        when(tasklistType){
            TASKLIST_TYPE_TODO -> taskViewModel.todoTaskList
            TASKLIST_TYPE_DOING -> taskViewModel.doingTaskList
            TASKLIST_TYPE_DONE -> taskViewModel.doneTaskList
            else -> throw Exception("Unknown tasklist type") }

    override fun deleteTaskFromViewModel(tasklistType: Int, adapterPosition: Int) {
        val tasklistFragment = (supportFragmentManager.fragments[tasklistType] as TasklistFragment)
        getTaskListFromViewModel(tasklistType).removeAt(adapterPosition)
        tasklistFragment.taskRecyclerView.adapter?.notifyItemRemoved(adapterPosition)
    }

    private fun writeViewModelToSharedPrefs(){
        val gson = Gson()

        val todoJSON = gson.toJson(taskViewModel.todoTaskList)
        val doingJSON = gson.toJson(taskViewModel.doingTaskList)
        val doneJSON = gson.toJson(taskViewModel.doneTaskList)

        getPreferences(MODE_PRIVATE).edit().apply {
            putString(KEY_TODO_JSON, todoJSON)
            putString(KEY_DOING_JSON, doingJSON)
            putString(KEY_DONE_JSON, doneJSON)
        }.apply()
    }

    private fun readSharedPrefsToViewModel(){
        val gson = Gson()
        val sharedPrefs = getPreferences(MODE_PRIVATE)

        val todoJSON = sharedPrefs.getString(KEY_TODO_JSON, "[]")
        val doingJSON = sharedPrefs.getString(KEY_DOING_JSON, "[]")
        val doneJSON = sharedPrefs.getString(KEY_DONE_JSON, "[]")

        val type = object: TypeToken<LinkedList<Task>>() {}.type
        taskViewModel.todoTaskList = gson.fromJson(todoJSON, type)
        taskViewModel.doingTaskList = gson.fromJson(doingJSON, type)
        taskViewModel.doneTaskList = gson.fromJson(doneJSON, type)
    }
}

