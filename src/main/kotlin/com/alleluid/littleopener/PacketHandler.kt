package com.alleluid.littleopener

import com.alleluid.littleopener.client.GuiOpener
import com.alleluid.littleopener.common.blocks.blockopener.TileOpener
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.util.math.BlockPos
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

object PacketHandler {
    @JvmStatic
    val INSTANCE = SimpleNetworkWrapper(MOD_ID)
    @JvmStatic
    var uid = 0

    @JvmStatic
    fun registerMessages() {
        INSTANCE.registerMessage(CoordsMessage.SaveCoordsMessageHandler::class.java, CoordsMessage::class.java, uid++, Side.SERVER)
        INSTANCE.registerMessage(CoordsMessage.LoadCoordsMessageHandler::class.java, CoordsMessage::class.java, uid++, Side.CLIENT)
    }
}

class CoordsMessage(var blockPosTE: BlockPos, var blockPosLT: BlockPos) : IMessage {

    constructor() : this(errorBlockPos, errorBlockPos)

    override fun toBytes(buf: ByteBuf) {
        if (blockPosLT == errorBlockPos || blockPosTE == errorBlockPos)
            return
        buf.writeLong(blockPosTE.toLong())
        buf.writeLong(blockPosLT.toLong())
    }

    override fun fromBytes(buf: ByteBuf) {
        blockPosTE = BlockPos.fromLong(buf.readLong())
        blockPosLT = BlockPos.fromLong(buf.readLong())
    }

    class SaveCoordsMessageHandler : IMessageHandler<CoordsMessage, IMessage> {
        override fun onMessage(message: CoordsMessage?, ctx: MessageContext?): IMessage? {
            if (message != null && ctx != null) {
                if (listOf(message.blockPosLT, message.blockPosTE).contains(errorBlockPos))
                    return null

                val serverWorld = ctx.serverHandler.player.serverWorld
                serverWorld.addScheduledTask {
                    if (listOf(message.blockPosLT, message.blockPosTE).all {
                                serverWorld.isBlockLoaded(it)
                                    && serverWorld.isBlockModifiable(ctx.serverHandler.player, it)
                            }){
                        val opener = serverWorld.getTileEntity(message.blockPosTE) as TileOpener
                        opener.targetPos = message.blockPosLT
                    }
                }
            }
            return null
        }
    }

    class LoadCoordsMessageHandler : IMessageHandler<CoordsMessage, IMessage> {
        override fun onMessage(message: CoordsMessage?, ctx: MessageContext?): IMessage? {
            if (message != null) {
                Minecraft.getMinecraft().addScheduledTask {
                    val opener = Minecraft.getMinecraft().world.getTileEntity(message.blockPosTE) as TileOpener
                    opener.targetPos = message.blockPosLT

                    val screen = Minecraft.getMinecraft().currentScreen
                    if (screen is GuiOpener){
                        screen.setFields(message.blockPosLT)
                    }
                }
            }
            return null
        }
    }

}
