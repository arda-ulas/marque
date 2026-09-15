package io.github.ardaulas.marque.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.ardaulas.marque.R
import io.github.ardaulas.marque.core.result.DataError

/** The one place a [DataError] becomes user-facing text; screens never switch on the error themselves. */
@StringRes
fun DataError.messageRes(): Int =
    when (this) {
        DataError.Network -> R.string.error_network
        DataError.Timeout -> R.string.error_timeout
        is DataError.Http -> R.string.error_http
        DataError.Serialization -> R.string.error_serialization
        DataError.Unknown -> R.string.error_unknown
    }

/** Format arguments for [messageRes]; only [DataError.Http] carries one, the status code. */
fun DataError.messageArgs(): Array<Any> =
    when (this) {
        is DataError.Http -> arrayOf(code)
        DataError.Network, DataError.Timeout, DataError.Serialization, DataError.Unknown -> emptyArray()
    }

@Composable
fun DataError.toMessage(): String = stringResource(messageRes(), *messageArgs())
