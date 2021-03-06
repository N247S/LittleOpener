package com.alleluid.littleopener.client

import com.alleluid.littleopener.*
import com.alleluid.littleopener.common.blocks.blockopener.TileOpener
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.client.resources.I18n
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.TextComponentTranslation

class GuiOpener(val openerPos: BlockPos) : GuiScreen() {
    val X_FIELD = 0
    val Y_FIELD = 1
    val Z_FIELD = 2
    lateinit var xField: GuiTextField
    lateinit var yField: GuiTextField
    lateinit var zField: GuiTextField
    private lateinit var fields: List<GuiTextField>

    override fun initGui() {
        super.initGui()
        //Centers, adjusts for offset, adjusts for coords referring to left side
        val targetPos = (Minecraft.getMinecraft().world.getTileEntity(openerPos) as TileOpener).targetPos
        xField = GuiTextField(X_FIELD, fontRenderer, width / 2 - 70 - 30, height / 2, 60, 15)
        yField = GuiTextField(Y_FIELD, fontRenderer, width / 2 - 0 - 30, height / 2, 60, 15)
        zField = GuiTextField(Z_FIELD, fontRenderer, width / 2 + 70 - 30, height / 2, 60, 15)
        fields = listOf(xField, yField, zField)
        fields.forEach {
            it.maxStringLength = 10
            it.isFocused = false
            it.visible = true
        }

        xField.apply { text = targetPos.x.toString() }
        yField.apply { text = targetPos.y.toString() }
        zField.apply { text = targetPos.z.toString() }
    }

    fun setFields(pos: BlockPos){
        xField.text = pos.x.toString()
        yField.text = pos.y.toString()
        zField.text = pos.z.toString()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        super.drawScreen(mouseX, mouseY, partialTicks)
        Gui.drawRect(width / 2 - 20, height / 2 - 20, width / 2 + 20, height / 2 + 20, 0xffffff)
        drawCenteredString(fontRenderer, I18n.format("text.littleopener.gui_opener.title"), width / 2, height / 2 - 15, 0xffffff)
        fields.forEach { it.drawTextBox() }
        drawCenteredString(fontRenderer, I18n.format("text.littleopener.gui_opener.max_dist", ConfigHandler.maxDistance), width / 2, height / 2 + 25, 0xa0a0a0)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        fields.forEach { it.mouseClicked(mouseX, mouseY, mouseButton) }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (validIntChars.contains(typedChar, true) ||
            specialCharacterCodes.contains(keyCode)
        ) {
            fields.forEach { it.textboxKeyTyped(typedChar, keyCode) }

        // If W or S are pressed, increment/decrement focused coord
        } else if (keyForward.isActiveAndMatches(keyCode) || keyBack.isActiveAndMatches(keyCode)){
            val focused = fields.firstOrNull { it.isFocused }
            var fieldInt = focused?.text?.toIntOrNull() ?: return

            if (keyForward.isActiveAndMatches(keyCode))
                fieldInt++
            else if (keyBack.isActiveAndMatches(keyCode))
                fieldInt--

            focused.text = fieldInt.toString()
        } else if (keyCode == 15){
            val focusedIdx = fields.indexOfFirst { it.isFocused }
            fields.forEach { it.isFocused = false }
            val newIdx = if (focusedIdx + 1 > 2) 0 else focusedIdx + 1
            fields[newIdx].isFocused = true
        } else if (keyInv.isActiveAndMatches(keyCode)) {
            this.mc.player.closeScreen()
        } else println("Keycode: $keyCode")
    }

    override fun onGuiClosed() {
        val intFields = fields.map { it.text.toIntOrNull() ?: Int.MIN_VALUE}
        val posLT = BlockPos(intFields[0], intFields[1], intFields[2])
        if (openerPos.getDistance(intFields[0], intFields[1], intFields[2]) > ConfigHandler.maxDistance?.toDouble() ?: 25.0)
            Minecraft.getMinecraft().player.sendStatusMessage(TextComponentTranslation("text.littleopener.gui_opener.over_limit"), true)
        PacketHandler.INSTANCE.sendToServer(CoordsMessage(openerPos, posLT))
        super.onGuiClosed()
    }
}