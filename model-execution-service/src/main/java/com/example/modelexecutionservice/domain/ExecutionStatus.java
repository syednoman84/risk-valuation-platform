package com.example.modelexecutionservice.domain;

public enum ExecutionStatus {
    PENDING, // created, not yet queued
    QUEUED, // messages prepared & enqueued
    RUNNING, // at least one chunk is executing
    PARTIAL_SUCCESS,// finished but some chunks/loans failed
    COMPLETED, // fully successful
    FAILED, // unrecoverable failure
    CANCELED // cancel requested & honored
}