package com.elitec.drop.container

import com.elitec.drop.Provides
import com.elitec.drop.ScopeType.ScopeType
import com.elitec.drop.Scoped
import com.elitec.drop.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

class FullContainer {
    private val providers = mutableMapOf<KClass<*>, Pair<() -> Any, ScopeType>>()
    private val singletons = mutableMapOf<KClass<*>, Deferred<Any>>()
    private val scopedFactories = mutableMapOf<KClass<*>, () -> Any>()

    fun <T : Any> register(clazz: KClass<T>, provider: () -> Any, scope: ScopeType = ScopeType.SINGLETON) {
        when (scope) {
            ScopeType.SINGLETON -> providers[clazz] = Pair(provider, scope)
            ScopeType.SCOPED -> scopedFactories[clazz] = provider
            ScopeType.TRANSIENT -> providers[clazz] = Pair(provider, scope)
        }
    }

    suspend fun <T : Any> resolve(clazz: KClass<T>): T = withContext(Dispatchers.Default) {
        val (provider, scope) = providers[clazz] ?: throw IllegalArgumentException("No provider found for ${clazz.simpleName}")

        return@withContext when (scope) {
            ScopeType.SINGLETON -> {
                val deferred = singletons.getOrPut(clazz) {
                    async {  provider() }
                }
                deferred.await() as T
            }
            ScopeType.TRANSIENT -> provider() as T
            ScopeType.SCOPED -> scopedFactories[clazz]?.invoke() as T
        }
    }

    fun loadModules(vararg modules: Any) {
        modules.forEach { module ->
            module::class.memberFunctions.filter { it.hasAnnotation<Provides>() }.forEach { function ->
                val returnType = function.returnType.classifier as KClass<*>
                val scope = when {
                    function.hasAnnotation<Singleton>() -> ScopeType.SINGLETON
                    function.hasAnnotation<Transient>() -> ScopeType.TRANSIENT
                    function.hasAnnotation<Scoped>() -> ScopeType.SCOPED
                    else -> ScopeType.SINGLETON
                }
                register(returnType, { function.call(module)!! }, scope)
            }
        }
    }

    suspend fun <T : Any> inject(clazz: KClass<T>): T = withContext(Dispatchers.Default) {
        val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException("No primary constructor found for ${clazz.simpleName}")
        val params = constructor.parameters.map { param ->
            resolve(param.type.classifier as KClass<*>)
        }
        return@withContext constructor.call(*params.toTypedArray())
    }
}