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

            // NEW in V1.0.2: lets the page save its backup file into Downloads.
            // The name "MinamoApp" must stay exactly like this: the page looks for it.
            // Keep this line BEFORE loadUrl below.
            addJavascriptInterface(MinamoBridge(this@MainActivity), "MinamoApp")

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

        // Back button and back gesture: ask the app to go one step back first
        // (close an anime page, filter screen, news card, window, or return to Discover).
        // The app answers "true" if it handled the press; otherwise there is nothing
        // left to close, so the activity finishes and the app exits.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(function(){try{return window.minamoBack?window.minamoBack()===true:false}catch(e){return false}})()"
                ) { handled ->
                    if (handled != "true") finish()
                }
            }
        })
    }
}