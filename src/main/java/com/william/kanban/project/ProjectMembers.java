package com.william.kanban.project;

import java.util.List;

record ProjectMembers(List<ProjectMemberView> members, ProjectAccess access) {
}
