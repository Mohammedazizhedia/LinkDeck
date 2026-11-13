package com.linkdeck.core.network

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideTlsIdentity(@ApplicationContext context: Context): TlsIdentity {
        // يتم تحميل الشهادة أو إنشائها بشكل متزامن في الخلفية
        return kotlinx.coroutines.runBlocking {
            TlsIdentity.loadOrCreate(context)
        }
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
