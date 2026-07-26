package dev.cinderhell

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun applicationIdentityIsStable() {
        assertEquals("dev.cinderhell.debug", BuildConfig.APPLICATION_ID)
    }
}
