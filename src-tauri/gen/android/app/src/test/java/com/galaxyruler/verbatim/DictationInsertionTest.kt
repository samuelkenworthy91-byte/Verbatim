package com.galaxyruler.verbatim

import org.junit.Assert.assertEquals
import org.junit.Test

class DictationInsertionTest {
  @Test
  fun removesOnlyTheLeadingMessagePrefix() {
    assertEquals("Hello there", stripLeadingMessage("Message Hello there"))
    assertEquals("Hello there", stripLeadingMessage("message: Hello there"))
    assertEquals("Hello there", stripLeadingMessage("  MESSAGE. Hello there"))
    assertEquals("message me later", stripLeadingMessage("Message message me later"))
    assertEquals("Send a message later", stripLeadingMessage("Send a message later"))
    assertEquals("Messages arrived", stripLeadingMessage("Messages arrived"))
    assertEquals("messenger", stripLeadingMessage("messenger"))
    assertEquals("", stripLeadingMessage("Message."))
    val longText = "a long transcript ".repeat(5000)
    assertEquals(longText, stripLeadingMessage("Message " + longText))
  }

  @Test
  fun secondDictationPreservesFirstAndMovesCursorToEnd() {
    val first = mergeDictation("", "First message.", 0, 0)
    val second = mergeDictation(first.first, "Second message.", first.second, first.second)
    assertEquals("First message. Second message." to 30, second)
  }

  @Test
  fun cursorInMiddlePreservesBothSides() {
    assertEquals("Hello wonderful world" to 16, mergeDictation("Hello world", "wonderful", 6, 6))
  }

  @Test
  fun onlyExplicitSelectionIsReplaced() {
    assertEquals("Hello new world" to 9, mergeDictation("Hello old world", "new", 6, 9))
    assertEquals("Hello new world" to 9, mergeDictation("Hello old world", "new", 9, 6))
  }

  @Test
  fun missingOrInvalidSelectionAppends() {
    assertEquals("Existing text More" to 18, mergeDictation("Existing text", "More", -1, -1))
    assertEquals("Existing text More" to 18, mergeDictation("Existing text", "More", 99, 99))
  }

  @Test
  fun keepsWhitespacePunctuationAndLongTranscripts() {
    assertEquals("Hello, world" to 6, mergeDictation("Hello world", ",", 5, 5))
    val longText = "word ".repeat(5000)
    assertEquals(longText to longText.length, mergeDictation("", longText, 0, 0))
  }
}
