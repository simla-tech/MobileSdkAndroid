package com.simla.android.sdk.notifications.firebase

import com.google.firebase.FirebaseApp

internal object DefaultFirebaseProjectNameProvider : FirebaseProjectNameProvider {
    override fun getName(): String = requireNotNull(FirebaseApp.getInstance().options.projectId) {
        "Firebase project ID is not configured"
    }
}
