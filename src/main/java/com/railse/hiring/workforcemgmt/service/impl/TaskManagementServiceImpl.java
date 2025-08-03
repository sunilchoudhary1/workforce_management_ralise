package com.railse.hiring.workforcemgmt.service.impl;

import com.railse.hiring.workforcemgmt.common.exception.ResourceNotFoundException;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.mapper.ITaskManagementMapper;
import com.railse.hiring.workforcemgmt.model.Activity;
import com.railse.hiring.workforcemgmt.model.Comment;
import com.railse.hiring.workforcemgmt.model.TaskManagement;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.model.enums.Task;
import com.railse.hiring.workforcemgmt.model.enums.TaskStatus;
import com.railse.hiring.workforcemgmt.repository.TaskRepository;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskManagementServiceImpl implements TaskManagementService {

    private final TaskRepository taskRepository;
    private final ITaskManagementMapper taskMapper;

    public TaskManagementServiceImpl(TaskRepository taskRepository, ITaskManagementMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskManagementDto findTaskById(Long id) {
        TaskManagement task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return taskMapper.modelToDto(task);
    }

    @Override
    public List<TaskManagementDto> createTasks(TaskCreateRequest createRequest) {
        List<TaskManagement> createdTasks = new ArrayList<>();
        for (TaskCreateRequest.RequestItem item : createRequest.getRequests()) {
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(item.getReferenceId());
            newTask.setReferenceType(item.getReferenceType());
            newTask.setTask(item.getTask());
            newTask.setAssigneeId(item.getAssigneeId());
            newTask.setPriority(item.getPriority());
            newTask.setTaskDeadlineTime(item.getTaskDeadlineTime());
            newTask.setStatus(TaskStatus.ASSIGNED);
            newTask.setDescription("New task created.");
            createdTasks.add(taskRepository.save(newTask));
        }
        return taskMapper.modelListToDtoList(createdTasks);
    }

    @Override
    public List<TaskManagementDto> updateTasks(UpdateTaskRequest updateRequest) {
        List<TaskManagement> updatedTasks = new ArrayList<>();
        for (UpdateTaskRequest.RequestItem item : updateRequest.getRequests()) {
            TaskManagement task = taskRepository.findById(item.getTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + item.getTaskId()));

            if (item.getTaskStatus() != null) {
                task.setStatus(item.getTaskStatus());
                task.getActivityHistory().add(new Activity("Update TaskStatus to" + item.getTaskStatus()));
            }
            if (item.getDescription() != null) {
                task.setDescription(item.getDescription());
                task.getActivityHistory().add(new Activity("Update discription to"+ item.getDescription()));
            }

            updatedTasks.add(taskRepository.save(task));
        }
        return taskMapper.modelListToDtoList(updatedTasks);
    }
/*
    @Override      // old
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());

            if (tasksOfType.isEmpty()) {
                return "No existing open task found for this reference and task type. Please assign the task first.";
            }
            if (!tasksOfType.isEmpty()) {
                for (TaskManagement taskToUpdate : tasksOfType) {
                    taskToUpdate.setAssigneeId(request.getAssigneeId()); //this is old code here
                    taskRepository.save(taskToUpdate);                 //  here only reassign task to
                }                                                   //new person without change in old assigned
            } else {                                 // by this sometimes single task can be assigned to multiple person
                TaskManagement newTask = new TaskManagement();    // If there is no such task find then assign new task
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }
        }
        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    */
   @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(
                request.getReferenceId(), request.getReferenceType()
        );

        // Find an open (ASSIGNED/STARTED) task
        TaskManagement sourceTask = existingTasks.stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED)
                .findFirst()
                .orElse(null);

        if (sourceTask == null) {
            return "No open task exists for this reference. Please create or assign a task first.";
        }

        String description =  sourceTask.getDescription();
        // Cancel all other tasks
        for (TaskManagement oldTask : existingTasks) {
            if (oldTask.getStatus() != TaskStatus.COMPLETED && oldTask.getStatus() != TaskStatus.CANCELLED) {
                oldTask.setStatus(TaskStatus.CANCELLED);
                oldTask.setDescription("Task auto-cancelled due to reassignment");
                oldTask.getActivityHistory().add(
                            new Activity("Task status changed to CANCELLED due to reassignment")
                    );
                taskRepository.save(oldTask);
            }
        }

        // Create a new task for the new assignee, copying values from sourceTask or get from request
        TaskManagement newTask = new TaskManagement();
        newTask.setReferenceId(sourceTask.getReferenceId());
        newTask.setReferenceType(sourceTask.getReferenceType());
        newTask.setTask(sourceTask.getTask());
        newTask.setDescription("Reassigned: " + description);
        newTask.setAssigneeId(request.getAssigneeId());
        newTask.setPriority(sourceTask.getPriority());

        // Set deadline: use user-provided value if present, else previous one from sourceTask
        Long newDeadline = request.getNewDeadline();
        if (newDeadline != null) {
            newTask.setTaskDeadlineTime(newDeadline);
        } else {
            newTask.setTaskDeadlineTime(sourceTask.getTaskDeadlineTime());
        }

        newTask.setStatus(TaskStatus.ASSIGNED);
        newTask.getActivityHistory().add(
                    new Activity("Task assigned by reference to user " + request.getAssigneeId())
            );
        if (newTask.getActivityHistory() != null) {
            newTask.getActivityHistory().sort((a1, a2) -> Long.compare(a1.getTimestamp(), a2.getTimestamp()));
        }
        taskRepository.save(newTask);
        return "New task assigned to user " + request.getAssigneeId() + " for reference " + request.getReferenceId();
    }


    /* @Override   // old
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task -> true)
                .collect(Collectors.toList());
        return taskMapper.modelListToDtoList(filteredTasks);
    }

*/
    @Override       // new
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() != TaskStatus.CANCELLED &&
                                (
                                        // All active tasks in range
                                        (task.getTaskDeadlineTime() >= request.getStartDate() && task.getTaskDeadlineTime() <= request.getEndDate())
                                                // PLUS: all still-open (not completed) tasks before the range (spillover)
                                                || (task.getTaskDeadlineTime() < request.getStartDate() && task.getStatus() != TaskStatus.COMPLETED)
                                )
                )
                .collect(Collectors.toList());

        return taskMapper.modelListToDtoList(filteredTasks);
    }
    // new features
    @Override
    public List<TaskManagementDto> getTasksByPriority(Priority priority) {
        List<TaskManagement> filtered = taskRepository.findAll().stream()
                .filter(task -> task.getPriority() == priority)
                .collect(Collectors.toList());
        return taskMapper.modelListToDtoList(filtered);
    }

    @Override
    public TaskManagementDto changeTaskPriority(ChangePriorityRequest request) {
        TaskManagement task = taskRepository.findById(request.getTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));
        task.setPriority(request.getPriority());
        task.getActivityHistory().add(new Activity("Priority changed to "+ ": "+ request.getPriority()));
        // Optional: sort activity history for chronological correctness
        if (task.getActivityHistory() != null) {
            task.getActivityHistory().sort((a1, a2) -> Long.compare(a1.getTimestamp(), a2.getTimestamp()));
        }
        taskRepository.save(task);
        return taskMapper.modelToDto(task);
    }

    @Override
    public TaskManagementDto addComment(Long taskId, CommentRequest request) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        Comment comment = new Comment();
        comment.setAuthor(request.getAuthor());
        comment.setMessage(request.getMessage());
        comment.setTimestamp(System.currentTimeMillis());
        task.getComments().add(comment);

        // Add to activity history
        task.getActivityHistory().add(new Activity(
                "Comment added by " + comment.getAuthor() + ": " + comment.getMessage()
        ));
        // --- Sort comments ---
        if (task.getComments() != null) {
            task.getComments().sort((c1, c2) -> Long.compare(c1.getTimestamp(), c2.getTimestamp()));
        }
        // --- Sort activity history ---
        if (task.getActivityHistory() != null) {
            task.getActivityHistory().sort((a1, a2) -> Long.compare(a1.getTimestamp(), a2.getTimestamp()));
        }

        taskRepository.save(task);
        return taskMapper.modelToDto(task);
    }


    @Override
    public List<TaskManagementDto> getAllTasks() {

        return taskMapper.modelListToDtoList(taskRepository.findAll());
    }



}
