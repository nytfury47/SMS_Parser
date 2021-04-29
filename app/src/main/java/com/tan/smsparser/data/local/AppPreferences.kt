package com.tan.smsparser.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Another persistence mechanism (SharedPreferences) for saving and reusing data within the app,
 * using Singleton design pattern.
 */
object AppPreferences {
    private const val SHARED_PREFS_NAME = "SMS_Parser_Preferences"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var kPrefs: SharedPreferences

    // App-specific preferences and their default values
    private val kLastSyncDate = Pair("LastSyncDate", 0L)

    // Timestamp for last successful sync
    var lastSyncDate: Long
        // custom getter to get a preference of a desired type, with a predefined default value
        get() { return kPrefs.getLong(kLastSyncDate.first, kLastSyncDate.second) }
        // custom setter to save a preference back to preferences file
        set(value) = kPrefs.edit { it.putLong(kLastSyncDate.first, value) }

    /**
     * Initialize AppPreferences
     */
    fun init(context: Context) {
        kPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, MODE)
    }

    /**
     * SharedPreferences extension function, so we won't need to call edit() and apply()
     * ourselves on every SharedPreferences operation.
     */
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }
}