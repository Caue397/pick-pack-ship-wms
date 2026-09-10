package com.pickpackship.auth.outbox;

import java.util.UUID;

public record WorkspaceDeletedPayload(UUID workspaceId) {}
