package com.tan.smsparser.viewmodel

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.tan.smsparser.model.DataModel

import org.json.JSONArray

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

    //region Public functions

    // Read all SMS from GCash
    fun readSMS(context: Context) {
        val messageList = ArrayList<String>()

        try {
            val uri = Uri.parse(SMS_URI_INBOX)
            val cur = context.contentResolver.query(uri, CURSOR_QUERY_PROJECTION, CURSOR_QUERY_SELECTION, null, CURSOR_QUERY_SORT_ORDER)

            if (cur != null) {
                if (cur.moveToFirst()) {
                    //val indexAddress = cur.getColumnIndex("address")
                    //val indexPerson = cur.getColumnIndex("person")
                    val indexBody = cur.getColumnIndex("body")
                    //val indexDate = cur.getColumnIndex("date")
                    //val indexType = cur.getColumnIndex("type")

                    do {
                        //val strAddress = cur.getString(index_Address)
                        //val intPerson = cur.getInt(index_Person)
                        val strBody = cur.getString(indexBody)
                        //val longDate = cur.getLong(index_Date)
                        //val int_Type = cur.getInt(index_Type)

                        messageList.add(strBody)
                    } while (cur.moveToNext())

                    if (!cur.isClosed) {
                        cur.close()
                        //cur = null
                    }
                }
            }
        } catch (ex: SQLiteException) {
            ex.message?.let { Log.d("SQLiteException", it) }
        }

        val jsonArray = JSONArray(messageList)
        val updatedText = model.textForUI
        uiTextLiveData.postValue(jsonArray.toString())
    }

    //endregion
    companion object {
        private const val SMS_URI_INBOX = "content://sms/inbox"
        private val CURSOR_QUERY_PROJECTION = arrayOf("_id", "address", "person", "body", "date", "type")
        //private const val CURSOR_QUERY_SELECTION = "address='GCash' OR address='GCASH' OR address='gcash'"
        private const val CURSOR_QUERY_SELECTION = "address='FBS'"
        private const val CURSOR_QUERY_SORT_ORDER = "date asc"
    }
}