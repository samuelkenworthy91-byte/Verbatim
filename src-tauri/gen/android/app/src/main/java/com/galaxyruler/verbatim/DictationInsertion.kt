package com.galaxyruler.verbatim

/** Insert at the cursor, replace an explicit selection, or append if unavailable. */
internal fun mergeDictation(
  existing: String,
  dictated: String,
  selectionStart: Int,
  selectionEnd: Int,
): Pair<String, Int> {
  val validSelection = selectionStart in 0..existing.length &&
    selectionEnd in 0..existing.length
  val start = if (validSelection) minOf(selectionStart, selectionEnd) else existing.length
  val end = if (validSelection) maxOf(selectionStart, selectionEnd) else existing.length
  if (dictated.isEmpty()) return existing to start

  val prefix = existing.substring(0, start)
  val suffix = existing.substring(end)
  val before = if (prefix.isNotEmpty() && !prefix.last().isWhitespace() &&
    dictated.first().isLetterOrDigit()) " " else ""
  val after = if (suffix.isNotEmpty() && suffix.first().isLetterOrDigit() &&
    !dictated.last().isWhitespace()) " " else ""
  val insertion = before + dictated
  return (prefix + insertion + after + suffix) to (prefix.length + insertion.length + after.length)
}
