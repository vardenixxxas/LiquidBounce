/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.antivoid.mode

import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.fakelag.FakeLag.firstPosition
import net.ccbluex.liquidbounce.features.module.modules.player.antivoid.ModuleAntiVoid
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

object AntiVoidBlinkMode : AntiVoidMode("Blink") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiVoid.mode

    // Cases in which the AntiVoid protection should not be active.
    override val isExempt
        get() = super.isExempt || ModuleScaffold.enabled

    // Whether artificial lag is needed to prevent falling into the void.
    val requiresLag
        get() = AntiVoidBlinkMode.handleEvents() && ModuleAntiVoid.isLikelyFalling
            && !isExempt && isWorth()

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket) {
            FakeLag.flush()
        }
    }

    override fun fix(): Boolean {
        if (!requiresLag) {
            return false
        }

        // Check if we have any previous positions to teleport to.
        firstPosition() ?: return false

        // Teleport the player to the last position.
        FakeLag.cancel()
        return true
    }

    private fun isWorth(): Boolean {
        // If we do not have any previous positions, we cannot determine if it is worth it,
        // so we assume it is.
        val (playerPosition, _, _) = firstPosition() ?: return true

        // If the first position is not safe, we should not continue to blink and wait for
        return playerPosition.toBlockPos().down().canStandOn()
    }

}
