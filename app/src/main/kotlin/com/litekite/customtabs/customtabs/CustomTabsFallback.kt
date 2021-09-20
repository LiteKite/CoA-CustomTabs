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

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.litekite.customtabs.webview.WebViewActivity

/**
 * A Fallback that opens a WebView when Custom Tabs is not available
 */
class CustomTabsFallback : CustomTabsServiceController.Fallback {

    override fun openUri(context: Context, uri: Uri) {
        val intent = Intent(context, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, uri.toString())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
