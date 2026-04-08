package com.decathlon.smartnutristock.domain.model

/**
 * WorkflowAction represents the corrective action taken on a batch.
 *
 * Tracks whether a batch is pending action, has been discounted (-20%),
 * or has been removed from the public shelf.
 */
enum class WorkflowAction {
    PENDING,
    DISCOUNTED,
    REMOVED
}
