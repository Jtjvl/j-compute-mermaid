package com.jcompute.mermaidpocket.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * [OpenDocument] starting at the granted library when the provider honors
 * [DocumentsContract.EXTRA_INITIAL_URI]. System Back still walks SAF folders;
 * the in-app [LibraryOpenDialog] is the Back-to-app path.
 */
class OpenDiagramContract(
    private val initialUri: () -> Uri?,
) : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            initialUri()?.let { uri ->
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
    }
}
