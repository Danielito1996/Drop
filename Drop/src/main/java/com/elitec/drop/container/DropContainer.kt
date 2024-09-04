package com.elitec.drop.container

import com.elitec.drop.Provides
import com.elitec.drop.ScopeType.ScopeType
import com.elitec.drop.Scoped
import com.elitec.drop.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

class DropContainer {

    private val providers = mutableMapOf<KClass<*>, Pair<()-> Any, ScopeType>>()
    private val singletons = mutableMapOf<KClass<*>, Any>()
    private val scopedFactories = mutableMapOf<KClass<*>, () -> Any>()

    fun <T : Any> register(clazz: KClass<T>, provider: () -> T, scope: ScopeType = ScopeType.SINGLETON) {
        when (scope) {
            ScopeType.SINGLETON -> providers[clazz] = Pair(provider, scope)
            ScopeType.SCOPED -> scopedFactories[clazz] = provider
            ScopeType.TRANSIENT -> providers[clazz] = Pair(provider, scope)
        }
    }

    fun <T : Any> resolve(clazz: KClass<T>): T {
        val (provider, scope) = providers[clazz] ?: throw IllegalArgumentException("No provider found for ${clazz.simpleName}")

        return when (scope) {
            ScopeType.SINGLETON -> singletons.getOrPut(clazz) { provider() } as T
            ScopeType.TRANSIENT -> provider() as T
            ScopeType.SCOPED -> scopedFactories[clazz]?.invoke() as T
        }
    }

    fun loadModules(vararg  modules: Any) {
        modules.forEach { module->
            module::class.memberFunctions.filter {
                it.hasAnnotation<Provides>()
            }.forEach { function->
                    val returnType = function.returnType.classifier as KClass<*>
                    val scope = when {
                        function.hasAnnotation<Singleton>()->ScopeType.SINGLETON
                        function.hasAnnotation<Scoped>()->ScopeType.SCOPED
                        function.hasAnnotation<Transient>()->ScopeType.TRANSIENT
                        else->ScopeType.SINGLETON
                    }
                providers[returnType] = Pair({function.call(module)!!},scope)
            }
        }
    }

    fun <T : Any> inject(clazz: KClass<T>): T {
        val constructor = clazz.primaryConstructor ?: throw IllegalArgumentException(
            "No primary constructor found for ${clazz.simpleName}"
        )
        val params = constructor.parameters.map { param ->
            resolve(param.type.classifier as KClass<*>)
        }
        return constructor.call(*params.toTypedArray())
    }
}
