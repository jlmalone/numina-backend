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
    single<AdminUserRepository> { AdminUserRepositoryImpl() }
    single<AdminAuditLogRepository> { AdminAuditLogRepositoryImpl() }
    single<FeatureFlagRepository> { FeatureFlagRepositoryImpl() }
    single<SystemSettingsRepository> { SystemSettingsRepositoryImpl() }

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
    single<AuditLogService> {
        AuditLogServiceImpl(
            auditLogRepository = get()
        )
    }
    single<FeatureFlagService> {
        FeatureFlagServiceImpl(
            featureFlagRepository = get()
        )
    }
    single<AdminUserService> {
        AdminUserServiceImpl(
            userRepository = get(),
            userProfileRepository = get()
        )
    }
    single<ModerationService> {
        ModerationServiceImpl()
    }
    single<AdminAnalyticsService> {
        AdminAnalyticsServiceImpl(
            userRepository = get(),
            classRepository = get()
        )
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
