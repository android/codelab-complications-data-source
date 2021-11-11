/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.complicationsdatasource

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.android.wearable.complicationsdatasource.data.TAP_COUNTER_PREF_KEY
import com.example.android.wearable.complicationsdatasource.data.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Simple [BroadcastReceiver] subclass for asynchronously incrementing an integer for any
 * complication id triggered via TapAction on complication. Also, provides static method to create
 * a [PendingIntent] that triggers this receiver.
 */
class ComplicationTapBroadcastReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {

        // Retrieve complication values from Intent's extras.
        val extras = intent.extras ?: return
        val dataSource = extras.getParcelable<ComponentName>(EXTRA_DATA_SOURCE_COMPONENT) ?: return
        val complicationId = extras.getInt(EXTRA_COMPLICATION_ID)

        // Required when using async code in onReceive().
        val result = goAsync()

        // Launches coroutine to update the DataStore counter value.
        scope.launch {
            try {
                context.dataStore.edit { preferences ->
                    val currentValue = preferences[TAP_COUNTER_PREF_KEY] ?: 0

                    // Update data for complication.
                    val newValue = (currentValue + 1) % MAX_NUMBER

                    preferences[TAP_COUNTER_PREF_KEY] = newValue
                }

                // Request an update for the complication that has just been tapped, that is,
                // the system call onComplicationUpdate on the specified complication data
                // source.

            } finally {
                // Always call finish, even if cancelled
                result.finish()
            }
        }
    }

    companion object {
        private const val EXTRA_DATA_SOURCE_COMPONENT =
            "com.example.android.wearable.complicationsdatasource.action.DATA_SOURCE_COMPONENT"
        private const val EXTRA_COMPLICATION_ID =
            "com.example.android.wearable.complicationsdatasource.action.COMPLICATION_ID"
        const val MAX_NUMBER = 20

        /**
         * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
         * toggled and updated.
         */
        fun getToggleIntent(
            context: Context,
            dataSource: ComponentName,
            complicationId: Int
        ): PendingIntent {
            val intent = Intent(context, ComplicationTapBroadcastReceiver::class.java)
            intent.putExtra(EXTRA_DATA_SOURCE_COMPONENT, dataSource)
            intent.putExtra(EXTRA_COMPLICATION_ID, complicationId)

            // Pass complicationId as the requestCode to ensure that different complications get
            // different intents.
            return PendingIntent.getBroadcast(
                context,
                complicationId,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
