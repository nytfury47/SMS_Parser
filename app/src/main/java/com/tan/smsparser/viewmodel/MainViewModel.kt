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

import java.text.SimpleDateFormat
import java.util.Locale
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
    val uiLastSyncTextLiveData = MutableLiveData<String>()

    //endregion

    init {
        uiSyncResultTextLiveData.postValue("")
        uiLastSyncTextLiveData.postValue("")
    }

    //region Private functions

    // Retrieve and add target SMS to list
    private fun getTargetSMSList(context: Context, targetSMSList: ArrayList<String>) {
        try {
            val columnBody = "body"
            val targetKeys = arrayOf(columnBody)
            val targetSelection = "address = ? COLLATE NOCASE AND date > cast(? as LONG)"
            val targetSelectionArgs = arrayOf(Constants.TARGET_SMS_SENDER, AppPreferences.lastSyncDate.toString())
            val targetUri = "content://sms/inbox"
            val uri = Uri.parse(targetUri)
            val cur = context.contentResolver.query(uri, targetKeys, targetSelection, targetSelectionArgs, null)

            if (cur != null && cur.moveToFirst()) {
                val indexBody = cur.getColumnIndex(columnBody)

                do {
                    val smsBody = cur.getString(indexBody)
                    targetSMSList.add(smsBody)
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

                // Update Sync Result
                uiSyncResultTextLiveData.postValue(syncResult)
                // Update Last Sync Date
                uiLastSyncTextLiveData.postValue(getCurrentDateTime(AppPreferences.lastSyncDate))
            }
        }
    }

    // Converting timestamp to presentable Date/Time
    private fun getCurrentDateTime(timeInMillis: Long): String = SimpleDateFormat("MM/dd/yyyy HH:mm:ss",
        Locale.getDefault()).format(timeInMillis)

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