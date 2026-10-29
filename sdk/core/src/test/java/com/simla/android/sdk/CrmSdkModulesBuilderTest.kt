package com.simla.android.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CrmSdkModulesBuilderTest {
    @Test
    fun repeated_singleton_module_is_registered_once() {
        val module = TestFeature("test")
        val builder = CrmSdkModulesBuilder()

        builder.register(module)
        builder.register(module)

        assertEquals(listOf(module), builder.build())
    }

    @Test
    fun different_modules_with_same_key_are_rejected() {
        val builder = CrmSdkModulesBuilder()
        builder.register(TestFeature("test"))

        assertThrows(IllegalStateException::class.java) {
            builder.register(TestFeature("test"))
        }
    }
}

private class TestFeature(key: String) : CrmFeature<Any> {
    override val key = CrmFeatureKey<Any>(key)

    override fun install(context: CrmFeatureContext): Any = Any()
}
