package com.tan.smsparser.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels

import com.afollestad.assent.GrantResult
import com.afollestad.assent.Permission
import com.afollestad.assent.askForPermissions
import com.afollestad.materialdialogs.MaterialDialog

import com.tan.smsparser.databinding.FragmentMainBinding
import com.tan.smsparser.viewmodel.MainViewModel

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

        setupClickListeners()
        fragmentTextUpdateObserver()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //endregion

    //region Private functions

    // Setup the button in our fragment to call getUpdatedText method in viewModel
    private fun setupClickListeners() {
        binding.fragmentButton.setOnClickListener { readSMS() }
    }

    // Observer is waiting for viewModel to update our UI
    private fun fragmentTextUpdateObserver() {
        viewModel.uiTextLiveData.observe(viewLifecycleOwner, { updatedText ->
            binding.fragmentTextView.text = updatedText
        })
    }

    private fun readSMS() {
        // Perform viewModel action only when READ_SMS permission is granted
        // If READ_SMS permission is not granted, ask user to grant permission from Settings page
        askForPermissions(PERMISSION_READ_SMS) { result ->
            if (result[PERMISSION_READ_SMS] == GrantResult.GRANTED) {
                viewModel.getUpdatedText()
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

    //endregion

    companion object {
        private val PERMISSION_READ_SMS = Permission.READ_SMS
        private const val DIALOG_TITLE_TEXT = "Permission to read SMS"
        private const val DIALOG_MESSAGE_TEXT = "Please grant this app the permission to read SMS from the Settings page."
        private const val DIALOG_POSITIVE_BUTTON_TEXT = "OK"
        private const val DIALOG_NEGATIVE_BUTTON_TEXT = "Cancel"
    }
}