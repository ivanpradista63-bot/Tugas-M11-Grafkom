package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Pengelola Hidden Surface Removal (HSR) - Indikator 3.
 * Mengontrol Z-Buffer Depth Test dan Back-Face Culling.
 */
public class HSRManager {

    /**
     * Mengatur status Z-Buffer Depth Test.
     * @param enable true untuk mengaktifkan GL_DEPTH_TEST dengan fungsi GL_LESS.
     */
    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // Pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    /**
     * Mengatur status Back-Face Culling.
     * @param enable true untuk membuang poligon tampak belakang (GL_BACK) dengan winding CCW.
     */
    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);  // Buang polygon belakang
            glFrontFace(GL_CCW);  // Counter-Clockwise adalah tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}
