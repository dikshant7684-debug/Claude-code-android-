package com.example.extensions.models;

public enum InstallationStatus {
    IDLE,
    FETCHING,
    INSPECTING,
    VALIDATING,
    AWAITING_APPROVAL,
    RESOLVING_DEPENDENCIES,
    INSTALLING,
    REGISTERING,
    VERIFYING,
    COMPLETED,
    FAILED,
    ROLLING_BACK
}
