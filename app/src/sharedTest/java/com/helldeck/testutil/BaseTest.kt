package com.helldeck.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before

/**
 * Base test class providing common testing utilities for all test types
 */
@ExperimentalCoroutinesApi
abstract class BaseTest {

    protected val testDispatcher = StandardTestDispatcher()
    protected val testScope = TestScope(testDispatcher)

    @Before
    open fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    open fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Helper function to run test with timeout
     */
    protected fun runTestWithTimeout(timeoutMs: Long = 5000L, test: suspend () -> Unit) {
        testScope.runTest(timeoutMs) {
            test()
        }
    }

    /**
     * Helper function to advance time by specified milliseconds
     */
    protected fun advanceTimeBy(milliseconds: Long) {
        testScope.testScheduler.advanceTimeBy(milliseconds)
    }

    /**
     * Helper function to run current coroutine tasks
     */
    protected fun runCurrent() {
        testScope.testScheduler.runCurrent()
    }
}