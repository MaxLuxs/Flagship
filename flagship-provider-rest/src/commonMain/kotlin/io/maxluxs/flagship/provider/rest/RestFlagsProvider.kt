package io.maxluxs.flagship.provider.rest

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.maxluxs.flagship.core.evaluator.BucketingEngine
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.FlagsProvider
import io.maxluxs.flagship.core.util.SystemClock

class RestFlagsProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    override val name: String = "rest"
) : FlagsProvider {
    private var snapshot: ProviderSnapshot = ProviderSnapshot(
        flags = emptyMap(),
        experiments = emptyMap(),
        revision = null,
        fetchedAtMs = 0L
    )

    override suspend fun bootstrap(): ProviderSnapshot {
        snapshot = fetchSnapshot()
        return snapshot
    }

    override suspend fun refresh(): ProviderSnapshot {
        snapshot = fetchSnapshot(snapshot.revision)
        return snapshot
    }

    override fun evaluateFlag(key: FlagKey, context: EvalContext): FlagValue? {
        return snapshot.flags[key]
    }

    override fun evaluateExperiment(key: ExperimentKey, context: EvalContext): ExperimentAssignment? {
        val experiment = snapshot.experiments[key] ?: return null
        return BucketingEngine.assign(experiment, context)
    }

    private suspend fun fetchSnapshot(currentRevision: String? = null): ProviderSnapshot {
        val url = if (currentRevision != null) {
            "$baseUrl/config?rev=$currentRevision"
        } else {
            "$baseUrl/config"
        }

        val response: RestResponse = client.get(url).body()
        return response.toProviderSnapshot()
    }
}

