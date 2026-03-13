package au.com.swagritech.opsapp.repo

import au.com.swagritech.opsapp.api.ApiClient
import au.com.swagritech.opsapp.api.OpsApiService
import au.com.swagritech.opsapp.model.ActiveJobResponse
import au.com.swagritech.opsapp.model.AuthIdentityResponse
import au.com.swagritech.opsapp.model.StartJobRequest
import au.com.swagritech.opsapp.model.StartJobResponse

class OpsRepository(
    private val api: OpsApiService = ApiClient.retrofit.create(OpsApiService::class.java)
) {
    suspend fun verifyIdentity(): Result<AuthIdentityResponse> {
        val response = api.getAuthIdentity()
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException("HTTP ${response.code()}"))
        }
        return Result.success(response.body() ?: AuthIdentityResponse())
    }

    suspend fun startJob(payload: StartJobRequest): Result<StartJobResponse> {
        val response = api.startJob(payload)
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException("HTTP ${response.code()}"))
        }
        return Result.success(response.body() ?: StartJobResponse(status = "error", error = "Empty response"))
    }

    suspend fun getActiveJob(pilot: String): Result<ActiveJobResponse> {
        val response = api.getActiveJob(pilot)
        if (!response.isSuccessful) {
            return Result.failure(IllegalStateException("HTTP ${response.code()}"))
        }
        return Result.success(response.body() ?: ActiveJobResponse(status = "error", error = "Empty response"))
    }
}
