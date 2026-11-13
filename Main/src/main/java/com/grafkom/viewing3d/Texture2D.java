package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

/**
 * Pengelola Tekstur 2D menggunakan STBImage - Indikator 4.
 * Mendukung UV Mapping, pergantian filter (Nearest vs Bilinear Mipmapped),
 * dan pembacaan texture file.
 */
public class Texture2D {
    private int textureID;
    private int width;
    private int height;

    public Texture2D(String filePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Parameter wrapping default: REPEAT
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Parameter filtering default: Bilinear + Trilinear Mipmap
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Balik koordinat Y gambar agar sesuai dengan konvensi OpenGL (origin di kiri-bawah)
        stbi_set_flip_vertically_on_load(true);

        // Tangani path file relatif atau absolut
        File file = new File(filePath);
        if (!file.exists()) {
            // Coba cari alternatif path
            File alt = new File("src/main/resources/" + filePath);
            if (alt.exists()) {
                filePath = alt.getAbsolutePath();
            } else {
                File alt2 = new File("Grafikom M10/" + filePath);
                if (alt2.exists()) {
                    filePath = alt2.getAbsolutePath();
                }
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            ByteBuffer data = stbi_load(filePath, w, h, channels, 4);
            if (data != null) {
                this.width = w.get(0);
                this.height = h.get(0);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                glGenerateMipmap(GL_TEXTURE_2D);
                stbi_image_free(data);
                System.out.println("[Texture2D] Berhasil memuat tekstur: " + filePath + " (" + width + "x" + height + ")");
            } else {
                System.err.println("[Texture2D] Gagal memuat tekstur: " + filePath + " -> " + stbi_failure_reason());
            }
        }
    }

    /**
     * Mengatur mode filtering tekstur secara real-time.
     * @param mipmapped true untuk Bilinear + Mipmapping, false untuk Nearest Neighbor (terlihat pixelated & moire).
     */
    public void setFilter(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER,
                mipmapped ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER,
                mipmapped ? GL_LINEAR : GL_NEAREST);
    }

    /**
     * Mengikat tekstur ke unit tekstur tertentu (misalnya GL_TEXTURE0 atau GL_TEXTURE1).
     * @param unit index unit tekstur (0, 1, ...)
     */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public int getTextureID() {
        return textureID;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
