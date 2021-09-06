/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.provider.SyncStateContract.Helpers.insert
import android.provider.SyncStateContract.Helpers.update
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

//      TODO (01) Create a viewModelJob and override onCleared() for canceling coroutine.
        private var viewModelJob = Job()

        override fun onCleared() {
                super.onCleared()
                viewModelJob.cancel()
        }

//      TODO (02) Define a scope for the coroutine to run in.
        private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

//      TODO (03) Create tonight Live data var and use a coroutine to initialize it from the database.
        private val tonight = MutableLiveData<SleepNight?>()

//      TODO (04) Get all nights from the database.
        private val nights = database.getAllNights()

        val nightsString = Transformations.map(nights) { nights ->
                formatNights(nights, application.resources)
        }

        // TODO (06) Using the familiar pattern create encapsualated showSnackBarEvent variable.
        // and doneShowingSnackBar() function.
        val startButtonVisible = Transformations.map(tonight){
                null == it
        }

        val stopButtonVisible = Transformations.map(tonight) {
                null != it
        }

        val clearButtonVisible = Transformations.map(nights) {
                it?.isNotEmpty()
        }

        // TODO (07) In onClear(), set the value of _showOnSnackbarEvent to true.
        private var _showSnackbarEvent = MutableLiveData<Boolean>()
        val showSnackbarEvent: LiveData<Boolean>
                get() = _showSnackbarEvent

        fun doneShowingSnackBar() {
                _showSnackbarEvent.value = false
        }

        private val _navigateToSleepQuality = MutableLiveData<SleepNight>()

        val navigateToSleepQuality: LiveData<SleepNight>
        get() = _navigateToSleepQuality

        fun doneNavigating() {
                _navigateToSleepQuality.value = null
        }

        init {
            initializeTonight()
        }

        private fun initializeTonight() {
                uiScope.launch {
                        tonight.value = getTonightFromDatabase()
                }
        }

        private suspend fun getTonightFromDatabase(): SleepNight? {
                return withContext(Dispatchers.IO) {
                        var night = database.getTonight()
                        if (night?.endTimeMilli != night?.startTimeMilli){
                                night = null
                        }
                        night
                }
        }

//      TODO (05) Add local functions for insert(), update(), and clear().
//      TODO (06) Implement handlers for Start, Stop,and Clear button using coroutines to do the database work.
        fun onStartTracking(){
                uiScope.launch {

                        val newNight = SleepNight()

                        insert(newNight)

                        tonight.value = getTonightFromDatabase()
                }
        }

        private suspend fun insert(night: SleepNight) {
                withContext(Dispatchers.IO) {
                        database.insert(night)
                }
        }

        fun onStopTracking() {
                uiScope.launch {
                        val oldNight = tonight.value ?: return@launch

                        oldNight.endTimeMilli = System.currentTimeMillis()

                        update(oldNight)
                        _navigateToSleepQuality.value = oldNight
                }
        }

        private suspend fun update(night: SleepNight) {
                withContext(Dispatchers.IO) {
                        database.update(night)
                }
        }

        fun onClear() {
                uiScope.launch {
                        clear()
                        tonight.value = null
                }
        }

        suspend fun clear() {
                withContext(Dispatchers.IO) {
                        database.clear()
                }
        }
//      TODO (09) Transfer nights into a nightString using formatNights().
}

