package com.zobaer53.plantapp.core.data.di

import javax.inject.Qualifier

/** Blocking I/O: content resolver reads and bitmap decoding. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/** CPU-bound work: bitmap scaling and tensor conversion. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

/** The single thread that creates, runs and closes the LiteRT CompiledModel. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class InferenceDispatcher
