package com.zoho.assist.cobrowse.demo

import com.zoho.assist.cobrowse.AssistCobrowseApplication
import com.zoho.assist.cobrowse.CobrowseSession

/**
 * Extends [AssistCobrowseApplication] because the SDK requires it: it needs
 * process-wide hooks to track the foreground Activity (whose `decorView` is the
 * capture target), tell backgrounding from a configuration change, and receive
 * `onConfigurationChanged`.
 */
class BankApplication : AssistCobrowseApplication() {
    override fun onCreate() {
        super.onCreate()

        // Process-scoped so the session survives Activity recreation.
        CobrowseSession.INSTANCE.setContext(this)

        // Your widget code and the domain it was issued for (e.g. assist.zoho.in)
        // go here. Never commit a real widget code: this repo is public. A
        // production app fetches it from its backend after login instead, since
        // anything compiled into the APK can be extracted.
        CobrowseController.widgetCode = ""
        CobrowseController.domain = ""
        // Optional: the website the agent sees the session coming from
        // (www.example.com or an https URL). Blank leaves assist.zoho.com.
        CobrowseController.websiteDomain = ""
    }
}

