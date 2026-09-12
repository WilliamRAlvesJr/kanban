package com.william.kanban.project;

import com.william.kanban.account.AccountSummary;

record ProjectMemberView(ProjectMember member, AccountSummary account, ProjectAccess access) {
}
