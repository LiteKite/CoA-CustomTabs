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
package com.litekite.customtabs.customtabs

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
 *
 * @see [https://github.com/GoogleChrome/android-browser-helper]
 */
@Singleton
class CustomTabsServiceController @Inject constructor(private val context: Context) :
    CallbackProvider<CustomTabsServiceController.CustomTabsServiceCallback> {

    companion object {

        const val TAG = "CustomTabsServiceController"

        val SAMPLE_URI: Uri = Uri.parse("https://www.google.com")
    }

    override val callbacks: ArrayList<CustomTabsServiceCallback> = ArrayList()

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
                callbacks.forEach { it.onCustomTabsReady() }
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
        val packageName = CustomTabsPackageProvider.getPackageNameToUse(context)
        if (packageName != null && !serviceConnected) {
            CustomTabsClient.bindCustomTabsService(context, packageName, connection)
            Runtime.getRuntime().addShutdownHook(Thread { tearDown() })
        }
    }

    fun setActivityContext(activityContext: Activity?) {
        this.activityContext = activityContext
    }

    fun startNewSession(uri: Uri, fallback: CustomTabsFallback) {
        val packageName = CustomTabsPackageProvider.getPackageNameToUse(context)
        if (packageName == null && !serviceConnected) {
            fallback.openUri(context, uri)
            return
        }
        customTabsSession = customTabsClient.newSession(customTabsCallback)
        launchCustomTabBrowser(uri)
    }

    private fun launchCustomTabBrowser(uri: Uri) {
        // Use a CustomTabsIntent.Builder to configure CustomTabsIntent.
        // Once ready, call CustomTabsIntent.Builder.build() to create a CustomTabsIntent
        // and launch the desired Url with CustomTabsIntent.launchUrl()
        val builder = CustomTabsIntent.Builder(customTabsSession)
        // Changes the background color. colorInt is an int that specifies a Color.
        val params = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .build()
        builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_LIGHT, params)
        // This creates an action that is performed when the button pressed on the custom tabs
        // activity.
        val actionPendingIntent = Intent(context, CustomTabsActionsReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            actionPendingIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val tabBitmapDrawable =
            ContextCompat.getDrawable(context, R.drawable.ic_add_tab)?.toBitmap()
        // Adds action button to custom tabs activity
        tabBitmapDrawable?.let {
            builder.setActionButton(tabBitmapDrawable, "adds tab", pendingIntent, false)
        }
        // Adds menu option item to custom tabs activity
        builder.addMenuItem("Add Tab", pendingIntent)
        // CustomTabsIntent is the one that loads your url in a custom tabs.
        val customTabsIntent = builder.build()
        CustomTabsPackageProvider.addKeepAliveExtra(context, customTabsIntent.intent)
        customTabsIntent.intent.`package` = CustomTabsPackageProvider.getPackageNameToUse(context)
        if (activityContext != null) {
            customTabsIntent.launchUrl(activityContext as Context, uri)
        }
    }

    override fun addCallback(cb: CustomTabsServiceCallback) {
        super.addCallback(cb)
        if (serviceConnected) {
            callbacks.forEach { it.onCustomTabsReady() }
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
        lateinit var customTabsServiceController: CustomTabsServiceController

        override fun onReceive(context: Context, intent: Intent?) {
            Log.d(TAG, "customTabsActionsReceiver: onReceive: ${intent?.action}")
            customTabsServiceController.startNewSession(SAMPLE_URI, CustomTabsFallback())
        }
    }

    interface CustomTabsServiceCallback {
        fun onCustomTabsReady()
    }

    /**
     * To be used as a fallback to open the Uri when Custom Tabs is not available.
     */
    interface Fallback {
        /**
         *
         * @param context The context that wants to open the Uri.
         * @param uri The uri to be opened by the fallback.
         */
        fun openUri(context: Context, uri: Uri)
    }
}
