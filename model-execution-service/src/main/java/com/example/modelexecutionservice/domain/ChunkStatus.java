package com.example.modelexecutionservice.domain;

public enum ChunkStatus {
    PENDING, // created, waiting to be enqueued
    QUEUED, // enqueued to broker
    RUNNING, // worker picked it up
    SUCCEEDED, // all loans in this chunk processed successfully
    FAILED, // exceeded retries / unrecoverable
    CANCELED, // cancel requested & honored
    DEAD_LETTERED // routed to DLQ by broker
}
