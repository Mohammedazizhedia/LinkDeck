package com.linkdeck.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideFileTransferManager(): FileTransferManager {
        return FileTransferManager()
    }

    @Provides
    @Singleton
    fun provideHostServer(
        identity: TlsIdentity,
        fileTransferManager: FileTransferManager
    ): HostServer {
        return HostServer(identity, fileTransferManager)
    }
}
