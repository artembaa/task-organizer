package com.project.taskorg

import java.util.UUID

data class Task(val taskId: UUID = UUID.randomUUID(),
                var taskText: String = "",
                var color: TaskColor = TaskColor.COLOR_1)
