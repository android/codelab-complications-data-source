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
package com.example.android.wearable.complicationsdataprovider

import android.content.ComponentName
import android.util.Log
import androidx.wear.complications.ComplicationProviderService
import androidx.wear.complications.data.*
import java.util.*

/**
 * Example watch face complication data provider provides a number that can be incremented on tap.
 */
class CustomComplicationProviderService : ComplicationProviderService() {
    /*
     * Called when a complication has been activated. The method is for any one-time
     * (per complication) set-up.
     *
     * You can continue sending data for the active complicationId until onComplicationDeactivated()
     * is called.
     */
    override fun onComplicationActivated(complicationId: Int, type: Int) {
        super.onComplicationActivated(complicationId, type)
        Log.d(TAG, "onComplicationActivated(): $complicationId")
    }

    /*
     * Called when the complication needs updated data from your provider. There are four scenarios
     * when this will happen:
     *
     *   1. An active watch face complication is changed to use this provider
     *   2. A complication using this provider becomes active
     *   3. The period of time you specified in the manifest has elapsed (UPDATE_PERIOD_SECONDS)
     *   4. You triggered an update from your own class via the
     *       ProviderUpdateRequester.requestUpdate() method.
     */
    override fun onComplicationUpdate(
        complicationId: Int,
        type: ComplicationType,
        resultCallback: ComplicationUpdateCallback
    ) {
        Log.d(TAG, "onComplicationUpdate() id: $complicationId")

        // Create Tap Action so that the user can trigger an update by tapping the complication.
        val thisProvider = ComponentName(this, javaClass)
        // We pass the complication id, so we can only update the specific complication tapped.
        val complicationPendingIntent =
            ComplicationTapBroadcastReceiver.getToggleIntent(
                this, thisProvider, complicationId
            )

        // Retrieves your data, in this case, we grab an incrementing number from SharedPrefs.
        val preferences = getSharedPreferences(
            ComplicationTapBroadcastReceiver.COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY,
            0
        )
        val number = preferences.getInt(
            ComplicationTapBroadcastReceiver
                .getPreferenceKey(thisProvider, complicationId), 0
        )
        val numberText = String.format(Locale.getDefault(), "%d!", number)
        val complicationData = when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData
                .Builder(ComplicationText.plain(numberText))
                .setTapAction(complicationPendingIntent)
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData
                .Builder(ComplicationText.plain("Number: $numberText"))
                .setTapAction(complicationPendingIntent)
                .build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData
                .Builder(
                    value = number.toFloat(),
                    min = 0f,
                    max = ComplicationTapBroadcastReceiver.MAX_NUMBER.toFloat()
                )
                .setText(ComplicationText.plain(numberText))
                .setTapAction(complicationPendingIntent)
                .build()
            else -> {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type $type")
                }
                null
            }
        }
        resultCallback.onUpdateComplication(complicationData)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData
                .Builder(ComplicationText.plain("12!"))
                .build()
            ComplicationType.LONG_TEXT -> LongTextComplicationData
                .Builder(ComplicationText.plain("Number: 12"))
                .build()
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData
                .Builder(
                    value = 12f,
                    min = 0f,
                    max = ComplicationTapBroadcastReceiver.MAX_NUMBER.toFloat()
                )
                .setText(ComplicationText.plain("12!"))
                .build()
            else -> {
                if (Log.isLoggable(TAG, Log.WARN)) {
                    Log.w(TAG, "Unexpected complication type $type")
                }
                null
            }
        }
    }

    /*
     * Called when the complication has been deactivated.
     */
    override fun onComplicationDeactivated(complicationId: Int) {
        Log.d(TAG, "onComplicationDeactivated(): $complicationId")
    }

    companion object {
        private const val TAG = "ComplicationProvider"
    }
}