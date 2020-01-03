package org.pedrofelix.examples.reactive.utils

import reactor.core.Disposable

/**
 * Extension function to use a `Disposable` ensuring its dispose at the the end.
 */
inline fun <T : Disposable?, R> T.use(block: (T) -> R): R = try {
    block(this)
} finally {
    this?.dispose()
}
