package com.simla.android.sdk.storage

interface SdkSessionStorage {
    fun activateScope(scope: String?) = Unit

    fun getUserToken(): String?

    fun setUserToken(value: String?)

    fun clear()
}
