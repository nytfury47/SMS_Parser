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

    //region Private functions

    // Retrieve and add target SMS to list
    private fun getTargetSMSList(context: Context, targetSMSList: ArrayList<String>) {
        try {
            val targetUri = "content://sms/inbox"
            val targetFields = arrayOf("_id", "address", "person", "body", "date", "type")
            val targetSelection = "address='$TARGET_SMS_SENDER' COLLATE NOCASE"
            val uri = Uri.parse(targetUri)
            val cur = context.contentResolver.query(uri, targetFields, targetSelection, null, null)

            if (cur != null) {
                if (cur.moveToFirst()) {
                    //val indexAddress = cur.getColumnIndex("address")
                    //val indexPerson = cur.getColumnIndex("person")
                    val indexBody = cur.getColumnIndex("body")
                    //val indexDate = cur.getColumnIndex("date")
                    //val indexType = cur.getColumnIndex("type")

                    do {
                        //val strAddress = cur.getString(indexAddress)
                        //val intPerson = cur.getInt(indexPerson)
                        val strBody = cur.getString(indexBody)
                        //val longDate = cur.getLong(indexDate)
                        //val intType = cur.getInt(indexType)

                        targetSMSList.add(strBody)
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
    }

    //endregion

    //region Public functions

    fun syncSMS(context: Context) {
        val targetSMSList = ArrayList<String>()

        // get target sms list
        getTargetSMSList(context, targetSMSList)

        // send to target sms list to server


        // delete if 201

        val jsonArray = JSONArray(targetSMSList)
        val updatedText = model.textForUI
        uiTextLiveData.postValue(jsonArray.toString())
    }

    //endregion

    companion object {
        private const val TARGET_SMS_SENDER = "GCash"
    }
}