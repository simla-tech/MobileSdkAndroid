package com.simla.android.sdk

/**
 * Marks public declarations that exist only for communication between SDK artifacts.
 * Applications must not depend on this API because it may change without notice.
 */
@MustBeDocumented
@RequiresOptIn(
    message = "This API is reserved for Simla SDK modules and may change without notice.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.TYPEALIAS,
)
annotation class CrmSdkInternalApi
