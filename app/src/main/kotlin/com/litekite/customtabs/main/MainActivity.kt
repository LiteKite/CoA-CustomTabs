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

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.litekite.customtabs.R
import com.litekite.customtabs.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * @author Vignesh S
 * @version 1.0, 08/05/2020
 * @since 1.0
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var uiWork: Job? = null
    private lateinit var binding: ActivityMainBinding
    private val mainVM: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        init()
    }

    private fun init() {
        binding.presenter = mainVM
        lifecycle.addObserver(mainVM)
    }

    override fun onStart() {
        super.onStart()
        uiWork = lifecycleScope.launch {
            mainVM.chromiumReady.collect { isReady ->
                if (isReady) {
                    binding.bStartChromiumWeb.isEnabled = true
                }
            }
        }
    }

    override fun onStop() {
        uiWork?.cancel()
        super.onStop()
    }
}
