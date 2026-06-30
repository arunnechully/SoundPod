package com.github.betterlyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class TTMLParserTest {

    @Test
    fun testParseTime() {
        // Reflection to call private method for testing if needed, or just test parseTTML
    }

    @Test
    fun testToLRC() {
        val lines = listOf(
            TTMLParser.ParsedLine("Hello World", 10.5, emptyList()),
            TTMLParser.ParsedLine("Goodbye", 20.0, emptyList())
        )
        val lrc = TTMLParser.toLRC(lines)
        val expected = "[00:10.50]Hello World\n[00:20.00]Goodbye\n"
        assertEquals(expected, lrc)
    }
}
