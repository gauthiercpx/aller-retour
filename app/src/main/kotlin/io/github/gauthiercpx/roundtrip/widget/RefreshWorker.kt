package io.github.gauthiercpx.roundtrip.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.gauthiercpx.roundtrip.data.DeparturesRepository

@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DeparturesRepository,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        repository.refresh()
        // Redraw even when the fetch failed so the footer shows how old the data is.
        DeparturesWidget().updateAll(applicationContext)
        // A failed fetch is not retried with backoff: the next 15-minute period is the retry, which spares the battery.
        return Result.success()
    }
}
