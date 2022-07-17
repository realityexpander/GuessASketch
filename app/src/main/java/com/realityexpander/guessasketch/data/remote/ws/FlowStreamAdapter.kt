package com.realityexpander.guessasketch.data.remote.ws

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.reflect.Type

// Convert WebSocket Stream to a Kotlin Flow.

@OptIn(ExperimentalCoroutinesApi::class)
class FlowStreamAdapter<T>: StreamAdapter<T, Flow<T>> {

    override fun adapt(stream: Stream<T>): Flow<T> {
        return callbackFlow {
            stream.start(object: Stream.Observer<T> {
                override fun onComplete() {
                    close()
                }

                override fun onError(throwable: Throwable) {
                    close(cause = throwable)
                }

                override fun onNext(data: T) {
                    if(!isClosedForSend) {
                        offer(data)  // like emit for flow
                    }
                }
            })
            awaitClose { }  // keeps the stream alive until the flow is closed (wait for close)
        }
    }

    // Gets the type for the factory that will be used to create the stream adapter
    object Factory: StreamAdapter.Factory {
        override fun create(type: Type): StreamAdapter<Any, Any> {
            return when(type.getRawType()) {
                Flow::class.java -> FlowStreamAdapter()
                else -> throw IllegalArgumentException("Unsupported stream adapter type: $type")
            }
        }
    }
}