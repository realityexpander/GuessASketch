package com.realityexpander.guessasketch.di

import android.content.Context
import com.google.gson.Gson
import com.realityexpander.guessasketch.data.remote.api.SetupApi
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.repository.SetupRepositoryImpl
import com.realityexpander.guessasketch.util.Constants
import com.realityexpander.guessasketch.util.DispatcherProvider
import com.realityexpander.guessasketch.util.clientId
import com.realityexpander.guessasketch.util.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideSetupRepository(
        setupApi: SetupApi,
        @ApplicationContext context: Context
    ): SetupRepository = SetupRepositoryImpl(setupApi, context)

    @Singleton
    @Provides
    fun provideOkHttpClient(@Named(CLIENT_ID) clientId: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Add the client id to the request query params
                val url = chain.request().url.newBuilder()
                    .addQueryParameter(Constants.QUERY_PARAMETER_CLIENT_ID, clientId)
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
        return runBlocking {
            context.dataStore.clientId()
        }
    }

    @Singleton
    @Provides
    fun provideGsonInstance(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
        return Retrofit.Builder()
            .baseUrl(Constants.HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)
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