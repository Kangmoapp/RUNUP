package com.example.runup.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager by lazy {WorkManager.getInstance(context)}

    fun setupDataSync() {
        // 1. 즉시 실행 (OneTime)
        val immediateRequest = OneTimeWorkRequestBuilder<CourseSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "IMMEDIATE_SYNC",
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
        /*
        // 2. 주기적 실행 (Periodic)
        val periodicRequest = PeriodicWorkRequestBuilder<CourseSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "PERIODIC_SYNC",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        */
    }
}