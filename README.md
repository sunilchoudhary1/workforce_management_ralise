
# Workforce Management API

A Spring Boot (Java 17) REST API that allows managers to create, assign, update, and track work tasks for staff within a logistics company. Built as part of the Railse backend engineer challenge, this project demonstrates robust code organization, feature-complete endpoints, and business-ready logic.

## Overview

- **Create, assign, and update workforce tasks**
- **Reassign tasks with proper audit trail & no duplication**
- **"Smart Daily View" so users always see active and spillover tasks**
- **Set and update task priority: HIGH, MEDIUM, LOW**
- **Filter tasks by priority**
- **Add comments to tasks & see all action history chronologically**
- **All data is stored in-memory (no DB required) for fast demo/testing**


## Project Structure

src/main/java/com/yourcompany/workforcemgmt/

├── Application.java

├── controller/
│   └── TaskManagementController.java.

├── service/
│   ├── TaskManagementService.java.
│   └── impl/TaskManagementServiceImpl.java.

├── model/
│   ├── TaskManagement.java
│   └── enums/ (Task, Priority, TaskStatus, etc.)

├── dto/
│   └── (All data transfer/request objects)
├── repository/
│   └── InMemoryTaskRepository.java
├── mapper/
│   └── ITaskManagementMapper.java
├── common/
│   └── exception/
│   └── model/

└── (other utility/helper folders as needed)


## How to Run

1. **Prerequisites:** JDK 17+, Gradle installed
2. **Clone repo:**
git clone https: https://github.com/sunilchoudhary1/workforce_management_ralise

3. **Start API:**
./gradlew bootRun

The app runs on `http://localhost:8080/`.


## API Endpoints (Examples)

### Get a Task by ID
curl --location 'http://localhost:8080/task-mgmt/1'


### Create a Task
curl --location 'http://localhost:8080/task-mgmt/create'
--header 'Content-Type: application/json'
--data '{
"requests": [
{
"reference_id": 105,
"reference_type": "ORDER",
"task": "CREATE_INVOICE",
"assignee_id": 1,
"priority": "HIGH",
"task_deadline_time": 1728192000000
}
]
}'


### Assign/Reassign by Reference
curl --location 'http://localhost:8080/task-mgmt/assign-by-ref'
--header 'Content-Type: application/json'
--data '{
"reference_id": 201,
"reference_type": "ENTITY",
"assignee_id": 5,
"new_deadline": 1754505600000 // optional
}'


### Fetch Tasks: “Smart Daily View”
curl --location 'http://localhost:8080/task-mgmt/fetch-by-date/v2'
--header 'Content-Type: application/json'
--data '{
"start_date": 1672531200000,
"end_date": 1735689599000,
"assignee_ids": [1,
}'


### Change Priority
curl --location --request POST 'http://localhost:8080/task-mgmt/change-priority'
--header 'Content-Type: application/json'
--data '{"taskId": 1, "priority": "LOW"}'


### Add Comment to Task
curl --location --request POST 'http://localhost:8080/task-mgmt/1/comment'
--header 'Content-Type: application/json'
--data '{"author": "Alice", "message": "Please process this urgently."}'



## Technical Stack

- **Java 17**, Spring Boot 3.0.4, Gradle
- **MapStruct, Lombok**
- **In-memory storage** for easy demo (no setup, all data resets on restart)

---

## Assignment/Feature Highlights

- **Bug fixes for duplicate assignment and task filtering**
- **"Smart Daily View"** (active + overdue task listing)
- **Priority logic:** update & filter by priority
- **Comments and complete activity history on tasks**
- **All arrays sorted chronologically for full audit/collaboration clarity**

**Note:** All state is non-persistent. Your demo data, comments, and actions are lost upon server restart (by assignment design).

## Submission

- [GitHub Repo Link] (add your actual repo link here)
- [Demo Video Link] (add your YouTube/Drive/Loom link here)
- See `SUBMISSION.md` in this repo root for official submission details.

