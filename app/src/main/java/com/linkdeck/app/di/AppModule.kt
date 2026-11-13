package com.linkdeck.app.di

import android.content.Context
import android.net.nsd.NsdManager
import com.linkdeck.app.devices.TrustedDeviceStore
import com.linkdeck.app.pairing.PairingManager
import com.linkdeck.core.network.ConnectionManager
import com.linkdeck.core.network.FileTransferManager
import com.linkdeck.core.network.HostServer
import com.linkdeck.core.network.NsdDiscovery
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
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideHostServer(
        identity: TlsIdentity,
        fileTransferManager: FileTransferManager,
        scope: CoroutineScope
    ): HostServer {
        return HostServer(identity, fileTransferManager, scope)
    }

    @Provides
    @Singleton
    fun provideConnectionManager(
        scope: CoroutineScope
    ): ConnectionManager {
        return ConnectionManager(scope)
    }

    @Provides
    @Singleton
    fun provideNsdManager(@ApplicationContext context: Context): NsdManager {
        return context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    @Provides
    @Singleton
    fun provideNsdDiscovery(
        nsdManager: NsdManager,
        scope: CoroutineScope
    ): NsdDiscovery {
        return NsdDiscovery(nsdManager, scope)
    }

    @Provides
    @Singleton
    fun provideTrustedDeviceStore(@ApplicationContext context: Context): TrustedDeviceStore {
        return TrustedDeviceStore(context)
    }

    @Provides
    @Singleton
    fun providePairingManager(
        @ApplicationContext context: Context,
        tlsIdentity: TlsIdentity,
        hostServer: HostServer,
        nsdManager: NsdManager,
        trustedStore: TrustedDeviceStore,
        connectionManager: ConnectionManager,
        scope: CoroutineScope
    ): PairingManager {
        return PairingManager(
            context,
            tlsIdentity,
            hostServer,
            nsdManager,
            trustedStore,
            connectionManager,
            scope
        )
    }
}
