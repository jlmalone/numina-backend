package com.numina.plugins

import com.numina.data.repositories.*
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    single<UserRepository> { UserRepositoryImpl() }
    single<UserProfileRepository> { UserProfileRepositoryImpl() }
    single<ClassRepository> { ClassRepositoryImpl() }
    single<RefreshTokenRepository> { RefreshTokenRepositoryImpl() }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
