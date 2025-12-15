package com.helldeck

import com.helldeck.ui.vm.GameNightViewModel
import org.junit.Test
import org.junit.Assert.*

/**
 * GameNightViewModel unit tests.
 * Validates core ViewModel functionality.
 */
class GameNightViewModelTest {
    @Test
    fun viewModel_initializes() {
        val vm = GameNightViewModel()
        assertNotNull(vm)
        assertFalse(vm.isLoading)
    }

    @Test
    fun sessionId_isUnique() {
        val vm1 = GameNightViewModel()
        val vm2 = GameNightViewModel()
        assertNotEquals(vm1.gameNightSessionId, vm2.gameNightSessionId)
    }
}
