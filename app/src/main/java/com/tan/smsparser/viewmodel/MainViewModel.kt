package com.tan.smsparser.viewmodel

import android.content.Context
import android.database.sqlite.SQLiteException
import android.net.Uri
import android.util.Log

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import com.tan.smsparser.Constants
import com.tan.smsparser.data.local.AppPreferences
import com.tan.smsparser.data.remote.APIService

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import org.json.JSONArray

import retrofit2.Retrofit

import java.util.Calendar

/*
 *      MainViewModel
 *      - viewModel that updates the MainFragment (the visible UI)
 *      - gets the data from model
 */
class MainViewModel: ViewModel() {

    //region Public properties

    // Create MutableLiveData which MainFragment can subscribe to
    // When this data changes, it triggers the UI to do an update
    val uiSyncResultTextLiveData = MutableLiveData<String>()

    //endregion

    //region Private functions

    // Retrieve and add target SMS to list
    private fun getTargetSMSList(context: Context, targetSMSList: ArrayList<String>) {
        try {
            val columnDate = "date"
            val columnBody = "body"
            val targetFields = arrayOf(columnDate, columnBody)
            val targetSelection = "address='${Constants.TARGET_SMS_SENDER}' COLLATE NOCASE"
            val targetUri = "content://sms/inbox"
            val uri = Uri.parse(targetUri)
            val cur = context.contentResolver.query(uri, targetFields, targetSelection, null, null)

            if (cur != null && cur.moveToFirst()) {
                val indexDate = cur.getColumnIndex(columnDate)
                val indexBody = cur.getColumnIndex(columnBody)
                val lastSyncDate = AppPreferences.lastSyncDate

                do {
                    val smsDate = cur.getLong(indexDate)
                    val smsBody = cur.getString(indexBody)

                    if (lastSyncDate < smsDate) {
                        targetSMSList.add(smsBody)
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

    private fun postToServer(targetSMSList: ArrayList<String>) {
        // Create Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .build()

        // Create Service
        val service = retrofit.create(APIService::class.java)

        // Create JSONArray string for requestBody
        val jsonArrayString = JSONArray(targetSMSList).toString()

        // Create RequestBody ( We're not using any converter, like GsonConverter, MoshiConverter e.t.c, that's why we use RequestBody )
        val requestBody = jsonArrayString.toRequestBody("application/json".toMediaTypeOrNull())

        CoroutineScope(Dispatchers.IO).launch {
            // Do the POST request and get response
            val response = service.createSMSList(requestBody)

            withContext(Dispatchers.Main) {
                var syncResult = "Success!"

                if (response.isSuccessful) {
                    // If post is successful, update last successful sync time
                    AppPreferences.lastSyncDate = Calendar.getInstance().timeInMillis
                } else {
                    syncResult = "Error ${response.code()} - ${response.message()}"
                }

                // Update Sync Result / Last Sync Time
                uiSyncResultTextLiveData.postValue(syncResult)
            }
        }
    }

    //endregion

    //region Public functions

    fun syncSMS(context: Context) {
        val targetSMSList = ArrayList<String>()

        // get target sms list
        getTargetSMSList(context, targetSMSList)

        // send to target sms list to server
        postToServer(targetSMSList)
    }

    //endregion
}