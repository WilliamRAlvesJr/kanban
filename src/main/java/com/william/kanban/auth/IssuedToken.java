package com.william.kanban.auth;

import java.time.OffsetDateTime;

record IssuedToken(String value, OffsetDateTime expiresAt) {
}
