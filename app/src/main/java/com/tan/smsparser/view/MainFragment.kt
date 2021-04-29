package com.tan.smsparser.view

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import com.afollestad.assent.GrantResult
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.materialdialogs.MaterialDialog

import com.tan.smsparser.R
import com.tan.smsparser.data.local.AppPreferences
import com.tan.smsparser.databinding.FragmentMainBinding
import com.tan.smsparser.viewmodel.MainViewModel

import java.text.SimpleDateFormat
import java.util.Locale

/*
 *      MainFragment
 *      - shows the UI
 *      - listens to viewModel for updates on UI
 */
class MainFragment: Fragment() {

    //region Private properties

    // View Binding
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    // Create a viewModel
    private val viewModel: MainViewModel by activityViewModels()

    //endregion

    //region View Life Cycle

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSubviews()
        setupClickListeners()
        fragmentTextUpdateObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Private functions

    private fun initSubviews() {
        val lastSyncTime = getCurrentDateTime(AppPreferences.lastSyncDate)

        binding.progressBar.visibility = View.INVISIBLE
        binding.fragmentSyncResultTextView.visibility = View.GONE
        binding.fragmentLastSyncTextView.text = String.format(getString(R.string.last_successful_sync_text_view), lastSyncTime)
    }

    // Setup the button in our fragment to call syncSMS method in viewModel
    private fun setupClickListeners() {
        binding.fragmentSyncButton.setOnClickListener { syncSMS() }
    }

    // Observer is waiting for viewModel to update our UI
    private fun fragmentTextUpdateObserver() {
        viewModel.uiSyncResultTextLiveData.observe(viewLifecycleOwner, { updatedText ->
            val lastSyncTime = getCurrentDateTime(AppPreferences.lastSyncDate)

            toggleSyncButton(true)

            binding.fragmentSyncResultTextView.visibility = View.VISIBLE
            binding.fragmentSyncResultTextView.text = String.format(getString(R.string.sync_result_text_view), updatedText)

            binding.fragmentLastSyncTextView.text = String.format(getString(R.string.last_successful_sync_text_view), lastSyncTime)
        })
    }

    private fun syncSMS() {
        // Internet must be available
        if (!isInternetAvailable()) {
            Toast.makeText(context, R.string.check_internet, Toast.LENGTH_SHORT).show()
            return
        }

        // Perform viewModel action only when READ_SMS permission is granted
        // If READ_SMS permission is not granted, ask user to grant permission from Settings page
        askForPermissions(PERMISSION_READ_SMS) { result ->
            if (result[PERMISSION_READ_SMS] == GrantResult.GRANTED) {
                context?.let {
                    toggleSyncButton(false)
                    viewModel.syncSMS(it)
                }
            } else {
                requestForPermissions()
            }
        }
    }

    private fun requestForPermissions() {
        activity?.let {
            MaterialDialog(it).show {
                title(text = DIALOG_TITLE_TEXT)
                message(text = DIALOG_MESSAGE_TEXT)
                positiveButton(text = DIALOG_POSITIVE_BUTTON_TEXT) {
                    openAppSystemSettings()
                }
                negativeButton(text = DIALOG_NEGATIVE_BUTTON_TEXT) {
                    // Do nothing
                }
            }
        }
    }

    private fun openAppSystemSettings() {
        activity?.let {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", it.packageName, null)
            })
        }
    }

    // Switch on/off sync button and progress bar
    private fun toggleSyncButton(enable: Boolean) {
        binding.fragmentSyncButton.isEnabled = enable
        binding.progressBar.visibility = if (enable) View.INVISIBLE else View.VISIBLE
    }

    //endregion

    //region Utility functions

    // Converting timestamp to presentable Date/Time
    private fun getCurrentDateTime(timeInMillis: Long): String = SimpleDateFormat("MM/dd/yyyy HH:mm:ss",
        Locale.getDefault()).format(timeInMillis)

    // Check for internet connection
    @Suppress("DEPRECATION")
    private fun isInternetAvailable(): Boolean {
        var result = false
        val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI)        -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)    -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)    -> true
                        else                                                    -> false
                    }
                }
            }
        } else {
            val networkInfo = cm.activeNetworkInfo
            result = (networkInfo !=null && networkInfo.isConnected)
        }

        return result
    }

    //endregion

    companion object {
        private val PERMISSION_READ_SMS = Permission.READ_SMS
        private const val DIALOG_TITLE_TEXT = "Permission to read SMS"
        private const val DIALOG_MESSAGE_TEXT = "Please grant this app the permission to read SMS from the Settings page."
        private const val DIALOG_POSITIVE_BUTTON_TEXT = "OK"
        private const val DIALOG_NEGATIVE_BUTTON_TEXT = "Cancel"
    }
}