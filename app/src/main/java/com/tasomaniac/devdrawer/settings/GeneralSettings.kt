package com.tasomaniac.devdrawer.settings

import android.content.SharedPreferences
import com.tasomaniac.devdrawer.R
import com.tasomaniac.devdrawer.data.Analytics
import com.tasomaniac.devdrawer.rx.SchedulingStrategy
import com.tasomaniac.devdrawer.widget.WidgetUpdater
import javax.inject.Inject

class GeneralSettings @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val sortingPreferences: SortingPreferences,
    private val widgetUpdater: WidgetUpdater,
    private val analytics: Analytics,
    private val scheduling: SchedulingStrategy,
    fragment: SettingsFragment
) : Settings(fragment),
    SharedPreferences.OnSharedPreferenceChangeListener {

  override fun setup() {
    addPreferencesFromResource(R.xml.pref_general)
    sharedPreferences.registerOnSharedPreferenceChangeListener(this)
  }

  override fun release() {
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key.isKeyEquals(R.string.pref_key_sorting)) {

      widgetUpdater.updateAll()
          .compose(scheduling.forCompletable())
          .subscribe()

      analytics.sendEvent("Preference", "Sorting", sortingPreferences.sorting.stringVale(context.resources))
    }
  }

}
