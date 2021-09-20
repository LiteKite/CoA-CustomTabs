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
import androidx.lifecycle.OnLifecycleEvent
import com.litekite.customtabs.R
import com.litekite.customtabs.app.ClientApp
import com.litekite.customtabs.customtabs.CustomTabsFallback
import com.litekite.customtabs.util.ContextUtil
import com.litekite.customtabs.customtabs.CustomTabsServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * @author Vignesh S
 * @version 1.0, 14/04/2021
 * @since 1.0
 */
@HiltViewModel
class MainVM @Inject constructor(
    application: Application,
    private val customTabsServiceController: CustomTabsServiceController
) : AndroidViewModel(application),
    LifecycleObserver,
    CustomTabsServiceController.CustomTabsServiceCallback {

    val customTabsStatus: ObservableField<String> = ObservableField()
    private val _customTabsReady: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val customTabsReady: StateFlow<Boolean> = _customTabsReady
    private val applicationContext = getApplication() as ClientApp

    init {
        customTabsStatus.set(applicationContext.getString(R.string.custom_tabs_not_connected))
    }

    fun onClick(v: View) {
        when (v.id) {
            R.id.b_start_custom_tabs -> {
                val activityContext = ContextUtil.getActivity(v.context)
                if (activityContext != null) {
                    customTabsServiceController.setActivityContext(activityContext)
                }
                customTabsServiceController.startNewSession(
                    CustomTabsServiceController.SAMPLE_URI,
                    CustomTabsFallback()
                )
            }
        }
    }

    override fun onCustomTabsReady() {
        customTabsStatus.set(applicationContext.getString(R.string.custom_tabs_ready))
        _customTabsReady.value = true
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        customTabsServiceController.addCallback(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        customTabsServiceController.setActivityContext(null)
        customTabsServiceController.removeCallback(this)
    }
}
