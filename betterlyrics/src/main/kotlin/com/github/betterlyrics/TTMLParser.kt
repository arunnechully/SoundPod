package com.github.betterlyrics

import timber.log.Timber
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

object TTMLParser {

    private const val TTML_PARAMETER_NS = "http://www.w3.org/ns/ttml#parameter"

    data class ParsedLine(
        val text: String,
        val startTime: Double,
        val words: List<ParsedWord>,
        val agent: String? = null,
        val isBackground: Boolean = false,
        val backgroundLines: List<ParsedLine> = emptyList()
    )

    data class ParsedWord(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean = false
    )

    data class SpanInfo(
        val text: String,
        val startTime: Double,
        val endTime: Double,
        val hasTrailingSpace: Boolean = false
    )

    private fun getAttr(element: Element, attr: String): String {
        return element.getAttribute(attr)
    }

    private fun timingAttr(element: Element, attr: String): String {
        return element.getAttribute(attr)
    }

    private fun findFirstSpanBegin(element: Element): String? {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && node.tagName == "span") {
                val begin = timingAttr(node, "begin")
                if (begin.isNotEmpty()) return begin
            }
        }
        return null
    }

    fun parseTTML(ttml: String): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(ttml.toByteArray()))
            val root = doc.documentElement

            val body = findChild(root, "body")
            walk(body, lines, 0.0, null)
        } catch (e: Exception) {
            Timber.e(e, "TTMLParser.parseTTML: Failed to parse TTML")
        }
        return lines
    }

    private fun findChild(element: Element, tagName: String): Element {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element && (node.tagName == tagName || node.localName == tagName)) {
                return node
            }
        }
        throw NoSuchElementException("Child with tag $tagName not found")
    }

    private fun walk(element: Element, lines: MutableList<ParsedLine>, offset: Double, agent: String?) {
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element) {
                val tagName = node.localName ?: node.tagName
                when (tagName) {
                    "p" -> parseP(node, lines, offset, agent)
                    else -> walk(node, lines, offset, agent)
                }
            }
        }
    }

    private fun parseP(element: Element, lines: MutableList<ParsedLine>, offset: Double, agent: String?) {
        val beginAttr = timingAttr(element, "begin")
        val startTime = parseTime(beginAttr) + offset
        val pAgent = getAttr(element, "ttm:agent").takeIf { it.isNotEmpty() } ?: agent

        val spans = mutableListOf<SpanInfo>()
        val backgroundLines = mutableListOf<ParsedLine>()

        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element) {
                val tagName = node.localName ?: node.tagName
                if (tagName == "span") {
                    val role = getAttr(node, "ttm:role")
                    if (role == "x-bg") {
                        val bgLine = parseBackgroundSpan(node, startTime, offset)
                        if (bgLine != null) backgroundLines.add(bgLine)
                    } else {
                        parseWordSpan(node, startTime, spans, node)
                    }
                }
            } else if (node.nodeType == Node.TEXT_NODE) {
                val text = node.nodeValue.trim()
                if (text.isNotEmpty()) {
                    spans.add(SpanInfo(text, startTime, startTime, true))
                }
            }
        }

        val words = mergeSpansIntoWords(spans)
        val lineText = buildLineText(words)

        if (lineText.isNotEmpty()) {
            lines.add(
                ParsedLine(
                    text = lineText,
                    startTime = startTime,
                    words = words,
                    agent = pAgent,
                    backgroundLines = backgroundLines
                )
            )
        }
    }

    private fun parseWordSpan(element: Element, pStart: Double, spans: MutableList<SpanInfo>, node: Node) {
        val begin = timingAttr(element, "begin")
        val end = timingAttr(element, "end")
        
        val rawStart = if (begin.isNotEmpty()) parseTime(begin) else 0.0
        val startTime = if (begin.isNotEmpty()) {
            if (rawStart >= pStart) rawStart else rawStart + pStart
        } else pStart
        
        val rawEnd = if (end.isNotEmpty()) parseTime(end) else 0.0
        val endTime = if (end.isNotEmpty()) {
            if (rawEnd >= startTime) rawEnd else rawEnd + pStart
        } else startTime

        val text = getDirectText(element)
        if (text.isNotEmpty()) {
            spans.add(SpanInfo(text, startTime, endTime, true))
        }
    }

    private fun parseBackgroundSpan(element: Element, pStart: Double, offset: Double): ParsedLine? {
        val beginAttr = timingAttr(element, "begin")
        val rawStart = if (beginAttr.isNotEmpty()) parseTime(beginAttr) else 0.0
        val startTime = if (beginAttr.isNotEmpty()) {
            if (rawStart >= pStart) rawStart else rawStart + pStart
        } else pStart

        val spans = mutableListOf<SpanInfo>()
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node is Element) {
                val tagName = node.localName ?: node.tagName
                if (tagName == "span") {
                    parseWordSpan(node, startTime, spans, node)
                }
            } else if (node.nodeType == Node.TEXT_NODE) {
                val text = node.nodeValue.trim()
                if (text.isNotEmpty()) {
                    spans.add(SpanInfo(text, startTime, startTime, true))
                }
            }
        }

        val words = mergeSpansIntoWords(spans)
        val text = buildLineText(words)
        return if (text.isNotEmpty()) {
            ParsedLine(text, startTime, words, isBackground = true)
        } else null
    }

    private fun getDirectText(element: Element): String {
        val sb = StringBuilder()
        val children = element.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeType == Node.TEXT_NODE) {
                sb.append(node.nodeValue)
            } else if (node is Element) {
                val tagName = node.localName ?: node.tagName
                if (tagName == "br") {
                    sb.append(" ")
                }
            }
        }
        return sb.toString().trim()
    }

    private fun buildLineText(words: List<ParsedWord>): String {
        return words.joinToString("") { word ->
            if (word.hasTrailingSpace) word.text + " " else word.text
        }.trim()
    }

    private fun mergeSpansIntoWords(spans: List<SpanInfo>): List<ParsedWord> {
        val words = mutableListOf<ParsedWord>()
        for (span in spans) {
            words.add(
                ParsedWord(
                    text = span.text,
                    startTime = span.startTime,
                    endTime = span.endTime,
                    hasTrailingSpace = span.hasTrailingSpace
                )
            )
        }
        return words
    }

    fun toLRC(lines: List<ParsedLine>): String {
        val sb = StringBuilder()
        for (line in lines) {
            val timeStr = formatLrcTime(line.startTime)
            sb.append("[$timeStr]${line.text}\n")

            // Optionally handle background lines
            for (bg in line.backgroundLines) {
                val bgTimeStr = formatLrcTime(bg.startTime)
                sb.append("[$bgTimeStr](${bg.text})\n")
            }
        }
        return sb.toString()
    }

    private fun formatLrcTime(seconds: Double): String {
        val totalMs = (seconds * 1000).toInt()
        val mm = totalMs / 60000
        val ss = (totalMs % 60000) / 1000
        val xx = (totalMs % 1000) / 10
        return String.format("%02d:%02d.%02d", mm, ss, xx)
    }

    private fun parseTime(time: String): Double {
        if (time.isEmpty()) return 0.0
        // Handle formats like "00:00:00.000" or "00:00.000" or "0.000s"
        return try {
            if (time.endsWith("s")) {
                time.dropLast(1).toDouble()
            } else {
                val parts = time.split(":")
                var seconds = 0.0
                for (part in parts) {
                    seconds = seconds * 60 + part.toDouble()
                }
                seconds
            }
        } catch (e: Exception) {
            0.0
        }
    }
}
