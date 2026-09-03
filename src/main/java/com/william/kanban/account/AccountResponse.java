package com.william.kanban.account;

import java.util.UUID;

record AccountResponse(UUID id, String email, String displayName) {
}
