package com.tan.smsparser.util

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility function for converting timestamp to presentable Date/Time
 */
fun getCurrentDateTime(timeInMillis: Long): String = SimpleDateFormat("MM/dd/yyyy HH:mm:ss",
    Locale.getDefault()).format(timeInMillis)