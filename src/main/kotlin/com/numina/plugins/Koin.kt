package com.numina.plugins

import com.numina.data.repositories.*
import com.numina.services.*
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
    single<MatchActionRepository> { MatchActionRepositoryImpl() }
    single<MutualMatchRepository> { MutualMatchRepositoryImpl() }
    single<MatchPreferencesRepository> { MatchPreferencesRepositoryImpl() }

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
    single<ScoreCalculator> { ScoreCalculatorImpl() }
    single<UserMatcher> {
        UserMatcherImpl(
            userProfileRepository = get(),
            matchActionRepository = get(),
            scoreCalculator = get()
        )
    }
    single<ClassMatcher> {
        ClassMatcherImpl(
            userProfileRepository = get(),
            classRepository = get(),
            matchPreferencesRepository = get(),
            scoreCalculator = get()
        )
    }
    single<MatchingService> {
        MatchingServiceImpl(
            userMatcher = get(),
            classMatcher = get(),
            matchActionRepository = get(),
            mutualMatchRepository = get(),
            userProfileRepository = get(),
            scoreCalculator = get()
        )
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
