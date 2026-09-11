/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.textures.FilterMode
 *  com.mojang.blaze3d.textures.TextureFormat
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 *  meteordevelopment.meteorclient.renderer.MeshBuilder
 *  meteordevelopment.meteorclient.renderer.Texture
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  org.lwjgl.BufferUtils
 *  org.lwjgl.stb.STBTTFontinfo
 *  org.lwjgl.stb.STBTTPackContext
 *  org.lwjgl.stb.STBTTPackRange
 *  org.lwjgl.stb.STBTTPackRange$Buffer
 *  org.lwjgl.stb.STBTTPackedchar
 *  org.lwjgl.stb.STBTTPackedchar$Buffer
 *  org.lwjgl.stb.STBTruetype
 *  org.lwjgl.system.MemoryStack
 *  org.lwjgl.system.Struct
 */
package com.xiaohe66.mc.meteor.lotus.util;

import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackRange;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

public class FontFix {
    public final Texture texture;
    private final int fontSize;
    private final float scale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<GlyphQuad> glyphs = new Int2ObjectOpenHashMap();
    private static final int ATLAS_SIZE = 2048;
    private final ByteBuffer fontBuffer;
    private final STBTTFontinfo fontInfo;
    private final ByteBuffer bitmap;
    private final STBTTPackContext packContext;
    private long lastPackTime = 0L;
    private int packCount = 0;
    private final int MAX_PACKS = 7;

    public FontFix(ByteBuffer fontBuffer, int fontSize) {
        this.fontBuffer = fontBuffer;
        this.fontSize = fontSize;
        this.fontInfo = STBTTFontinfo.create();
        STBTruetype.stbtt_InitFont((STBTTFontinfo)this.fontInfo, (ByteBuffer)fontBuffer);
        this.bitmap = BufferUtils.createByteBuffer((int)0x400000);
        this.packContext = STBTTPackContext.create();
        STBTruetype.stbtt_PackBegin((STBTTPackContext)this.packContext, (ByteBuffer)this.bitmap, (int)2048, (int)2048, (int)0, (int)1);
        this.texture = new Texture(2048, 2048, TextureFormat.RED8, FilterMode.LINEAR, FilterMode.LINEAR);
        this.scale = STBTruetype.stbtt_ScaleForPixelHeight((STBTTFontinfo)this.fontInfo, (float)fontSize);
        try (MemoryStack stack = MemoryStack.stackPush();){
            IntBuffer ascentBuffer = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics((STBTTFontinfo)this.fontInfo, (IntBuffer)ascentBuffer, null, null);
            this.ascent = ascentBuffer.get(0);
        }
        this.packInitialGlyphs();
    }

    private void packInitialGlyphs() {
        STBTTPackedchar.Buffer packedChars = STBTTPackedchar.create((int)128);
        STBTTPackRange.Buffer packRange = STBTTPackRange.create((int)1);
        packRange.put(STBTTPackRange.create().set((float)this.fontSize, 32, null, 128, packedChars, (byte)2, (byte)2));
        packRange.flip();
        STBTruetype.stbtt_PackFontRanges((STBTTPackContext)this.packContext, (ByteBuffer)this.fontBuffer, (int)0, (STBTTPackRange.Buffer)packRange);
        for (int i = 0; i < packedChars.capacity(); ++i) {
            STBTTPackedchar packedChar = (STBTTPackedchar)packedChars.get(i);
            this.putGlyphQuad(i + 32, packedChar);
        }
        this.texture.upload(this.bitmap);
    }

    private void addGlyphs(List<Integer> codepoints) {
        if (System.currentTimeMillis() - this.lastPackTime > 100L) {
            this.lastPackTime = System.currentTimeMillis();
            this.packCount = 0;
        }
        if (this.packCount >= this.MAX_PACKS) {
            return;
        }
        for (Integer codepoint : codepoints) {
            this.addGlyph(codepoint);
        }
        this.texture.upload(this.bitmap);
        ++this.packCount;
    }

    private void addGlyph(int codepoint) {
        if (this.glyphs.containsKey(codepoint)) {
            return;
        }
        STBTTPackedchar.Buffer packedChars = STBTTPackedchar.create((int)1);
        STBTTPackRange.Buffer packRange = STBTTPackRange.create((int)1);
        packRange.put(STBTTPackRange.create().set((float)this.fontSize, codepoint, null, 1, packedChars, (byte)2, (byte)2));
        packRange.flip();
        STBTruetype.stbtt_PackFontRanges((STBTTPackContext)this.packContext, (ByteBuffer)this.fontBuffer, (int)0, (STBTTPackRange.Buffer)packRange);
        STBTTPackedchar packedChar = (STBTTPackedchar)packedChars.get(0);
        this.putGlyphQuad(codepoint, packedChar);
    }

    private void putGlyphQuad(int codepoint, STBTTPackedchar packedChar) {
        float uvScaleX = 4.8828125E-4f;
        float uvScaleY = 4.8828125E-4f;
        this.glyphs.put(codepoint, new GlyphQuad(packedChar.xoff(), packedChar.yoff(), packedChar.xoff2(), packedChar.yoff2(), (float)packedChar.x0() * uvScaleX, (float)packedChar.y0() * uvScaleY, (float)packedChar.x1() * uvScaleX, (float)packedChar.y1() * uvScaleY, packedChar.xadvance()));
    }

    public double getWidth(String text, int length) {
        double width = 0.0;
        if (this.ensureGlyphs(text)) {
            return width;
        }
        for (int i = 0; i < length; ++i) {
            char c = text.charAt(i);
            GlyphQuad glyph = (GlyphQuad)this.glyphs.get((int)c);
            if (glyph == null) continue;
            width += (double)glyph.advance;
        }
        return width;
    }

    public int getFontSize() {
        return this.fontSize;
    }

    private boolean ensureGlyphs(String text) {
        boolean added = false;
        ArrayList<Integer> missingGlyphs = null;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            GlyphQuad glyph = (GlyphQuad)this.glyphs.get((int)c);
            if (glyph != null) continue;
            if (missingGlyphs == null) {
                missingGlyphs = new ArrayList<Integer>();
            }
            missingGlyphs.add(Integer.valueOf(c));
            added = true;
        }
        if (missingGlyphs != null) {
            this.addGlyphs(missingGlyphs);
        }
        return added;
    }

    public double render(MeshBuilder meshBuilder, String text, double x, double y, Color color, double scale) {
        if (this.ensureGlyphs(text)) {
            return x;
        }
        y += (double)(this.ascent * this.scale) * scale;
        int length = text.length();
        meshBuilder.ensureCapacity(length * 4, length * 6);
        for (int i = 0; i < length; ++i) {
            char c = text.charAt(i);
            GlyphQuad glyph = (GlyphQuad)this.glyphs.get((int)c);
            if (glyph == null) continue;
            meshBuilder.quad(meshBuilder.vec2(x + (double)glyph.xoff * scale, y + (double)glyph.yoff * scale).vec2((double)glyph.u0, (double)glyph.v0).color(color).next(), meshBuilder.vec2(x + (double)glyph.xoff * scale, y + (double)glyph.yoff2 * scale).vec2((double)glyph.u0, (double)glyph.v1).color(color).next(), meshBuilder.vec2(x + (double)glyph.xoff2 * scale, y + (double)glyph.yoff2 * scale).vec2((double)glyph.u1, (double)glyph.v1).color(color).next(), meshBuilder.vec2(x + (double)glyph.xoff2 * scale, y + (double)glyph.yoff * scale).vec2((double)glyph.u1, (double)glyph.v0).color(color).next());
            x += (double)glyph.advance * scale;
        }
        return x;
    }

    private record GlyphQuad(float xoff, float yoff, float xoff2, float yoff2, float u0, float v0, float u1, float v1, float advance) {
    }
}
