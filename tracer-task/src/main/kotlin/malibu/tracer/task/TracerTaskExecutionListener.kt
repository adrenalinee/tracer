package malibu.tracer.task

import org.springframework.cloud.task.listener.TaskExecutionListener
import org.springframework.cloud.task.repository.TaskExecution

class TracerTaskExecutionListener: TaskExecutionListener {
    override fun onTaskStartup(taskExecution: TaskExecution) {
        TODO("Not yet implemented")
    }

    override fun onTaskEnd(taskExecution: TaskExecution) {
        TODO("Not yet implemented")
    }

    override fun onTaskFailed(taskExecution: TaskExecution, throwable: Throwable) {
        TODO("Not yet implemented")
    }
}