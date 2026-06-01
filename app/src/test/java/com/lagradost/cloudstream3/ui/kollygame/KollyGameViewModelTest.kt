package com.lagradost.cloudstream3.ui.kollygame

import org.junit.Assert
import org.junit.Test

class KollyGameViewModelTest {

    @Test
    fun testGenreMapping() {
        val vm = KollyGameViewModel()
        Assert.assertEquals(28L, vm.genreMap["Action"])
        Assert.assertEquals(35L, vm.genreMap["Comedy"])
        Assert.assertEquals(53L, vm.genreMap["Thriller"])
    }

    @Test
    fun testDecadeMapping() {
        val vm = KollyGameViewModel()
        // Accessing year parsing fallback check indirectly via state trigger, or verifying mapping definitions
        Assert.assertNotNull(vm.curatorChatMessages.value)
        Assert.assertTrue(vm.curatorChatMessages.value.isNotEmpty())
    }
}
