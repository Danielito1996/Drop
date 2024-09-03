# Drop
Pequeña librería de inyección de dependencia pensada para Ktor server para proyectos de poca concurrencia

---

# DIContainer

Una librería simple de inyección de dependencias para Kotlin, utilizando coroutines para manejar la concurrencia de manera eficiente.

## Tipos y Funciones

### Tipos

- **ScopeType**: Enum que define los diferentes alcances de las dependencias.
  - `SINGLETON`: Una única instancia compartida.
  - `TRANSIENT`: Una nueva instancia cada vez que se solicita.
  - `SCOPED`: Una nueva instancia para cada alcance definido.

### Anotaciones

- **@Singleton**: Marca una función proveedora para que su instancia sea singleton.
- **@Transient**: Marca una función proveedora para que su instancia sea transient.
- **@Scoped**: Marca una función proveedora para que su instancia sea scoped.
- **@Provides**: Marca una función como proveedora de dependencias.
- **@InjectConstructor**: Marca un constructor para inyección de dependencias.

### Funciones

- **register**: Registra una dependencia en el contenedor.
  ```kotlin
  fun <T : Any> register(clazz: KClass<T>, provider: () -> T, scope: ScopeType = ScopeType.SINGLETON)
  ```

- **resolve**: Resuelve una dependencia registrada.
  ```kotlin
  suspend fun <T : Any> resolve(clazz: KClass<T>): T
  ```

- **loadModules**: Carga módulos que contienen funciones proveedoras.
  ```kotlin
  fun loadModules(vararg modules: Any)
  ```

- **inject**: Inyecta dependencias en una clase utilizando su constructor.
  ```kotlin
  suspend fun <T : Any> inject(clazz: KClass<T>): T
  ```

## Uso

### Registro de Dependencias

Para registrar dependencias, define un módulo con funciones proveedoras anotadas.

```kotlin
@Module
class AppModule {

    @Singleton
    @Provides
    fun provideGreetingService(): GreetingService {
        return EnglishGreetingService()
    }

    @Scoped
    @Provides
    fun provideSessionService(): SessionService {
        return SessionService()
    }

    @Transient
    @Provides
    fun provideRequestService(): RequestService {
        return RequestService()
    }
}
```

### Consumo de Dependencias

Para consumir dependencias, utiliza el contenedor para resolverlas o inyectarlas.

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val diContainer = DIContainer()
    diContainer.loadModules(AppModule())

    // Resolviendo una dependencia
    val greetingService = diContainer.resolve(GreetingService::class)
    println(greetingService.greet())

    // Inyectando dependencias en una clase
    val sessionService = diContainer.inject(SessionService::class)
    println(sessionService.getSessionInfo())
}
```

### Ejemplo de Inyección de Interfaces

Define una interfaz y su implementación concreta, y regístrala en el módulo.

```kotlin
interface GreetingService {
    fun greet(): String
}

class EnglishGreetingService @InjectConstructor constructor() : GreetingService {
    override fun greet() = "Hello!"
}

class SpanishGreetingService @InjectConstructor constructor() : GreetingService {
    override fun greet() = "¡Hola!"
}

@Module
class AppModule {

    @Singleton
    @Provides
    fun provideGreetingService(): GreetingService {
        return EnglishGreetingService()
    }
}
```

Para consumir la interfaz:

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val diContainer = DIContainer()
    diContainer.loadModules(AppModule())

    val greetingService = diContainer.resolve(GreetingService::class)
    println(greetingService.greet())
}
```

## Contribuciones

Las contribuciones son bienvenidas y agradecidas. Por favor, abra un issue o un pull request para discutir cualquier cambio que te gustaría hacer.

---
