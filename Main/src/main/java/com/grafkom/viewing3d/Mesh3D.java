package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/**
 * Kelas Mesh3D merepresentasikan model geometri 3D untuk visualisasi
 * proyeksi dan stereo viewing.
 *
 * Menyediakan:
 * 1. Model 3D Rumah Sederhana beratap prisma dengan setiap sisi berpewarnaan kontras.
 * 2. Kubus 3D berwarna (Rubik's cube style / multi-colored cube).
 * 3. Sumbu koordinat Kartesian 3D (X=Merah, Y=Hijau, Z=Biru).
 * 4. Ground Grid bidang XZ untuk mempertegas garis konvergensi perspektif (vanishing points).
 *
 * @author Tim Praktikum Grafika Komputer
 */
public class Mesh3D {

    // Toggle tampilan elemen visual
    private boolean showGrid = true;
    private boolean showAxes = true;
    private boolean wireframe = false;

    /**
     * Menggambar seluruh scene 3D (Grid, Sumbu Koordinat, dan Model Rumah 3D).
     */
    public void draw() {
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        } else {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }

        // 1. Gambar Ground Grid referensi kedalaman
        if (showGrid) {
            drawGrid();
        }

        // 2. Gambar Sumbu Koordinat Kartesian
        if (showAxes) {
            drawAxes();
        }

        // 3. Gambar Model Rumah 3D
        drawHouseModel();

        // Kembalikan ke mode fill
        if (wireframe) {
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        }
    }

    /**
     * Menggambar model rumah 3D dengan warna sisi kontras:
     * - Dinding Utama (Kotak -1 s.d +1)
     * - Pintu & Jendela
     * - Atap Prisma Segitiga
     * - Cerobong Asap (Chimney)
     */
    private void drawHouseModel() {
        glBegin(GL_QUADS);

        // ==========================================
        // 1. DINDING DEPAN (Front Face: z = 1.0, Warna Oranye Terang)
        // ==========================================
        glColor3f(0.95f, 0.45f, 0.20f);
        glVertex3f(-1.0f, -0.5f,  1.0f);
        glVertex3f( 1.0f, -0.5f,  1.0f);
        glVertex3f( 1.0f,  0.8f,  1.0f);
        glVertex3f(-1.0f,  0.8f,  1.0f);

        // ==========================================
        // 2. DINDING BELAKANG (Back Face: z = -1.0, Warna Biru Keabuan)
        // ==========================================
        glColor3f(0.25f, 0.55f, 0.85f);
        glVertex3f( 1.0f, -0.5f, -1.0f);
        glVertex3f(-1.0f, -0.5f, -1.0f);
        glVertex3f(-1.0f,  0.8f, -1.0f);
        glVertex3f( 1.0f,  0.8f, -1.0f);

        // ==========================================
        // 3. DINDING KIRI (Left Face: x = -1.0, Warna Hijau Mint)
        // ==========================================
        glColor3f(0.20f, 0.75f, 0.50f);
        glVertex3f(-1.0f, -0.5f, -1.0f);
        glVertex3f(-1.0f, -0.5f,  1.0f);
        glVertex3f(-1.0f,  0.8f,  1.0f);
        glVertex3f(-1.0f,  0.8f, -1.0f);

        // ==========================================
        // 4. DINDING KANAN (Right Face: x = 1.0, Warna Kuning Emas)
        // ==========================================
        glColor3f(0.95f, 0.80f, 0.20f);
        glVertex3f( 1.0f, -0.5f,  1.0f);
        glVertex3f( 1.0f, -0.5f, -1.0f);
        glVertex3f( 1.0f,  0.8f, -1.0f);
        glVertex3f( 1.0f,  0.8f,  1.0f);

        // ==========================================
        // 5. LANTAI BAWAH RUMAH (Bottom Face: y = -0.5, Cokelat Tua)
        // ==========================================
        glColor3f(0.40f, 0.25f, 0.15f);
        glVertex3f(-1.0f, -0.5f, -1.0f);
        glVertex3f( 1.0f, -0.5f, -1.0f);
        glVertex3f( 1.0f, -0.5f,  1.0f);
        glVertex3f(-1.0f, -0.5f,  1.0f);

        // ==========================================
        // 6. ATAP MIRING KIRI (Roof Slope Left, Merah Bata Gelap)
        // ==========================================
        glColor3f(0.80f, 0.25f, 0.20f);
        glVertex3f(-1.1f, 0.75f,  1.1f);
        glVertex3f( 0.0f, 1.60f,  1.1f);
        glVertex3f( 0.0f, 1.60f, -1.1f);
        glVertex3f(-1.1f, 0.75f, -1.1f);

        // ==========================================
        // 7. ATAP MIRING KANAN (Roof Slope Right, Merah Marun)
        // ==========================================
        glColor3f(0.70f, 0.15f, 0.15f);
        glVertex3f( 0.0f, 1.60f,  1.1f);
        glVertex3f( 1.1f, 0.75f,  1.1f);
        glVertex3f( 1.1f, 0.75f, -1.1f);
        glVertex3f( 0.0f, 1.60f, -1.1f);

        // ==========================================
        // 8. PINTU DEPAN (z = 1.01 agar tidak z-fighting, Cokelat Kayu)
        // ==========================================
        glColor3f(0.35f, 0.18f, 0.08f);
        glVertex3f(-0.25f, -0.50f, 1.01f);
        glVertex3f( 0.25f, -0.50f, 1.01f);
        glVertex3f( 0.25f,  0.25f, 1.01f);
        glVertex3f(-0.25f,  0.25f, 1.01f);

        // ==========================================
        // 9. JENDELA KIRI & KANAN (Biru Langit Reflektif)
        // ==========================================
        glColor3f(0.60f, 0.85f, 0.95f);
        // Jendela Kiri
        glVertex3f(-0.80f, 0.0f, 1.01f);
        glVertex3f(-0.45f, 0.0f, 1.01f);
        glVertex3f(-0.45f, 0.5f, 1.01f);
        glVertex3f(-0.80f, 0.5f, 1.01f);

        // Jendela Kanan
        glVertex3f(0.45f, 0.0f, 1.01f);
        glVertex3f(0.80f, 0.0f, 1.01f);
        glVertex3f(0.80f, 0.5f, 1.01f);
        glVertex3f(0.45f, 0.5f, 1.01f);

        // ==========================================
        // 10. CEROBONG ASAP (Chimney)
        // ==========================================
        glColor3f(0.50f, 0.50f, 0.50f);
        // Sisi cerobong
        glVertex3f(0.4f, 1.0f, -0.6f);
        glVertex3f(0.7f, 1.0f, -0.6f);
        glVertex3f(0.7f, 1.9f, -0.6f);
        glVertex3f(0.4f, 1.9f, -0.6f);

        glVertex3f(0.7f, 1.0f, -0.3f);
        glVertex3f(0.4f, 1.0f, -0.3f);
        glVertex3f(0.4f, 1.9f, -0.3f);
        glVertex3f(0.7f, 1.9f, -0.3f);

        glVertex3f(0.7f, 1.0f, -0.6f);
        glVertex3f(0.7f, 1.0f, -0.3f);
        glVertex3f(0.7f, 1.9f, -0.3f);
        glVertex3f(0.7f, 1.9f, -0.6f);

        glVertex3f(0.4f, 1.0f, -0.3f);
        glVertex3f(0.4f, 1.0f, -0.6f);
        glVertex3f(0.4f, 1.9f, -0.6f);
        glVertex3f(0.4f, 1.9f, -0.3f);

        // Lubang atas cerobong
        glColor3f(0.20f, 0.20f, 0.20f);
        glVertex3f(0.4f, 1.9f, -0.6f);
        glVertex3f(0.7f, 1.9f, -0.6f);
        glVertex3f(0.7f, 1.9f, -0.3f);
        glVertex3f(0.4f, 1.9f, -0.3f);

        glEnd();

        // Segitiga gable atap depan dan belakang
        glBegin(GL_TRIANGLES);
        // Gable Depan (Kuning Kecokelatan)
        glColor3f(0.90f, 0.60f, 0.30f);
        glVertex3f(-1.0f, 0.8f,  1.0f);
        glVertex3f( 1.0f, 0.8f,  1.0f);
        glVertex3f( 0.0f, 1.55f, 1.0f);

        // Gable Belakang
        glColor3f(0.35f, 0.50f, 0.70f);
        glVertex3f( 1.0f, 0.8f, -1.0f);
        glVertex3f(-1.0f, 0.8f, -1.0f);
        glVertex3f( 0.0f, 1.55f, -1.0f);
        glEnd();
    }

    /**
     * Menggambar Sumbu Koordinat Kartesian 3D:
     * - Sumbu X: Merah
     * - Sumbu Y: Hijau
     * - Sumbu Z: Biru
     */
    private void drawAxes() {
        glLineWidth(2.5f);
        glBegin(GL_LINES);

        float len = 2.5f;

        // X-Axis (Merah)
        glColor3f(1.0f, 0.1f, 0.1f);
        glVertex3f(0.0f, -0.5f, 0.0f);
        glVertex3f(len,  -0.5f, 0.0f);

        // Y-Axis (Hijau)
        glColor3f(0.1f, 1.0f, 0.1f);
        glVertex3f(0.0f, -0.5f, 0.0f);
        glVertex3f(0.0f, len,   0.0f);

        // Z-Axis (Biru)
        glColor3f(0.2f, 0.4f, 1.0f);
        glVertex3f(0.0f, -0.5f, 0.0f);
        glVertex3f(0.0f, -0.5f, len);

        glEnd();
        glLineWidth(1.0f);
    }

    /**
     * Menggambar grid lantai pada bidang XZ (y = -0.5) untuk visualisasi
     * efek proyeksi, foreshortening, garis konvergensi, dan stereo parallax.
     */
    private void drawGrid() {
        glLineWidth(1.0f);
        glBegin(GL_LINES);

        glColor3f(0.35f, 0.35f, 0.40f);

        float extent = 6.0f;
        float step = 1.0f;
        float y = -0.5f;

        for (float i = -extent; i <= extent; i += step) {
            // Garis sejajar sumbu Z
            glVertex3f(i, y, -extent);
            glVertex3f(i, y,  extent);

            // Garis sejajar sumbu X
            glVertex3f(-extent, y, i);
            glVertex3f( extent, y, i);
        }

        glEnd();
    }

    // --- Toggle Settings ---

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public void toggleGrid() {
        this.showGrid = !this.showGrid;
    }

    public boolean isShowAxes() {
        return showAxes;
    }

    public void setShowAxes(boolean showAxes) {
        this.showAxes = showAxes;
    }

    public void toggleAxes() {
        this.showAxes = !this.showAxes;
    }

    public boolean isWireframe() {
        return wireframe;
    }

    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }

    public void toggleWireframe() {
        this.wireframe = !this.wireframe;
    }
}
