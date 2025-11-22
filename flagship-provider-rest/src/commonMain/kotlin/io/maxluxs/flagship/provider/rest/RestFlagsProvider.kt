package io.maxluxs.flagship.provider.rest

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.maxluxs.flagship.core.model.*
import io.maxluxs.flagship.core.provider.BaseFlagsProvider

class RestFlagsProvider(
    private val client: HttpClient,
    private val baseUrl: String,
    name: String = "rest"
) : BaseFlagsProvider(name) {

    override suspend fun fetchSnapshot(currentRevision: String?): ProviderSnapshot {
        val url = if (currentRevision != null) {
            "$baseUrl/config?rev=$currentRevision"
        } else {
            "$baseUrl/config"
        }

        val response: RestResponse = client.get(url).body()
        return response.toProviderSnapshot()
    }
}

