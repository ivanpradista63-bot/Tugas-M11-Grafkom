package com.grafkom.viewing3d;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Status Input dan State Kontrol Interaktif untuk 3D Visual Realism Suite.
 */
public class InputState {
    public boolean blinnPhong   = true;  // B: Blinn-Phong (true) vs Standard Phong (false)
    public boolean smooth       = true;  // F: Smooth Shading (true) vs Flat Shading (false)
    public boolean depthTest    = true;  // Z: Z-Buffer Depth Test ON/OFF
    public boolean cullFace     = true;  // C: Back-Face Culling ON/OFF
    public boolean useTexture   = true;  // T: Texture Mapping ON/OFF
    public boolean mipmapped    = true;  // N: Bilinear Mipmapped (true) vs Nearest (false)
    public boolean shadow       = true;  // H: Shadow Mapping ON/OFF
    public int     biasMode     = 0;     // P: 0 = Dynamic Bias, 1 = Zero Bias (Acne), 2 = Large Bias (Peter Panning)
    public boolean usePCF       = true;  // K: PCF 3x3 Soft Shadow vs Single Sample Hard Shadow
    public boolean useChecker   = false; // X: Tekstur Bata vs Catur (Checkerboard)
    public boolean autoRotate   = false; // SPACE: Auto Rotasi Objek/Kamera
    public boolean takeScreenshot = false; // F12: Request tangkapan layar

    public void onKey(int key, int action) {
        if (action != GLFW_PRESS) return; // Hanya tangani saat tombol ditekan (1x per press)

        switch (key) {
            case GLFW_KEY_B -> blinnPhong     = !blinnPhong;
            case GLFW_KEY_F -> smooth         = !smooth;
            case GLFW_KEY_Z -> depthTest      = !depthTest;
            case GLFW_KEY_C -> cullFace       = !cullFace;
            case GLFW_KEY_T -> useTexture     = !useTexture;
            case GLFW_KEY_N -> mipmapped      = !mipmapped;
            case GLFW_KEY_H -> shadow         = !shadow;
            case GLFW_KEY_P -> biasMode       = (biasMode + 1) % 3;
            case GLFW_KEY_K -> usePCF         = !usePCF;
            case GLFW_KEY_X -> useChecker     = !useChecker;
            case GLFW_KEY_SPACE -> autoRotate = !autoRotate;
            case GLFW_KEY_F12   -> takeScreenshot = true;
        }
    }

    public String getBiasName() {
        return switch (biasMode) {
            case 1 -> "Zero(Acne)";
            case 2 -> "Large(PeterPan)";
            default -> "Dynamic";
        };
    }
}
