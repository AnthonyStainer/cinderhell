package dev.cinderhell.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandheldGameplayMappingTest {
    @Test
    fun mappingCoversEveryPromisedGameplayCapability() {
        val actions = HandheldGameplayMapping.controls.joinToString { it.action }
        listOf(
            "Move",
            "Turn",
            "Fire",
            "Use",
            "weapon",
            "menu",
            "Automap",
        ).forEach { capability ->
            assertTrue("$capability is mapped", actions.contains(capability, ignoreCase = true))
        }
        assertEquals(
            HandheldGameplayMapping.controls.map { it.control }.distinct().size,
            HandheldGameplayMapping.controls.size,
        )
    }
}
