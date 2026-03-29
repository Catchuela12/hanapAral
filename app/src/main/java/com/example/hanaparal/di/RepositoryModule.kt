package com.example.hanaparal.di

import com.example.hanaparal.data.repository.AuthRepository
import com.example.hanaparal.data.repository.AuthRepositoryImpl
import com.example.hanaparal.data.repository.GroupRepository
import com.example.hanaparal.data.repository.GroupRepositoryImpl
import com.example.hanaparal.data.repository.ProfileRepository
import com.example.hanaparal.data.repository.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGroupRepository(
        impl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
}