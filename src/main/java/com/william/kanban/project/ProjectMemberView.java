package com.william.kanban.project;

import com.william.kanban.dto.account.AccountSummary;

record ProjectMemberView(ProjectMember member, AccountSummary account, ProjectAccess access) {
}
