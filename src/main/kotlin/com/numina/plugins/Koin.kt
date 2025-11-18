package com.numina.plugins

import com.numina.data.repositories.*
import com.numina.messaging.MessagingService
import com.numina.messaging.MessagingServiceImpl
import com.numina.messaging.WebSocketManager
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

    // Messaging Repositories
    single<MessageRepository> { MessageRepositoryImpl() }
    single<ConversationRepository> { ConversationRepositoryImpl(userRepository = get()) }
    single<BlockedUserRepository> { BlockedUserRepositoryImpl() }
    single<MessageReportRepository> { MessageReportRepositoryImpl() }

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

    // Messaging Service
    single<MessagingService> {
        MessagingServiceImpl(
            messageRepository = get(),
            conversationRepository = get(),
            blockedUserRepository = get(),
            messageReportRepository = get(),
            userRepository = get()
        )
    }

    // WebSocket Manager (singleton)
    single { WebSocketManager() }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
