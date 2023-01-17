package com.project.taskorg

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

private const val ARG_TASKLIST_TYPE = "tasklist_type"
private const val TASKLIST_TYPE_TODO = 0
private const val TASKLIST_TYPE_DOING = 1
private const val TASKLIST_TYPE_DONE = 2

class TasklistFragment : Fragment() {
    private lateinit var visibleColorPaletteViewList : List<View>
    lateinit var taskRecyclerView: RecyclerView

    private var tasklistType : Int = -1
    private var adapter : TaskViewAdapter? = TaskViewAdapter(LinkedList<Task>())
    private var colorPaletteIsVisible : Boolean = false
    private var callbacks: Callbacks? = null

    interface Callbacks {
        fun addTaskToViewModel(task: Task, destinationTasklistType: Int)
        fun deleteTaskFromViewModel(tasklistType: Int, adapterPosition: Int)
        fun getTaskListFromViewModel(tasklistType: Int) : LinkedList<Task>
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private val itemTouchHelper by lazy {
        val taskItemTouchCallback = object : ItemTouchHelper.SimpleCallback(UP or DOWN, 0){
            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {
                val adapter = recyclerView.adapter as TaskViewAdapter
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition

                adapter.moveTaskView(from, to)
                adapter.notifyItemMoved(from, to)

                return true
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)

                if (actionState == ACTION_STATE_DRAG)
                    viewHolder?.itemView?.alpha = 0.7f
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)

                viewHolder.itemView.alpha = 1.0f
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { /* Специально не реализовано. */ }
        }
        ItemTouchHelper(taskItemTouchCallback)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tasklistType = arguments?.getInt(ARG_TASKLIST_TYPE) as Int
    }

    override fun onCreateView (
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.tasklist_fragment_layout, container,false)
        taskRecyclerView = view.findViewById(R.id.task_recycler_view) as RecyclerView
        taskRecyclerView.layoutManager = LinearLayoutManager(context)
        itemTouchHelper.attachToRecyclerView(taskRecyclerView)
        updateInterface()
        return view
    }

    private fun updateInterface() {
        val tasks = callbacks!!.getTaskListFromViewModel(tasklistType)
        adapter = TaskViewAdapter(tasks)
        taskRecyclerView.adapter = adapter
    }

    private inner class TaskViewHolder(view: View): RecyclerView.ViewHolder(view) {
        lateinit var task: Task

        val taskEditText : EditText = view.findViewById(R.id.task_edit_text)
        val taskLayout : ConstraintLayout = view.findViewById(R.id.task_layout)

        val deleteButton : ImageButton = view.findViewById(R.id.btn_delete)
        val colorButton : ImageButton = view.findViewById(R.id.btn_color)
        val moveButton : ImageButton = view.findViewById(R.id.btn_move)
        val dragButton : ImageButton = view.findViewById(R.id.btn_drag)

        val colorPickerButton1 : ImageButton = view.findViewById(R.id.color_picker_1)
        val colorPickerButton2 : ImageButton = view.findViewById(R.id.color_picker_2)
        val colorPickerButton3 : ImageButton = view.findViewById(R.id.color_picker_3)
        val colorPickerButton4 : ImageButton = view.findViewById(R.id.color_picker_4)
        val colorPaletteBackground : ImageView = view.findViewById(R.id.color_palette_background)

        val deleteDialog : MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.delete_dialog_title)
                .setMessage(R.string.delete_dialog_text)
                .setNegativeButton(R.string.delete_dialog_no_button_label)
                { _, _ -> }

        // не менять порядок!!!
        val colorPaletteViews : List<View> = listOf (
            colorPaletteBackground,
            colorPickerButton3,
            colorPickerButton4,
            colorPickerButton2,
            colorPickerButton1
        )

        fun setColorPaletteVisibility(visibilityStasus : Int, colorPaletteViewList : List<View> = colorPaletteViews){
            for(view in colorPaletteViewList)
                view.visibility = visibilityStasus
        }

        fun bindTaskDataToView(task: Task) {
            this.task = task
            val taskCardDrawableResId = when (task.color) {
                TaskColor.COLOR_1 -> R.drawable.task_card_color_1
                TaskColor.COLOR_2 -> R.drawable.task_card_color_2
                TaskColor.COLOR_3 -> R.drawable.task_card_color_3
                TaskColor.COLOR_4 -> R.drawable.task_card_color_4
            }
            taskEditText.setText(task.taskText)
            taskLayout.background = ResourcesCompat.getDrawable(resources, taskCardDrawableResId, null)
        }

        @SuppressLint("ClickableViewAccessibility")
        fun prepareView() : TaskViewHolder {
            when(tasklistType){
                TASKLIST_TYPE_TODO -> { moveButton.setImageResource(R.drawable.ic_arrow_forward_24px) }
                TASKLIST_TYPE_DOING -> { moveButton.setImageResource(R.drawable.ic_done_24px) }
                TASKLIST_TYPE_DONE -> {
                    moveButton.setImageResource(R.drawable.ic_arrow_forward_24px)
                    moveButton.rotation = 180.0f
                }
                else -> { throw Exception("Unrecognized tasklist type") }
            }

            setColorPaletteVisibility(View.INVISIBLE)

            colorButton.setOnClickListener {
                if(colorPaletteIsVisible) {
                    setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)

                    if(visibleColorPaletteViewList[0] != colorPaletteBackground){
                        setColorPaletteVisibility(View.VISIBLE)

                        visibleColorPaletteViewList = colorPaletteViews
                    }
                    else{
                        colorPaletteIsVisible = false
                    }
                }
                else{
                    colorPaletteIsVisible = true
                    setColorPaletteVisibility(View.VISIBLE)

                    visibleColorPaletteViewList = colorPaletteViews
                }
            }
            taskLayout.setOnClickListener {
                if(colorPaletteIsVisible){
                    setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)
                    colorPaletteIsVisible = false
                }
            }

            deleteButton.setOnClickListener {
                deleteDialog.setPositiveButton(R.string.delete_dialog_yes_button_label)
                { _, _ ->
                    callbacks?.deleteTaskFromViewModel(tasklistType, adapterPosition)
                }.create().show()
            }

            taskEditText.addTextChangedListener {
                task.taskText = it.toString()
            }

            for (i in 1..4) {
                val colorEnum: TaskColor
                val colorResId: Int

                when (i) {
                    1 -> {
                        colorEnum = TaskColor.COLOR_1
                        colorResId = R.drawable.task_card_color_1 }
                    2 -> {
                        colorEnum = TaskColor.COLOR_2
                        colorResId = R.drawable.task_card_color_4 }
                    3 -> {
                        colorEnum = TaskColor.COLOR_3
                        colorResId = R.drawable.task_card_color_3 }
                    4 -> {
                        colorEnum = TaskColor.COLOR_4
                        colorResId = R.drawable.task_card_color_2 }
                    else -> throw Exception()
                }

                colorPaletteViews[i].setOnClickListener {
                    taskLayout.background = ResourcesCompat.getDrawable(resources, colorResId, null)

                    if (colorPaletteIsVisible) {
                        setColorPaletteVisibility(View.INVISIBLE, visibleColorPaletteViewList)
                        colorPaletteIsVisible = false
                    }

                    task.color = colorEnum
                }
            }

            dragButton.setOnTouchListener { v, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN)
                        itemTouchHelper.startDrag(this)

                    v.performClick()
                    return@setOnTouchListener true
            }

            moveButton.setOnClickListener {
                val destinationTaskListType = when(tasklistType){
                    TASKLIST_TYPE_TODO, TASKLIST_TYPE_DONE -> TASKLIST_TYPE_DOING
                    TASKLIST_TYPE_DOING -> TASKLIST_TYPE_DONE
                    else -> throw Exception("Unrecognized tasklist type")
                }

                callbacks?.addTaskToViewModel(task, destinationTaskListType)
                callbacks?.deleteTaskFromViewModel(tasklistType, adapterPosition)
            }

            return this
        }
    }

    private inner class TaskViewAdapter(var taskList : LinkedList<Task>)
        : RecyclerView.Adapter<TaskViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = layoutInflater.inflate(R.layout.task_view, parent, false)
            return TaskViewHolder(view).prepareView()
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task : Task = taskList[position]
            holder.bindTaskDataToView(task)
        }

        override fun getItemCount(): Int = taskList.size

        fun moveTaskView(from: Int, to: Int){
            val temp = taskList[from]
            taskList.removeAt(from)
            taskList.add(to, temp)
        }
    }
}

