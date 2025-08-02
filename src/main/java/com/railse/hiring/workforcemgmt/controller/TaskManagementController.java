package com.railse.hiring.workforcemgmt.controller;

import com.railse.hiring.workforcemgmt.common.model.response.Response;
import com.railse.hiring.workforcemgmt.dto.*;
import com.railse.hiring.workforcemgmt.model.enums.Priority;
import com.railse.hiring.workforcemgmt.service.TaskManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task-mgmt")
public class TaskManagementController {

    private final TaskManagementService taskManagementService;

    public TaskManagementController(TaskManagementService taskManagementService) {
        this.taskManagementService = taskManagementService;
    }

    @GetMapping("/{id}")
    public Response<TaskManagementDto> getTaskById(@PathVariable Long id) {
        return new Response<>(taskManagementService.findTaskById(id));
    }

    @PostMapping("/create")
    public Response<List<TaskManagementDto>> createTasks(@RequestBody TaskCreateRequest request) {
        return new Response<>(taskManagementService.createTasks(request));
    }

    @PostMapping("/update")
    public Response<List<TaskManagementDto>> updateTasks(@RequestBody UpdateTaskRequest request) {
        return new Response<>(taskManagementService.updateTasks(request));
    }

    @PostMapping("/assign-by-ref")
    public Response<String> assignByReference(@RequestBody AssignByReferenceRequest request) {
        return new Response<>(taskManagementService.assignByReference(request));
    }

    @PostMapping("/fetch-by-date/v2")
    public Response<List<TaskManagementDto>> fetchByDate(@RequestBody TaskFetchByDateRequest request) {
        return new Response<>(taskManagementService.fetchTasksByDate(request));
    }


    // new features
    @PostMapping("/change-priority")
    public Response<TaskManagementDto> changePriority(@RequestParam Long taskId, @RequestParam Priority priority) {
        return new Response<>(taskManagementService.changeTaskPriority(taskId, priority));
    }

    @GetMapping("/priority/{priority}")
    public Response<List<TaskManagementDto>> getTasksByPriority(@PathVariable Priority priority) {
        return new Response<>(taskManagementService.getTasksByPriority(priority));
    }

    // comment
    @PostMapping("/{id}/comment")
    public Response<TaskManagementDto> addComment(@PathVariable Long id, @RequestBody CommentRequest request) {
        return new Response<>(taskManagementService.addComment(id, request));
    }


    @GetMapping("/all")
    public Response<List<TaskManagementDto>> getAllTasks() {
        return new Response<>(taskManagementService.getAllTasks());
    }


}
