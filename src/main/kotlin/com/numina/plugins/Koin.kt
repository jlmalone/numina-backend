package com.numina.plugins

import com.numina.data.repositories.*
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
    single<GroupRepository> { GroupRepositoryImpl() }
    single<GroupMemberRepository> { GroupMemberRepositoryImpl() }
    single<GroupActivityRepository> { GroupActivityRepositoryImpl() }

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
    single<GroupService> {
        GroupServiceImpl(
            groupRepository = get(),
            memberRepository = get()
        )
    }
    single<GroupActivityService> {
        GroupActivityServiceImpl(
            activityRepository = get(),
            memberRepository = get()
        )
    }
}

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(appModule)
    }
}
