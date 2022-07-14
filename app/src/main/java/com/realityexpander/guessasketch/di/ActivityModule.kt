package com.realityexpander.guessasketch.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.realityexpander.guessasketch.data.remote.api.SetupApi
import com.realityexpander.guessasketch.data.remote.common.Constants
import com.realityexpander.guessasketch.data.remote.ws.CustomGsonMessageAdapter
import com.realityexpander.guessasketch.data.remote.ws.DrawingApi
import com.realityexpander.guessasketch.data.remote.ws.FlowStreamAdapter
import com.realityexpander.guessasketch.repository.SetupRepository
import com.realityexpander.guessasketch.repository.SetupRepositoryImpl
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
// This module is scoped to the entire lifetime of the Activity, ignores any configuration changes (like the ViewModel).
//   note: using ActivityComponent would scope the entire Activity to the configuration changes.
@InstallIn(ActivityRetainedComponent::class)
object ActivityModule {


//    // todo - does this need to be in ActivityRetainedComponent?
//    @ActivityRetainedScoped // must match the @InstallIn()
//    @Provides
//    fun provideSetupRepository(
//        setupApi: SetupApi,
//        @ApplicationContext context: Context
//    ): SetupRepository = SetupRepositoryImpl(setupApi, context)
//
//    // todo - does this need to be in ActivityRetainedComponent?
//    // HTTP objects<->json conversion library
//    @ActivityRetainedScoped
//    @Provides
//    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
//        return Retrofit.Builder()
//            .baseUrl(Constants.HTTP_BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .client(okHttpClient)
//            .build()
//            .create(SetupApi::class.java)  // interface must be supplied here, unlike Scarlet
//    }

    // Websocket objects<->json conversion library
    @ActivityRetainedScoped
    @Provides
    fun provideDrawingApi(
        app: Application,
        okHttpClient: OkHttpClient,
        gson: Gson,
    ): DrawingApi {
        return Scarlet.Builder()
            .backoffStrategy(LinearBackoffStrategy(Constants.WEBSOCKET_RECONNECT_INTERVAL))
            .lifecycle(AndroidLifecycle.ofApplicationForeground(app))  // sockets are kept for the lifetime of the app
            .webSocketFactory(
                okHttpClient.newWebSocketFactory(
                    Constants.WS_BASE_URL
                )
            )
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            .addMessageAdapterFactory(CustomGsonMessageAdapter.Factory(gson))
            .build()
            .create()  // return type is inferred from the return type of interface (DrawingApi)
    }
}