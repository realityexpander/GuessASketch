package com.realityexpander.guessasketch.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.realityexpander.guessasketch.data.remote.api.SetupApi
import com.realityexpander.guessasketch.data.remote.common.Constants
import com.realityexpander.guessasketch.data.remote.common.Constants.HTTP_BASE_URL
import com.realityexpander.guessasketch.data.remote.common.Constants.QUERY_PARAMETER_CLIENT_ID
import com.realityexpander.guessasketch.data.remote.common.Constants.WEBSOCKET_RECONNECT_INTERVAL
import com.realityexpander.guessasketch.data.remote.common.Constants.WS_BASE_URL
import com.realityexpander.guessasketch.data.remote.ws.CustomGsonMessageAdapter
import com.realityexpander.guessasketch.data.remote.ws.DrawingApi
import com.realityexpander.guessasketch.data.remote.ws.FlowStreamAdapter
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.repository.SetupRepositoryImpl
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.realityexpander.guessasketch.util.clientId
import com.realityexpander.guessasketch.util.dataStore
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

const val CLIENT_ID = "clientId"

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(@Named(CLIENT_ID) clientId: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Add the client id to the request query params
                val url = chain.request().url.newBuilder()
                    .addQueryParameter(QUERY_PARAMETER_CLIENT_ID, clientId)
                    .build()
                val request = chain.request().newBuilder()
                    .url(url)
                    .build()

                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BODY)
            })
            .build()
    }

    @Singleton
    @Provides
    @Named(CLIENT_ID)
    fun provideClientId(@ApplicationContext context: Context): String {
        // Get clientId from the data store
        return runBlocking {
            context.dataStore.clientId()
        }
    }

    @Singleton
    @Provides
    fun provideGsonInstance(): Gson {
        return Gson()
    }

    @Singleton // must match the @InstallIn()
    @Provides
    fun provideSetupRepository(
        setupApi: SetupApi,
        @ApplicationContext context: Context
    ): SetupRepository = SetupRepositoryImpl(setupApi, context)

    // HTTP objects<->json conversion library
    @Singleton
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
        return Retrofit.Builder()
            .baseUrl(Constants.HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)  // interface must be supplied here, unlike Scarlet
    }

    @Singleton
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return object: DispatcherProvider {
            override val main: CoroutineDispatcher
                get() = Dispatchers.Main
            override val io: CoroutineDispatcher
                get() = Dispatchers.IO
            override val default: CoroutineDispatcher
                get() = Dispatchers.Default

        }
    }

    @Singleton
    @Provides
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ) = context
}