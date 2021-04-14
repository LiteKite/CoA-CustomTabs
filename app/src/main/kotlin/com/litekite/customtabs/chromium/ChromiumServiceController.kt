/*
 * Copyright 2021 LiteKite Startup. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.litekite.customtabs.chromium

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.litekite.customtabs.R
import com.litekite.customtabs.base.CallbackProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * @author Vignesh S
 * @version 1.0, 08/05/2020
 * @since 1.0
 */
@Singleton
class ChromiumServiceController @Inject constructor(private val context: Context) :
    CallbackProvider<ChromiumServiceController.ChromiumServiceCallback> {

    companion object {

        const val TAG = "ChromiumServiceController"

        // Package name for the Chrome channel the client wants to connect to. This depends on the
        // channel name.
        // Stable = com.android.chrome
        // Beta = com.chrome.beta
        // Dev = com.chrome.dev
        const val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome" // Change when in stable
    }

    override val callbacks: ArrayList<ChromiumServiceCallback> = ArrayList()

    private var customTabsSession: CustomTabsSession? = null
    private lateinit var customTabsClient: CustomTabsClient
    var serviceConnected: Boolean = false
    @Volatile
    private var activityContext: Activity? = null

    private var connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
        override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
            serviceConnected = true
            Log.d(TAG, "onCustomTabsServiceConnected: $serviceConnected")
            customTabsClient = client
            val ok = customTabsClient.warmup(0)
            Log.d(TAG, "onCustomTabsServiceConnected: warm up: $ok")
            if (ok) {
                callbacks.forEach { it.onChromiumWebReady() }
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            serviceConnected = false
            Log.d(TAG, "onServiceDisconnected: $serviceConnected")
        }
    }

    private val customTabsCallback = object : CustomTabsCallback() {
        override fun onRelationshipValidationResult(
            relation: Int,
            requestedOrigin: Uri,
            validated: Boolean,
            extras: Bundle?
        ) {
            super.onRelationshipValidationResult(relation, requestedOrigin, validated, extras)
            Log.d(TAG, "onRelationshipValidationResult: isValidated: $validated")
        }

        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            super.onNavigationEvent(navigationEvent, extras)
            Log.d(TAG, "onRelationshipValidationResult: navigationEvent: $navigationEvent")
        }

        override fun extraCallback(callbackName: String, args: Bundle?) {
            super.extraCallback(callbackName, args)
            Log.d(TAG, "extraCallback: callbackName: $callbackName")
        }

        override fun extraCallbackWithResult(callbackName: String, args: Bundle?): Bundle? {
            Log.d(TAG, "extraCallbackWithResult: callbackName: $callbackName")
            return super.extraCallbackWithResult(callbackName, args)
        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            super.onPostMessage(message, extras)
            Log.d(TAG, "onPostMessage: message: $message")
        }

        override fun onMessageChannelReady(extras: Bundle?) {
            super.onMessageChannelReady(extras)
            Log.d(TAG, "onMessageChannelReady: extras: $extras")
        }
    }

    init {
        CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME, connection)
        Runtime.getRuntime().addShutdownHook(Thread { tearDown() })
    }

    fun setActivityContext(activityContext: Activity?) {
        this.activityContext = activityContext
    }

    fun startNewSession() {
        if (!serviceConnected) return
        customTabsSession = customTabsClient.newSession(customTabsCallback)
        launchCustomTabBrowser()
    }

    private fun launchCustomTabBrowser() {
        // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
        // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
        // and launch the desired Url with CustomTabsIntent.launchUrl()
        val builder = CustomTabsIntent.Builder()
        // Changes the background color. colorInt is an int that specifies a Color.
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()
        builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, params)
        // This creates an action that is performed when the button pressed on the chromium
        // activity.
        val actionPendingIntent = Intent(context, CustomTabsActionsReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            actionPendingIntent,
            0
        )
        val tabBitmapDrawable =
            ContextCompat.getDrawable(context, R.drawable.ic_add_tab)?.toBitmap()
        // Adds Action Button to Chromium Activity
        tabBitmapDrawable?.let {
            builder.setActionButton(tabBitmapDrawable, "adds tab", pendingIntent, false)
        }
        // Adds Menu Option Item to Chromium Activity
        builder.addMenuItem("Add Tab", pendingIntent)
        customTabsSession?.let { builder.setSession(it) }
        // CustomTabsIntent is the one that loads your url in a custom tab.
        val customTabsIntent = builder.build()
        val url = "https://google.com"
        customTabsIntent.intent.`package` = CUSTOM_TAB_PACKAGE_NAME
        if (activityContext != null) {
            customTabsIntent.launchUrl(activityContext as Context, Uri.parse(url))
        }
    }

    override fun addCallback(cb: ChromiumServiceCallback) {
        super.addCallback(cb)
        if (serviceConnected) {
            callbacks.forEach { it.onChromiumWebReady() }
        }
    }

    private fun tearDown() {
        if (!serviceConnected) return
        context.unbindService(connection)
        activityContext = null
    }

    @AndroidEntryPoint
    class CustomTabsActionsReceiver : BroadcastReceiver() {

        @Inject
        lateinit var chromiumServiceController: ChromiumServiceController

        override fun onReceive(context: Context, intent: Intent?) {
            Log.d(TAG, "customTabsActionsReceiver: onReceive: ${intent?.action}")
            chromiumServiceController.startNewSession()
        }
    }

    interface ChromiumServiceCallback {
        fun onChromiumWebReady()
    }
}
