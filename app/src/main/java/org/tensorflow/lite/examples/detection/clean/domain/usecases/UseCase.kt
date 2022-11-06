package org.tensorflow.lite.examples.detection.clean.domain.usecases


abstract class UseCase<T, R> {
    abstract suspend fun invoke(data: T? = null): R
}