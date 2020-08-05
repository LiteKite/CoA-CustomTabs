/*
 * Copyright 2020 LiteKite Startup. All rights reserved.
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

package com.litekite.customtabs

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap

/**
 * @author Vignesh S
 * @version 1.0, 08/05/2020
 * @since 1.0
 */
class ChromiumServiceController(
	private val context: Context,
	private val chromiumServiceCallback: ChromiumServiceCallback
) {

	companion object {

		const val TAG = "ChromiumServiceController"

		// Package name for the Chrome channel the client wants to connect to. This depends on the
		// channel name.
		// Stable = com.android.chrome
		// Beta = com.chrome.beta
		// Dev = com.chrome.dev
		const val CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome" // Change when in stable

		lateinit var chromiumServiceController: ChromiumServiceController
	}

	private var customTabsSession: CustomTabsSession? = null
	private lateinit var customTabsClient: CustomTabsClient
	private var serviceConnected: Boolean = false

	private var connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
		override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
			serviceConnected = true
			Log.d(TAG, "onCustomTabsServiceConnected: $serviceConnected")
			customTabsClient = client
			val ok = customTabsClient.warmup(0)
			Log.d(TAG, "onCustomTabsServiceConnected: warm up: $ok")
			if (ok) chromiumServiceCallback.onChromiumWebReady()
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
			Log.d(TAG, "onMessageChannelReady: extras: ${extras.toString()}")
		}
	}

	init {
		CustomTabsClient.bindCustomTabsService(context, CUSTOM_TAB_PACKAGE_NAME, connection)
		chromiumServiceController = this
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
		builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
		// Sets SystemUI Navigation Bar Color
		builder.setNavigationBarColor(ContextCompat.getColor(context, R.color.colorPrimary))
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
		customTabsIntent.launchUrl(context, Uri.parse(url))
	}

	fun tearDown() {
		context.unbindService(connection)
	}

	class CustomTabsActionsReceiver : BroadcastReceiver() {

		override fun onReceive(context: Context?, intent: Intent?) {
			Log.d(TAG, "customTabsActionsReceiver: onReceive: ${intent?.action}")
			chromiumServiceController.startNewSession()
		}

	}

	interface ChromiumServiceCallback {
		fun onChromiumWebReady()
	}

}