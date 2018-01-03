package com.tasomaniac.devdrawer.configure

import android.content.Intent
import android.content.pm.PackageManager
import android.support.annotation.VisibleForTesting
import com.tasomaniac.devdrawer.data.Dao
import com.tasomaniac.devdrawer.data.Widget
import com.tasomaniac.devdrawer.data.insertApps
import com.tasomaniac.devdrawer.data.insertFilters
import com.tasomaniac.devdrawer.data.insertWidget
import com.tasomaniac.devdrawer.data.updateWidget
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.annotations.CheckReturnValue
import javax.inject.Inject

class ConfigureUseCase @Inject constructor(
    private val packageManager: PackageManager,
    private val dao: Dao,
    val appWidgetId: Int
) {

  @CheckReturnValue
  fun insert(widgetName: String, packageMatchers: List<String>): Completable {
    if (packageMatchers.isEmpty()) throw IllegalArgumentException("Empty packageMatchers")
    
    val widget = Widget(appWidgetId, widgetName)
    return dao.findWidgetById(appWidgetId)
        .isEmpty
        .flatMapCompletable { isEmpty ->
          if (isEmpty) dao.insertWidget(widget) else dao.updateWidget(widget)
        }
        .andThen(dao.insertFilters(appWidgetId, packageMatchers))
        .andThen(
            Observable.fromIterable(packageMatchers)
                .flatMapCompletable { packageMatcher ->
                  findMatchingPackages(packageMatcher)
                      .flatMapCompletable {
                        dao.insertApps(appWidgetId, it)
                      }
                }
        )
  }

  private fun findMatchingPackages(packageMatcher: String): Observable<List<String>> {
    return Observable.fromCallable {
      findMatchingPackagesSync(packageMatcher, allLauncherPackages())
    }
  }

  @CheckReturnValue
  fun findPossiblePackageMatchers(): Observable<Collection<String>> {
    return Observable.fromCallable {
      findPossiblePackageMatchersSync(allLauncherPackages())
    }
  }

  private fun allLauncherPackages(): List<String> {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
    return packageManager.queryIntentActivities(intent, 0)
        .map { it.activityInfo.applicationInfo.packageName }
        .toSortedSet()
        .toList()
  }

  @VisibleForTesting
  fun findPossiblePackageMatchersSync(packageNames: List<String>): Set<String> {
    return packageNames
        .flatMap {
          var packageName = it

          it.split('.')
              .foldRight(setOf(it)) { part, acc ->
                packageName = packageName.removeSuffix(".$part")
                acc + "$packageName.*"
              }
              .reversed()
        }
        .toSet()
  }

  @VisibleForTesting
  fun findMatchingPackagesSync(packageMatcher: String, packageNames: List<String>): List<String> {
    return packageNames
        .filter {
          if (packageMatcher.endsWith(".*")) {
            it.startsWith(packageMatcher.removeSuffix(".*"), ignoreCase = true)
          } else {
            it.equals(packageMatcher, ignoreCase = true)
          }
        }
  }
}