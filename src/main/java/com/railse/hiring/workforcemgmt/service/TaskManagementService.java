package com.railse.hiring.workforcemgmt.service;

import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.model.enums.Priority;

import java.util.List;

public interface TaskManagementService {
    List<TaskManagementDto> createTasks(TaskCreateRequest request);
    List<TaskManagementDto> updateTasks(UpdateTaskRequest request);
    String assignByReference(AssignByReferenceRequest request);
    List<TaskManagementDto> fetchTasksByDate(TaskFetchByDateRequest request);
    TaskManagementDto findTaskById(Long id);
    List<TaskManagementDto> getTasksByPriority(Priority priority);
    TaskManagementDto changeTaskPriority(ChangePriorityRequest request);
    TaskManagementDto addComment(Long taskId, CommentRequest request);
    List<TaskManagementDto> getAllTasks();


}
