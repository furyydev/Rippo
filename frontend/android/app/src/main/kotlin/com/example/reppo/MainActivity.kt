package com.example.reppo

import android.content.Intent
import android.net.Uri
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private var browserChannel: MethodChannel? = null
    private var pendingAuthUri: String? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        browserChannel = MethodChannel(
            flutterEngine.dartExecutor.binaryMessenger,
            "com.example.reppo/external_browser",
        )

        pendingAuthUri = intent?.dataString

        browserChannel?.setMethodCallHandler { call, result ->
            when (call.method) {
                "launch" -> {
                    val url = call.argument<String>("url")
                    if (url.isNullOrBlank()) {
                        result.error("INVALID_URL", "URL is required", null)
                        return@setMethodCallHandler
                    }
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("LAUNCH_FAILED", e.message, null)
                    }
                }
                "getInitialAuthUri" -> {
                    result.success(pendingAuthUri)
                    pendingAuthUri = null
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val authUri = intent.dataString ?: return
        browserChannel?.invokeMethod("authCallback", authUri)
    }
}
