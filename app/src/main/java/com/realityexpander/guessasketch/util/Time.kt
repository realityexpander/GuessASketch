package com.realityexpander.guessasketch.util

import java.text.SimpleDateFormat
import java.util.*

fun Long.toTimeString(): String {
    val dateTime = Date(this)
    val format = SimpleDateFormat("HH:mm:ss", Locale.US)
    return format.format(dateTime)
}

fun Long.toTimeDateString(): String {
    val dateTime = Date(this)
    val format = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.US)
    return format.format(dateTime)
}

fun String.toTimeDateLong(): Long {
    val format = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.US)
    return format.parse(this)?.time ?: throw IllegalArgumentException("Invalid time string")
}