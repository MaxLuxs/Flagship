package io.maxluxs.flagship.core.manager

import io.maxluxs.flagship.core.model.FlagKey

interface FlagsListener {
    fun onSnapshotUpdated(source: String)
    fun onOverrideChanged(key: FlagKey)
}

