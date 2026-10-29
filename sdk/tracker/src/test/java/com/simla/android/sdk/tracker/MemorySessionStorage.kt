package com.simla.android.sdk.tracker

import com.simla.android.sdk.storage.SdkSessionStorage

internal class MemorySessionStorage : SdkSessionStorage {
    private var token: String? = null
    override fun getUserToken() = token
    override fun setUserToken(value: String?) {
        token = value
    }

    override fun clear() {
        token = null
    }
}
