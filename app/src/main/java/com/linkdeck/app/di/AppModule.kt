package com.linkdeck.app.di

import android.content.Context
import android.net.nsd.NsdManager
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.HostServer
import com.linkdeck.core.network.TlsIdentity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTlsIdentity(@ApplicationContext context: Context): TlsIdentity =
        kotlinx.coroutines.runBlocking { TlsIdentity.loadOrCreate(context) }

    @Provides
    @Singleton
    fun provideHostServer(identity: TlsIdentity): HostServer = HostServer(identity)

    @Provides
    @Singleton
    fun provideNsdManager(@ApplicationContext context: Context): NsdManager =
        context.getSystemService(NsdManager::class.java)

    @Provides
    @Singleton
    fun provideConnectionManager(): ConnectionManager = ConnectionManager()

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
