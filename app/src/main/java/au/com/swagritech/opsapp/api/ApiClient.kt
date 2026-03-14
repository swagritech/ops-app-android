package au.com.swagritech.opsapp.api

import au.com.swagritech.opsapp.BuildConfig
import au.com.swagritech.opsapp.auth.AuthSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = AuthSession.accessToken
                val easyAuthToken = AuthSession.easyAuthToken
                val principalName = AuthSession.principalName
                val principalId = AuthSession.principalId
                val requestBuilder = chain.request().newBuilder()
                if (!easyAuthToken.isNullOrBlank()) {
                    requestBuilder.addHeader("X-ZUMO-AUTH", easyAuthToken)
                } else if (!token.isNullOrBlank()) {
                    // Fallback only when EasyAuth token has not yet been established.
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                if (!principalName.isNullOrBlank()) {
                    requestBuilder.addHeader("X-MS-CLIENT-PRINCIPAL-NAME", principalName)
                }
                if (!principalId.isNullOrBlank()) {
                    requestBuilder.addHeader("X-MS-CLIENT-PRINCIPAL-ID", principalId)
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
