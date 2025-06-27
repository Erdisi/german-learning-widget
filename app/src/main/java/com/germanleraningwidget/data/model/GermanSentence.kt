package com.germanleraningwidget.data.model

data class GermanSentence(
    val id: Long = 0,
    val germanText: String,
    val translation: String,
    val level: GermanLevel,
    val topic: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SentenceHistory(
    val id: Long = 0,
    val sentenceId: Long,
    val germanText: String,
    val translation: String,
    val level: GermanLevel,
    val topic: String,
    val deliveredAt: Long = System.currentTimeMillis()
) 