package bigbade.betterhealth.listener;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.advancements.criterion.MobEffectsPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.ForgeIngameGui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber
public class HealthRenderer {

    private static final ResourceLocation ICON_HEARTS = new ResourceLocation("betterhealth", "textures/gui/hearts.png");
    private static final ResourceLocation ICON_ABSORB = new ResourceLocation("betterhealth", "textures/gui/absorb.png");
    private static final ResourceLocation ICON_VANILLA = new ResourceLocation("textures/gui/icons.png");

    private static final Minecraft mc = Minecraft.getInstance();

    private static int updateCounter = 0;
    private static int playerHealth = 0;
    private static int lastPlayerHealth = 0;
    private static long healthUpdateCounter = 0;
    private static long lastSystemTime = 0;
    private static Random rand = new Random();

    private static int height;
    private static int width;
    private static int regen;

    private static int left_height = 39;

    private static void drawTexturedModalRect(int x, int y, int textureX, int textureY, int width, int height) {
        float f = 0.00390625F;
        float f1 = 0.00390625F;
        double zLevel = -90;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos((double)(x + 0), (double)(y + height), zLevel).tex((double)((float)(textureX + 0) * 0.00390625F), (double)((float)(textureY + height) * 0.00390625F)).endVertex();
        bufferbuilder.pos((double)(x + width), (double)(y + height), zLevel).tex((double)((float)(textureX + width) * 0.00390625F), (double)((float)(textureY + height) * 0.00390625F)).endVertex();
        bufferbuilder.pos((double)(x + width), (double)(y + 0), zLevel).tex((double)((float)(textureX + width) * 0.00390625F), (double)((float)(textureY + 0) * 0.00390625F)).endVertex();
        bufferbuilder.pos((double)(x + 0), (double)(y + 0), zLevel).tex((double)((float)(textureX + 0) * 0.00390625F), (double)((float)(textureY + 0) * 0.00390625F)).endVertex();
        tessellator.draw();
    }

    @SubscribeEvent
    public static void renderHeath(final RenderGameOverlayEvent.Pre event) {
        Entity renderViewEnity = mc.getRenderViewEntity();
        if(event.getType() != RenderGameOverlayEvent.ElementType.HEALTH
                || event.isCanceled()
                || !(renderViewEnity instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity player = (PlayerEntity) mc.getRenderViewEntity();

        // extra setup stuff from us
        left_height = ForgeIngameGui.left_height;
        width = mc.mainWindow.getScaledWidth();
        height = mc.mainWindow.getScaledHeight();
        event.setCanceled(true);
        updateCounter = mc.ingameGUI.getTicks();

        // start default forge/mc rendering
        // changes are indicated by comment
        mc.getProfiler().startSection("health");
        GlStateManager.enableBlend();

        int health = MathHelper.ceil(player.getHealth());
        boolean highlight = healthUpdateCounter > (long)updateCounter && (healthUpdateCounter - (long)updateCounter) / 3L %2L == 1L;

        if (health < playerHealth && player.hurtResistantTime > 0)
        {
            lastSystemTime = Util.milliTime();
            healthUpdateCounter = (long)(updateCounter + 20);
        }
        else if (health > playerHealth && player.hurtResistantTime > 0)
        {
            lastSystemTime = Util.milliTime();
            healthUpdateCounter = (long)(updateCounter + 10);
        }

        if (Util.milliTime() - lastSystemTime > 1000L)
        {
            playerHealth = health;
            lastPlayerHealth = health;
            lastSystemTime = Util.milliTime();
        }

        playerHealth = health;
        int healthLast = lastPlayerHealth;

        IAttributeInstance attrMaxHealth = player.getAttribute(SharedMonsterAttributes.MAX_HEALTH);
        float healthMax = (float)attrMaxHealth.getValue();
        float absorb = MathHelper.ceil(player.getAbsorptionAmount());

        // CHANGE: simulate 10 hearts max if there's more, so vanilla only renders one row max
        healthMax = Math.min(healthMax, 20f);
        health = Math.min(health, 20);
        absorb = Math.min(absorb, 20);

        int healthRows = MathHelper.ceil((healthMax /*+ absorb*/) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (healthRows - 2), 3);

        rand.setSeed((long)(updateCounter * 312871));

        int left = width / 2 - 91;
        int top = height - left_height;
        left_height += (healthRows * rowHeight);
        if (rowHeight != 10) left_height += 10 - rowHeight;

        regen = -1;
        if (player.isPotionActive(Effects.REGENERATION))
        {
            regen = updateCounter % 25;
        }

        final int TOP =  9 * (mc.world.getWorldInfo().isHardcore() ? 5 : 0);
        final int BACKGROUND = (highlight ? 25 : 16);
        int MARGIN = 16;
        if (player.isPotionActive(Effects.POISON))      MARGIN += 36;
        else if (player.isPotionActive(Effects.WITHER)) MARGIN += 72;
        float absorbRemaining = absorb;

        for (int i = MathHelper.ceil((healthMax /*+ absorb*/) / 2.0F) - 1; i >= 0; --i)
        {
            //int b0 = (highlight ? 1 : 0);
            int row = MathHelper.ceil((float)(i + 1) / 10.0F) - 1;
            int x = left + i % 10 * 8;
            int y = top - row * rowHeight;

            if (health <= 4) y += rand.nextInt(2);
            if (i == regen) y -= 2;

            drawTexturedModalRect(x, y, BACKGROUND, TOP, 9, 9);

            if (highlight)
            {
                if (i * 2 + 1 < healthLast)
                    drawTexturedModalRect(x, y, MARGIN + 54, TOP, 9, 9); //6
                else if (i * 2 + 1 == healthLast)
                    drawTexturedModalRect(x, y, MARGIN + 63, TOP, 9, 9); //7
            }

            /*if (absorbRemaining > 0.0F)
            {
                if (absorbRemaining == absorb && absorb % 2.0F == 1.0F)
                {
                    drawTexturedModalRect(x, y, MARGIN + 153, TOP, 9, 9); //17
                    absorbRemaining -= 1.0F;
                }
                else
                {
                    drawTexturedModalRect(x, y, MARGIN + 144, TOP, 9, 9); //16
                    absorbRemaining -= 2.0F;
                }
            }
            else
            {*/
                if (i * 2 + 1 < health)
                    drawTexturedModalRect(x, y, MARGIN + 36, TOP, 9, 9); //4
                else if (i * 2 + 1 == health)
                    drawTexturedModalRect(x, y, MARGIN + 45, TOP, 9, 9); //5
            //}
        }


        renderExtraHearts(left, top, player);
        //renderExtraAbsorption(left, top - rowHeight, player);


        mc.getTextureManager().bindTexture(ICON_VANILLA);
        ForgeIngameGui.left_height += 10;
        //if(absorb > 0) {
            //ForgeIngameGui.left_height += 10;
        //}

        event.setCanceled(true);

        GlStateManager.disableBlend();
        mc.getProfiler().endSection();
    }

    private static void renderExtraHearts(int xBasePos, int yBasePos, PlayerEntity player) {
        int potionOffset = getPotionOffset(player);

        // Extra hearts
        mc.getTextureManager().bindTexture(ICON_HEARTS);

        int hp = MathHelper.ceil(player.getHealth())+MathHelper.ceil(player.getAbsorptionAmount());
        renderCustomHearts(xBasePos, yBasePos, potionOffset, hp, false);
    }

    private static void renderCustomHearts(int xBasePos, int yBasePos, int potionOffset, int count, boolean absorb) {
        int regenOffset = absorb ? 10 : 0;
        for(int iter = 0; iter < count / 20; iter++) {
            int renderHearts = (count - 20 * (iter + 1)) / 2;
            int heartIndex = iter % 11;
            if(renderHearts > 10) {
                renderHearts = 10;
            }
            for(int i = 0; i < renderHearts; i++) {
                int y = getYRegenOffset(i, regenOffset);
                if(absorb) {
                    drawTexturedModalRect(xBasePos + 8 * i, yBasePos + y, 0, 54, 9, 9);
                }
                drawTexturedModalRect(xBasePos + 8 * i, yBasePos + y, 0 + 18 * heartIndex, potionOffset, 9, 9);
            }
            if(count % 2 == 1 && renderHearts < 10) {
                int y = getYRegenOffset(renderHearts, regenOffset);
                if(absorb) {
                    drawTexturedModalRect(xBasePos + 8 * renderHearts, yBasePos + y, 0, 54, 9, 9);
                }
                drawTexturedModalRect(xBasePos + 8 * renderHearts, yBasePos + y, 9 + 18 * heartIndex, potionOffset, 9, 9);
            }
        }
    }

    private static int getYRegenOffset(int i, int offset) {
        return i + offset == regen ? -2 : 0;
    }

    private static int getPotionOffset(PlayerEntity player) {
        int potionOffset = 0;
        EffectInstance potion = player.getActivePotionEffect(Effects.WITHER);
        if(potion != null) {
            potionOffset = 18;
        }
        potion = player.getActivePotionEffect(Effects.POISON);
        if(potion != null) {
            potionOffset = 9;
        }
        if(mc.world.getWorldInfo().isHardcore()) {
            potionOffset += 27;
        }
        return potionOffset;
    }

    private static void renderExtraAbsorption(int xBasePos, int yBasePos, PlayerEntity player) {
        int potionOffset = getPotionOffset(player);

        // Extra hearts
        mc.getTextureManager().bindTexture(ICON_ABSORB);

        int absorb = MathHelper.ceil(player.getAbsorptionAmount());
        renderCustomHearts(xBasePos, yBasePos, potionOffset, absorb, true);
    }
}
