package com.porpoise.geocarching.Dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.porpoise.geocarching.R


class AddMarkerFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: AddMarkerDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface AddMarkerDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, cacheName: String, cacheDesc: String, cacheModel: String)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            listener = targetFragment as AddMarkerDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException("Calling Fragment must implement AddMarkerDialogListener")
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Build the dialog and set up the button click handlers
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            val dialogView: View = inflater.inflate(R.layout.dialog_add_marker, null)
            val nameTextView: EditText = dialogView.findViewById(R.id.add_marker_name)
            val descEditText: EditText = dialogView.findViewById(R.id.add_marker_desc)

            val spinner: Spinner = dialogView.findViewById(R.id.model_spinner)
            context?.let { safeContext ->
                ArrayAdapter.createFromResource(
                        safeContext,
                        R.array.cache_model_array,
                        android.R.layout.simple_spinner_item
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinner.adapter = adapter
                }
            }

            builder.setView(dialogView)
                    .setMessage(R.string.add_marker_dialog_message)
                    .setPositiveButton(R.string.dialog_positive
                    ) { dialog, id ->
                        // Send the positive button event back to the host activity
                        listener.onDialogPositiveClick(this, nameTextView.text.toString(), descEditText.text.toString(), spinner.selectedItem.toString())
                    }
                    .setNegativeButton(R.string.dialog_negative
                    ) { dialog, id ->
                        // Send the negative button event back to the host activity
                        listener.onDialogNegativeClick(this)
                    }

            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }
}
