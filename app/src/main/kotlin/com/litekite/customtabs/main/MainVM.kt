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
package com.litekite.customtabs.main

import android.app.Application
import android.view.View
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.litekite.customtabs.R
import com.litekite.customtabs.app.ClientApp
import com.litekite.customtabs.chromium.ChromiumServiceController
import com.litekite.customtabs.util.ContextUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @author Vignesh S
 * @version 1.0, 14/04/2021
 * @since 1.0
 */
@HiltViewModel
class MainVM @Inject constructor(
    application: Application,
    private val chromiumServiceController: ChromiumServiceController
) : AndroidViewModel(application),
    LifecycleObserver,
    ChromiumServiceController.ChromiumServiceCallback {

    val chromiumStatus: ObservableField<String> = ObservableField()
    val chromiumReady: MutableLiveData<Boolean> = MutableLiveData()
    private val applicationContext = getApplication() as ClientApp

    init {
        chromiumStatus.set(applicationContext.getString(R.string.chromium_not_connected))
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.b_start_chromium_web -> {
                val activityContext = ContextUtil.getActivity(v.context)
                if (activityContext != null) {
                    chromiumServiceController.setActivityContext(activityContext)
                }
                chromiumServiceController.startNewSession()
            }
        }
    }

    override fun onChromiumWebReady() {
        chromiumStatus.set(applicationContext.getString(R.string.chromium_ready))
        chromiumReady.value = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        chromiumServiceController.addCallback(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        chromiumServiceController.setActivityContext(null)
        chromiumServiceController.removeCallback(this)
    }
}
