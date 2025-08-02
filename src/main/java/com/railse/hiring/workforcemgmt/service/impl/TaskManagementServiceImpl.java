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

    @Override
    public String assignByReference(AssignByReferenceRequest request) {
        List<Task> applicableTasks = Task.getTasksByReferenceType(request.getReferenceType());
        List<TaskManagement> existingTasks = taskRepository.findByReferenceIdAndReferenceType(request.getReferenceId(), request.getReferenceType());

        for (Task taskType : applicableTasks) {
            List<TaskManagement> tasksOfType = existingTasks.stream()
                    .filter(t -> t.getTask() == taskType && t.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());
/*
            if (!tasksOfType.isEmpty()) {
                for (TaskManagement taskToUpdate : tasksOfType) {
                    taskToUpdate.setAssigneeId(request.getAssigneeId());  this is old code here
                    taskRepository.save(taskToUpdate);                     here only reassign task to
                }                                                     new person without change in old assigned
            } else {                                    by this sometimes single task can be assigned to multiple person
                TaskManagement newTask = new TaskManagement();      If there is no such task find then assign new task
                newTask.setReferenceId(request.getReferenceId());
                newTask.setReferenceType(request.getReferenceType());
                newTask.setTask(taskType);
                newTask.setAssigneeId(request.getAssigneeId());
                newTask.setStatus(TaskStatus.ASSIGNED);
                taskRepository.save(newTask);
            }
 */
            for (TaskManagement oldTask : tasksOfType) {
                oldTask.setStatus(TaskStatus.CANCELLED);
                oldTask.setDescription("Task auto-cancelled due to reassignment");
                oldTask.getActivityHistory().add(new Activity("status changed due to reassignment"+ " : " + oldTask.getStatus())); // add activity
                taskRepository.save(oldTask);                // here task get cancelled that previously assigned
            }                                  // and assign again to new person.

            // Now assign the new task for the new assignee
            TaskManagement newTask = new TaskManagement();
            newTask.setReferenceId(request.getReferenceId());
            newTask.setReferenceType(request.getReferenceType());
            newTask.setTask(taskType);
            newTask.setAssigneeId(request.getAssigneeId());
            newTask.setStatus(TaskStatus.ASSIGNED);
            taskRepository.save(newTask);
        }
        return "Tasks assigned successfully for reference " + request.getReferenceId();
    }

    @Override
    public List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request) {
        List<TaskManagement> tasks = taskRepository.findByAssigneeIdIn(request.getAssigneeIds());
//        List<TaskManagement> filteredTasks = tasks.stream()
//                .filter(task -> true)
//                .collect(Collectors.toList());

        List<TaskManagement> filteredTasks = tasks.stream()
                .filter(task ->
                        task.getStatus() != TaskStatus.CANCELLED &&
                                (
                                        (task.getTaskDeadlineTime() >= request.getStartDate() && task.getTaskDeadlineTime() <= request.getEndDate())
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
    public TaskManagementDto changeTaskPriority(Long taskId, Priority priority) {
        TaskManagement task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        task.setPriority(priority);
        task.getActivityHistory().add(new Activity("Priority changed to " + priority));
        // Log activity history if implemented (see Feature 3)
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

        taskRepository.save(task);
        return taskMapper.modelToDto(task);
    }


    @Override
    public List<TaskManagementDto> getAllTasks() {
        return taskMapper.modelListToDtoList(taskRepository.findAll());
    }



}
