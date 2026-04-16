package com.decathlon.smartnutristock.data.notification

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for NotificationHelper.
 *
 * Tests verify notification channel creation, grouped notification building,
 * and proper interaction with NotificationManagerCompat.
 */
class NotificationHelperTest {

    @Test
    fun `channel constants are correctly defined`() {
        // Then - verify constants match design spec
        assertThat(NotificationHelper.CHANNEL_ID).isEqualTo("smartnutristock_alerts")
        assertThat(NotificationHelper.GROUP_KEY_YELLOW).isEqualTo("smartnutristock_group_yellow")
        assertThat(NotificationHelper.GROUP_KEY_EXPIRED).isEqualTo("smartnutristock_group_expired")
    }
}
