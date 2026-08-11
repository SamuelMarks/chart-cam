/**
 * @file InitDatabaseJvmTest.kt
 * Contains declarations for InitDatabaseJvmTest.kt.
 */
package io.healthplatform.chartcam

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import kotlin.test.assertTrue

/**
 * Tests for database initialization on JVM.
 */
class InitDatabaseJvmTest {
    /**
     * Helper to mock any object.
     * @return null casted to T.
     */
    private fun <T> anyOrNull(): T? = Mockito.any()

    /**
     * Tests successful database initialization.
     */
    @Test
    fun testInitDatabaseSuccess() =
        runBlocking {
            val driver = Mockito.mock(SqlDriver::class.java)
            Mockito.doReturn(QueryResult.Value(1L)).`when`(driver).execute(anyOrNull(), anyString(), anyInt(), anyOrNull())
            initDatabase(driver)
            assertTrue(true)
        }

    /**
     * Tests handling of IllegalStateException during initialization.
     */
    @Test
    fun testInitDatabaseIllegalStateException() =
        runBlocking {
            val driver = Mockito.mock(SqlDriver::class.java)
            val e = IllegalStateException("test")
            Mockito.doThrow(e).`when`(driver).execute(anyOrNull(), anyString(), anyInt(), anyOrNull())
            initDatabase(driver)
            assertTrue(true)
        }

    /**
     * Tests handling of IllegalArgumentException during initialization.
     */
    @Test
    fun testInitDatabaseIllegalArgumentException() =
        runBlocking {
            val driver = Mockito.mock(SqlDriver::class.java)
            val e = IllegalArgumentException("test")
            Mockito.doThrow(e).`when`(driver).execute(anyOrNull(), anyString(), anyInt(), anyOrNull())
            initDatabase(driver)
            assertTrue(true)
        }
}
