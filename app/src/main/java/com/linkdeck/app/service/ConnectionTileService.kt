package com.linkdeck.app.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.linkdeck.app.R

/** Quick Settings tile: tap to start the connection service, tap again to stop. */
class ConnectionTileService : TileService() {

    override fun onStartListening() {
        update(LinkDeckService.running)
    }

    override fun onClick() {
        if (LinkDeckService.running) {
            startService(Intent(this, LinkDeckService::class.java).setAction(LinkDeckService.ACTION_STOP))
            update(false)
        } else {
            startForegroundService(Intent(this, LinkDeckService::class.java).setAction(LinkDeckService.ACTION_START))
            update(true)
        }
    }

    private fun update(active: Boolean) {
        qsTile?.apply {
            state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = getString(if (active) R.string.tile_state_on else R.string.tile_state_off)
            updateTile()
        }
    }
}
