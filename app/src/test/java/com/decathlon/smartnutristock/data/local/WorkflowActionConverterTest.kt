package com.decathlon.smartnutristock.data.local

import com.decathlon.smartnutristock.domain.model.WorkflowAction
import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Unit tests for WorkflowActionConverter.
 *
 * Verifies round-trip conversion between WorkflowAction enum and String.
 */
class WorkflowActionConverterTest {

    private val converter = WorkflowActionConverter()

    @Test
    fun `fromWorkflowAction converts PENDING to String`() {
        val result = converter.fromWorkflowAction(WorkflowAction.PENDING)
        assertThat(result).isEqualTo("PENDING")
    }

    @Test
    fun `fromWorkflowAction converts DISCOUNTED to String`() {
        val result = converter.fromWorkflowAction(WorkflowAction.DISCOUNTED)
        assertThat(result).isEqualTo("DISCOUNTED")
    }

    @Test
    fun `fromWorkflowAction converts REMOVED to String`() {
        val result = converter.fromWorkflowAction(WorkflowAction.REMOVED)
        assertThat(result).isEqualTo("REMOVED")
    }

    @Test
    fun `toWorkflowAction converts String to PENDING`() {
        val result = converter.toWorkflowAction("PENDING")
        assertThat(result).isEqualTo(WorkflowAction.PENDING)
    }

    @Test
    fun `toWorkflowAction converts String to DISCOUNTED`() {
        val result = converter.toWorkflowAction("DISCOUNTED")
        assertThat(result).isEqualTo(WorkflowAction.DISCOUNTED)
    }

    @Test
    fun `toWorkflowAction converts String to REMOVED`() {
        val result = converter.toWorkflowAction("REMOVED")
        assertThat(result).isEqualTo(WorkflowAction.REMOVED)
    }

    @Test
    fun `round-trip conversion PENDING works correctly`() {
        val original = WorkflowAction.PENDING
        val string = converter.fromWorkflowAction(original)
        val convertedBack = converter.toWorkflowAction(string)
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun `round-trip conversion DISCOUNTED works correctly`() {
        val original = WorkflowAction.DISCOUNTED
        val string = converter.fromWorkflowAction(original)
        val convertedBack = converter.toWorkflowAction(string)
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test
    fun `round-trip conversion REMOVED works correctly`() {
        val original = WorkflowAction.REMOVED
        val string = converter.fromWorkflowAction(original)
        val convertedBack = converter.toWorkflowAction(string)
        assertThat(convertedBack).isEqualTo(original)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toWorkflowAction throws exception for invalid String`() {
        converter.toWorkflowAction("INVALID_ACTION")
    }
}
