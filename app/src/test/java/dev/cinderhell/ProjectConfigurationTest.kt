package dev.cinderhell

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigurationTest {
    @Test
    fun applicationIdentityIsStable() {
        assertEquals("io.github.anthonystainer.cinderhell.debug", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun versionMetadataUsesExplicitReleaseOverrideOrDevelopmentDefault() {
        val expectedName = System.getenv("CINDERHELL_VERSION_NAME") ?: "0.1.0-dev"
        val expectedCode = System.getenv("CINDERHELL_VERSION_CODE")?.toInt() ?: 1_000_000

        assertEquals(expectedName, BuildConfig.VERSION_NAME)
        assertEquals(expectedCode, BuildConfig.VERSION_CODE)
    }
}
