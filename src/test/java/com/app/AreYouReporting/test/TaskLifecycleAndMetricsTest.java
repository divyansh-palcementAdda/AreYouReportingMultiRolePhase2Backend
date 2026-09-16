package com.app.AreYouReporting.test;

import com.app.AreYouReporting.Entities.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TaskLifecycleAndMetricsTest {

    @Test
    @DisplayName("Task lifecycle tracks delay, extension count and outcome metrics correctly")
    void testTaskMetricsCalculation() {
        User creator = User.builder().username("creator").fullName("Creator User").build();
        creator.setId(UUID.randomUUID());

        Instant originalDue = Instant.now().minus(2, ChronoUnit.DAYS);

        Task task = Task.builder()
                .title("Sample Task")
                .status(TaskStatus.DELAYED)
                .startDate(Instant.now().minus(5, ChronoUnit.DAYS))
                .dueDate(originalDue)
                .originalDueDate(originalDue)
                .creator(creator)
                .extensionCount(1)
                .isCompletedAfterDelay(true)
                .isCompletedAfterExtension(true)
                .completionOutcome(TaskCompletionOutcome.AFTER_DELAY_AND_EXTENSION)
                .build();

        assertEquals(TaskStatus.DELAYED, task.getStatus());
        assertTrue(task.isCompletedAfterDelay());
        assertTrue(task.isCompletedAfterExtension());
        assertEquals(TaskCompletionOutcome.AFTER_DELAY_AND_EXTENSION, task.getCompletionOutcome());
        assertEquals(1, task.getExtensionCount());
    }

    @Test
    @DisplayName("Task request rejection counters increment properly")
    void testTaskRejectionCounters() {
        Task task = Task.builder()
                .title("Audit Task")
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .originalDueDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .extensionRejectionCount(2)
                .closureRejectionCount(1)
                .totalRejectionCount(3)
                .lastRejectionReason("Incomplete attendance proofs")
                .build();

        assertEquals(2, task.getExtensionRejectionCount());
        assertEquals(1, task.getClosureRejectionCount());
        assertEquals(3, task.getTotalRejectionCount());
        assertEquals("Incomplete attendance proofs", task.getLastRejectionReason());
    }
}
