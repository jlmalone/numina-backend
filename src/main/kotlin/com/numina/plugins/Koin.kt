package com.numina.plugins

import com.numina.data.repositories.*
import com.numina.messaging.MessagingService
import com.numina.messaging.MessagingServiceImpl
import com.numina.messaging.WebSocketManager
import com.numina.services.*
import com.numina.services.groups.*
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
    single<ReviewRepository> { ReviewRepositoryImpl() }
    single<RatingAggregateRepository> { RatingAggregateRepositoryImpl() }

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
    single<RatingAggregationService> {
        RatingAggregationServiceImpl(
            ratingAggregateRepository = get(),
            reviewRepository = get()
        )
    }
    single<ReviewService> {
        ReviewServiceImpl(
            reviewRepository = get(),
            ratingAggregationService = get()
        )
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
