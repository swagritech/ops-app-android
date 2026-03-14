package au.com.swagritech.opsapp.api

import au.com.swagritech.opsapp.model.ActiveJobResponse
import au.com.swagritech.opsapp.model.AuthIdentityResponse
import au.com.swagritech.opsapp.model.CreateFlightRequest
import au.com.swagritech.opsapp.model.CreateFlightResponse
import au.com.swagritech.opsapp.model.StartJobRequest
import au.com.swagritech.opsapp.model.StartJobResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface OpsApiService {
    @GET("get_auth_identity")
    suspend fun getAuthIdentity(): Response<AuthIdentityResponse>

    @POST("start_job")
    suspend fun startJob(@Body payload: StartJobRequest): Response<StartJobResponse>

    @GET("get_active_job")
    suspend fun getActiveJob(@Query("Pilot") pilot: String): Response<ActiveJobResponse>

    @POST("create_flight")
    suspend fun createFlight(@Body payload: CreateFlightRequest): Response<CreateFlightResponse>
}
