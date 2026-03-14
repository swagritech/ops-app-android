package au.com.swagritech.opsapp.repo

import au.com.swagritech.opsapp.api.ApiClient
import au.com.swagritech.opsapp.api.OpsApiService
import au.com.swagritech.opsapp.model.ActiveJobResponse
import au.com.swagritech.opsapp.model.AuthIdentityResponse
import au.com.swagritech.opsapp.model.CreateFlightRequest
import au.com.swagritech.opsapp.model.CreateFlightResponse
import au.com.swagritech.opsapp.model.StartJobRequest
import au.com.swagritech.opsapp.model.StartJobResponse
import java.io.IOException

class ApiHttpException(val code: Int, message: String) : Exception(message)

class OpsRepository(
    private val api: OpsApiService = ApiClient.retrofit.create(OpsApiService::class.java)
) {
    suspend fun verifyIdentity(): Result<AuthIdentityResponse> {
        return try {
            val response = api.getAuthIdentity()
            if (!response.isSuccessful) {
                Result.failure(ApiHttpException(response.code(), "HTTP ${response.code()}"))
            } else {
                Result.success(response.body() ?: AuthIdentityResponse())
            }
        } catch (io: IOException) {
            Result.failure(io)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startJob(payload: StartJobRequest): Result<StartJobResponse> {
        return try {
            val response = api.startJob(payload)
            if (!response.isSuccessful) {
                Result.failure(ApiHttpException(response.code(), "HTTP ${response.code()}"))
            } else {
                Result.success(response.body() ?: StartJobResponse(status = "error", error = "Empty response"))
            }
        } catch (io: IOException) {
            Result.failure(io)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveJob(pilot: String): Result<ActiveJobResponse> {
        return try {
            val response = api.getActiveJob(pilot)
            if (!response.isSuccessful) {
                Result.failure(ApiHttpException(response.code(), "HTTP ${response.code()}"))
            } else {
                Result.success(response.body() ?: ActiveJobResponse(status = "error", error = "Empty response"))
            }
        } catch (io: IOException) {
            Result.failure(io)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFlight(payload: CreateFlightRequest): Result<CreateFlightResponse> {
        return try {
            val response = api.createFlight(payload)
            if (!response.isSuccessful) {
                Result.failure(ApiHttpException(response.code(), "HTTP ${response.code()}"))
            } else {
                Result.success(response.body() ?: CreateFlightResponse(status = "error", error = "Empty response"))
            }
        } catch (io: IOException) {
            Result.failure(io)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
