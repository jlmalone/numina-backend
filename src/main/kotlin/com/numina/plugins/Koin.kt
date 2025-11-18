package com.numina.plugins

import com.numina.data.repositories.*
import com.numina.services.*
import com.numina.services.matching.ClassMatcher
import com.numina.services.matching.UserMatcher
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

val appModule = module {
    // Repositories
    single<UserRepository> { UserRepositoryImpl() }
    single<UserProfileRepository> { UserProfileRepositoryImpl() }
    single<ClassRepository> { ClassRepositoryImpl() }
    single<RefreshTokenRepository> { RefreshTokenRepositoryImpl() }
    single<MatchRepository> { MatchRepositoryImpl() }

    // Matching components
    single { UserMatcher() }
    single { ClassMatcher() }

    // Services
    single<AuthService> {
        AuthServiceImpl(
            userRepository = get(),
            userProfileRepository = get(),
            refreshTokenRepository = get()
        )
    }
    single<UserService> {
        UserServiceImpl(
            userProfileRepository = get()
        )
    }
    single<ClassService> {
        ClassServiceImpl(
            classRepository = get()
        )
    }
    single<MatchingService> {
        MatchingServiceImpl(
            userProfileRepository = get(),
            classRepository = get(),
            matchRepository = get(),
            userMatcher = get(),
            classMatcher = get()
        )
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
