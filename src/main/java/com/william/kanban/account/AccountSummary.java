package com.william.kanban.account;

import java.util.UUID;

public record AccountSummary(UUID id, String email, String displayName) {
}
