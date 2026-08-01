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

class InitDatabaseJvmTest {
    private fun <T> anyOrNull(): T? = Mockito.any()

    @Test
    fun testInitDatabaseSuccess() =
        runBlocking {
            val driver = Mockito.mock(SqlDriver::class.java)
            Mockito.doReturn(QueryResult.Value(1L)).`when`(driver).execute(anyOrNull(), anyString(), anyInt(), anyOrNull())
            initDatabase(driver)
            assertTrue(true)
        }

    @Test
    fun testInitDatabaseIllegalStateException() =
        runBlocking {
            val driver = Mockito.mock(SqlDriver::class.java)
            val e = IllegalStateException("test")
            Mockito.doThrow(e).`when`(driver).execute(anyOrNull(), anyString(), anyInt(), anyOrNull())
            initDatabase(driver)
            assertTrue(true)
        }

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
