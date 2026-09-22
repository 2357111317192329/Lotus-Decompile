/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.renderer.MeshBuilder
 *  meteordevelopment.meteorclient.renderer.MeshRenderer
 *  meteordevelopment.meteorclient.renderer.MeteorRenderPipelines
 *  meteordevelopment.meteorclient.renderer.text.CustomTextRenderer
 *  meteordevelopment.meteorclient.renderer.text.Font
 *  meteordevelopment.meteorclient.renderer.text.FontFace
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  net.minecraft.client.MinecraftClient
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Overwrite
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xiaohe66.mc.meteor.lotus.mixin;

import com.xiaohe66.mc.meteor.lotus.util.FontFix;
import java.io.IOException;
import java.nio.ByteBuffer;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CustomTextRenderer.class}, remap=false)
public abstract class CustomTextRendererMixin {
    @Shadow
    @Final
    private MeshBuilder mesh;
    @Shadow
    private boolean building;
    @Shadow
    private boolean scaleOnly;
    @Shadow
    private double fontScale;
    @Shadow
    private double scale;
    @Shadow
    @Final
    private Font[] fonts;
    @Unique
    private FontFix[] lotus$fixedFonts = new FontFix[5];
    @Unique
    private FontFix lotus$fixedFont;

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    public void onInit(FontFace fontFace, CallbackInfo ci) throws IOException {
        ByteBuffer fontBuffer = fontFace.readToDirectByteBuffer();
        for (int i = 0; i < this.lotus$fixedFonts.length; ++i) {
            this.lotus$fixedFonts[i] = new FontFix(fontBuffer, (int)Math.round(27.0 * ((double)i * 0.5 + 1.0)));
        }
    }

    @Overwrite
    public double getWidth(String text, int count, boolean shadow) {
        if (text.isEmpty()) {
            return 0.0;
        }
        FontFix font = this.building ? this.lotus$fixedFont : this.lotus$fixedFonts[0];
        return (font.getWidth(text, count) + (double)(shadow ? 1 : 0)) * this.scale / 1.5;
    }

    @Overwrite
    public double getHeight(boolean shadow) {
        FontFix font = this.building ? this.lotus$fixedFont : this.lotus$fixedFonts[0];
        return (double)(font.getFontSize() + 1 + (shadow ? 1 : 0)) * this.scale / 1.5;
    }

    @Overwrite
    public void begin(double scale, boolean scaleOnly, boolean maxSize) {
        if (this.building) {
            throw new RuntimeException("CustomTextRenderer.begin() called twice");
        }
        if (!scaleOnly) {
            this.mesh.begin();
        }
        if (maxSize) {
            this.lotus$fixedFont = this.lotus$fixedFonts[this.lotus$fixedFonts.length - 1];
        } else {
            double roundedScale = Math.floor(scale * 10.0) / 10.0;
            int index = roundedScale >= 3.0 ? 5 : (roundedScale >= 2.5 ? 4 : (roundedScale >= 2.0 ? 3 : (roundedScale >= 1.5 ? 2 : 1)));
            this.lotus$fixedFont = this.lotus$fixedFonts[index - 1];
        }
        this.building = true;
        this.scaleOnly = scaleOnly;
        this.fontScale = (double)this.lotus$fixedFont.getFontSize() / 27.0;
        this.scale = 1.0 + (scale - this.fontScale) / this.fontScale;
    }

    @Overwrite
    public void end() {
        if (!this.building) {
            throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");
        }
        if (!this.scaleOnly) {
            this.mesh.end();
            MeshRenderer.begin().attachments(MinecraftClient.getInstance().getFramebuffer()).pipeline(MeteorRenderPipelines.UI_TEXT).mesh(this.mesh).sampler("u_Texture", this.lotus$fixedFont.texture.getGlTextureView(), this.lotus$fixedFont.texture.getSampler()).end();
        }
        this.building = false;
        this.scale = 1.0;
    }

    @Overwrite
    public double render(String text, double x, double y, Color color, boolean shadow) {
        double x2;
        boolean wasBuilding = this.building;
        if (!wasBuilding) {
            this.begin(1.0, false, false);
        }
        if (shadow) {
            int shadowAlpha = CustomTextRenderer.SHADOW_COLOR.a;
            CustomTextRenderer.SHADOW_COLOR.a = (int)((double)color.a / 255.0 * (double)shadowAlpha);
            x2 = this.lotus$fixedFont.render(this.mesh, text, x + this.fontScale * this.scale / 1.5, y + this.fontScale * this.scale / 1.5, CustomTextRenderer.SHADOW_COLOR, this.scale / 1.5);
            this.lotus$fixedFont.render(this.mesh, text, x, y, color, this.scale / 1.5);
            CustomTextRenderer.SHADOW_COLOR.a = shadowAlpha;
        } else {
            x2 = this.lotus$fixedFont.render(this.mesh, text, x, y, color, this.scale / 1.5);
        }
        if (!wasBuilding) {
            this.end();
        }
        return x2;
    }

    @Overwrite
    public void destroy() {
        for (Font font : this.fonts) {
            if (font == null) continue;
            font.texture.close();
        }
        for (FontFix fixedFont : this.lotus$fixedFonts) {
            if (fixedFont == null) continue;
            fixedFont.texture.close();
        }
    }
}
