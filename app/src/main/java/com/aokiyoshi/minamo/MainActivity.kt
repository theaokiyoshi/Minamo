package com.aokiyoshi.minamo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Modern Android file picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                filePathCallback?.onReceiveValue(arrayOf(uri))
            } else {
                // Check for multiple selected files if enabled
                val clipData = result.data?.clipData
                if (clipData != null) {
                    val uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    filePathCallback?.onReceiveValue(uris)
                } else {
                    filePathCallback?.onReceiveValue(null)
                }
            }
        } else {
            // Cancelled selection: MUST send null to unblock the WebView input
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).apply {
            // Prevents the white screen flash on startup by matching a dark theme background
            setBackgroundColor(Color.BLACK)

            // Hides native Android scrollbars and edge glow effects
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
            }

            webViewClient = WebViewClient()

            // Handles system popups, file choosers, and JS dialogs
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    // Cancel any existing callback
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*" // Allows importing any file type (JSON, XML, images, backup files)
                    }

                    return try {
                        filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
                        true
                    } catch (e: Exception) {
                        this@MainActivity.filePathCallback = null
                        false
                    }
                }
            }

            loadUrl("file:///android_asset/index.html")
        }

        setContentView(webView)

        // Back button: go back inside the app first, close only when there is no history
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }
}