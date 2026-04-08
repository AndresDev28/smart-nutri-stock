package com.decathlon.smartnutristock.data.local

import androidx.room.TypeConverter
import com.decathlon.smartnutristock.domain.model.WorkflowAction

/**
 * TypeConverter for WorkflowAction enum.
 *
 * Uses String-based storage (NOT ordinal) to prevent data corruption
 * when enum values are reordered or new values are added.
 */
class WorkflowActionConverter {
    /**
     * Convert WorkflowAction enum to String for database storage.
     */
    @TypeConverter
    fun fromWorkflowAction(action: WorkflowAction): String = action.name

    /**
     * Convert String from database to WorkflowAction enum.
     */
    @TypeConverter
    fun toWorkflowAction(value: String): WorkflowAction = enumValueOf(value)
}
