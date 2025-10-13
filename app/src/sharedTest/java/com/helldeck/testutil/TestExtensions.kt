package com.helldeck.testutil

import io.mockk.MockKMatcherScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

/**
 * Test utility extensions for better testing experience
 */

/**
 * Convert Flow to List for testing
 */
suspend fun <T> Flow<T>.toList(): List<T> {
    return this.toList()
}

/**
 * Get first item from Flow for testing
 */
suspend fun <T> Flow<T>.firstItem(): T {
    return this.first()
}

/**
 * MockK matcher for any string containing a substring
 */
fun MockKMatcherScope contains(substring: String) = match<String> {
    it.contains(substring)
}

/**
 * MockK matcher for any string matching a regex
 */
fun MockKMatcherScope matches(regex: Regex) = match<String> {
    regex.matches(it)
}

/**
 * Helper function to run test with retry logic
 */
fun retryTest(times: Int = 3, test: () -> Unit) {
    var lastException: Exception? = null

    repeat(times) { attempt ->
        try {
            test()
            return
        } catch (e: Exception) {
            lastException = e
            if (attempt < times - 1) {
                Thread.sleep(100) // Brief pause between retries
            }
        }
    }

    throw lastException!!
}

/**
 * Helper function to assert that a block throws a specific exception
 */
inline fun <reified T : Throwable> assertThrows(block: () -> Unit): T {
    try {
        block()
        throw AssertionError("Expected exception of type ${T::class.simpleName} but none was thrown")
    } catch (e: Throwable) {
        if (e is T) {
            return e
        }
        throw AssertionError("Expected exception of type ${T::class.simpleName} but got ${e::class.simpleName}")
    }
}

/**
 * Helper function to assert that a block does not throw
 */
fun assertDoesNotThrow(block: () -> Unit) {
    try {
        block()
    } catch (e: Exception) {
        throw AssertionError("Expected no exception but got ${e::class.simpleName}: ${e.message}")
    }
}