package com.tan.smsparser.viewmodel

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.tan.smsparser.model.DataModel
import com.tan.smsparser.model.local.AppPreferences

import org.json.JSONArray

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

/*
 *      MainViewModel
 *      - viewModel that updates the MainFragment (the visible UI)
 *      - gets the data from model
 */
class MainViewModel: ViewModel() {

    //region Private properties

    // Create the model which contains data for our UI
    private val model = DataModel(textForUI = "Here's the updated text!")

    //endregion

    //region Public properties

    // Create MutableLiveData which MainFragment can subscribe to
    // When this data changes, it triggers the UI to do an update
    val uiTextLiveData = MutableLiveData<String>()

    //endregion

    //region Private functions

    // Retrieve and add target SMS to list
    private fun getTargetSMSList(context: Context, targetSMSList: ArrayList<String>) {
        try {
            val columnDate = "date"
            val columnBody = "body"
            val targetFields = arrayOf(columnDate, columnBody)
            val targetSelection = "address='$TARGET_SMS_SENDER' COLLATE NOCASE"
            val targetUri = "content://sms/inbox"
            val uri = Uri.parse(targetUri)
            val cur = context.contentResolver.query(uri, targetFields, targetSelection, null, null)

            if (cur != null && cur.moveToFirst()) {
                val indexDate = cur.getColumnIndex(columnDate)
                val indexBody = cur.getColumnIndex(columnBody)
                val lastSyncDate = AppPreferences.lastSyncDate

                do {
                    val longDate = cur.getLong(indexDate)
                    val strBody = cur.getString(indexBody)

                    if (lastSyncDate == 0L || lastSyncDate <= longDate) {
                        targetSMSList.add(strBody)
                    }
                } while (cur.moveToNext())

                if (!cur.isClosed) {
                    cur.close()
                    //cur = null
                }
            }
        } catch (ex: SQLiteException) {
            ex.message?.let { Log.d("SQLiteException", it) }
        }
    }

    /**
     * Helper function for converting timestamp to presentable Date/Time
     */
    private fun getCurrentDateTime(timeInMillis: Long) = SimpleDateFormat("MM/dd/yyyy HH:mm:ss",
            Locale.getDefault()).format(timeInMillis)

    //endregion

    //region Public functions

    fun syncSMS(context: Context) {
        val targetSMSList = ArrayList<String>()

        // get target sms list
        getTargetSMSList(context, targetSMSList)

        // send to target sms list to server


        // if server returns 201, save last sync time
        AppPreferences.lastSyncDate = Calendar.getInstance().timeInMillis

        val jsonArray = JSONArray(targetSMSList)
        val updatedText = model.textForUI
        //uiTextLiveData.postValue(jsonArray.toString())
        uiTextLiveData.postValue(jsonArray.toString() + "\n" + getCurrentDateTime(AppPreferences.lastSyncDate))
    }

    //endregion

    companion object {
        private const val TARGET_SMS_SENDER = "fbs"
    }
}