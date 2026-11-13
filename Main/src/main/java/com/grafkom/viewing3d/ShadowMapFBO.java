package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL30.*;

/**
 * Pengelola Framebuffer Object (FBO) untuk Shadow Depth Map - Indikator 5.
 * Menyimpan depth buffer dari sudut pandang sumber cahaya pada pass 1
 * untuk digunakan pada pass 2 render utama.
 */
public class ShadowMapFBO {
    private int fboID;
    private int depthTextureID;
    private final int SHADOW_WIDTH = 2048;
    private final int SHADOW_HEIGHT = 2048;

    public ShadowMapFBO() {
        // 1. Buat Framebuffer Object
        fboID = glGenFramebuffers();

        // 2. Buat Depth Texture 2D
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, SHADOW_WIDTH, SHADOW_HEIGHT, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);

        // Filtering Nearest untuk pembacaan depth akurat
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Clamp to border dengan warna border 1.0 (putih / far distance)
        // Mencegah area di luar frustum cahaya terarsir bayangan hitam
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        // 3. Kaitkan depth texture ke FBO
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);

        // FBO ini tidak membutuhkan color buffer output
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        // Verifikasi kelengkapan status framebuffer
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("[ShadowMapFBO] Framebuffer tidak lengkap! Status: " + status);
        }

        // Lepaskan ikatan framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Mempersiapkan FBO untuk pass 1 (penulisan depth dari pandangan cahaya).
     */
    public void bindForWriting() {
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glBindFramebuffer(GL_FRAMEBUFFER, fboID);
        glClear(GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Mengembalikan render target ke layar utama (pass 2).
     * @param screenWidth lebar viewport layar utama
     * @param screenHeight tinggi viewport layar utama
     */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    public int getDepthTextureID() {
        return depthTextureID;
    }

    public int getFboID() {
        return fboID;
    }

    public int getShadowWidth() {
        return SHADOW_WIDTH;
    }

    public int getShadowHeight() {
        return SHADOW_HEIGHT;
    }
}
