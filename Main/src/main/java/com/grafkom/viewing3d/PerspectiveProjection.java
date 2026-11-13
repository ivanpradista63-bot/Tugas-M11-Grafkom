package com.grafkom.viewing3d;

import org.joml.Matrix4f;

/**
 * Kelas PerspectiveProjection menangani pembentukan frustum dan matriks proyeksi
 * perspektif secara matematis (Indikator 2).
 *
 * Mendukung proyeksi perspektif simetris standar dengan perhitungan manual,
 * proyeksi frustum asimetris (off-axis) untuk kebutuhan stereo rendering, serta
 * modifikasi FOV dinamis secara interaktif (efek dolly zoom).
 *
 * @author Tim Praktikum Grafika Komputer
 */
public class PerspectiveProjection {
    // Parameter proyeksi perspektif
    private float fov = 45.0f;          // Field of View vertikal dalam derajat
    private float aspect = 16.0f / 9.0f;// Aspect ratio layar (width / height)
    private float zNear = 0.1f;         // Jarak bidang potong dekat (near clipping plane)
    private float zFar = 100.0f;        // Jarak bidang potong jauh (far clipping plane)

    // Matriks Proyeksi
    private final Matrix4f projectionMatrix = new Matrix4f();

    // Nilai default untuk reset
    private final float defaultFov;

    /**
     * Konstruktor default dengan FOV 45 derajat, rasio 16:9, near 0.1, far 100.0.
     */
    public PerspectiveProjection() {
        this(45.0f, 16.0f / 9.0f, 0.1f, 100.0f);
    }

    /**
     * Konstruktor dengan parameter lengkap.
     *
     * @param fov   Field of View vertikal (derajat)
     * @param aspect Rasio aspek lebar terhadap tinggi layar
     * @param zNear Bidang potong dekat (harus > 0)
     * @param zFar  Bidang potong jauh (harus > zNear)
     */
    public PerspectiveProjection(float fov, float aspect, float zNear, float zFar) {
        this.fov = fov;
        this.defaultFov = fov;
        this.aspect = aspect;
        this.zNear = zNear;
        this.zFar = zFar;
        updateProjectionMatrix();
    }

    /**
     * Menghitung matriks proyeksi perspektif standar secara manual:
     * f = 1 / tan(FOV_vertikal / 2)
     *
     * Matriks Proyeksi Perspektif:
     * [ f/aspect      0                   0                                0                 ]
     * [    0          f                   0                                0                 ]
     * [    0          0      (zFar + zNear) / (zNear - zFar)   (2 * zFar * zNear) / (zNear - zFar)]
     * [    0          0                  -1                                0                 ]
     */
    public void updateProjectionMatrix() {
        float fovRad = (float) Math.toRadians(fov);
        float f = 1.0f / (float) Math.tan(fovRad / 2.0f);
        float nf = 1.0f / (zNear - zFar);

        // Susun matriks kolom per kolom (JOML column-major layout)
        projectionMatrix.set(
            f / aspect, 0.0f, 0.0f,                     0.0f,  // Kolom 0
            0.0f,       f,    0.0f,                     0.0f,  // Kolom 1
            0.0f,       0.0f, (zFar + zNear) * nf,      -1.0f, // Kolom 2
            0.0f,       0.0f, (2.0f * zFar * zNear) * nf, 0.0f   // Kolom 3
        );
    }

    /**
     * Menghasilkan matriks frustum asimetris (off-axis) manual.
     * Sangat esensial untuk Stereo Anaglyph off-axis viewing agar tidak menimbulkan
     * distorsi keystone yang biasa terjadi pada metode toe-in.
     *
     * Matriks glFrustum OpenGL:
     * [ 2*near/(r-l)        0           (r+l)/(r-l)            0              ]
     * [      0         2*near/(t-b)     (t+b)/(t-b)            0              ]
     * [      0              0        -(far+near)/(far-near) -(2*far*near)/(far-near)]
     * [      0              0               -1                 0              ]
     *
     * @param left   koordinat batas kiri bidang potong dekat
     * @param right  koordinat batas kanan bidang potong dekat
     * @param bottom koordinat batas bawah bidang potong dekat
     * @param top    koordinat batas atas bidang potong dekat
     * @param near   jarak bidang potong dekat
     * @param far    jarak bidang potong jauh
     * @param targetMatrix matriks penampung hasil perhitungan
     * @return matriks targetMatrix yang telah terisi
     */
    public static Matrix4f createAsymmetricFrustum(float left, float right, float bottom, float top,
                                                   float near, float far, Matrix4f targetMatrix) {
        float rl = 1.0f / (right - left);
        float tb = 1.0f / (top - bottom);
        float fn = 1.0f / (far - near);

        targetMatrix.set(
            (2.0f * near) * rl, 0.0f,                 0.0f,                   0.0f,
            0.0f,               (2.0f * near) * tb,   0.0f,                   0.0f,
            (right + left) * rl,(top + bottom) * tb, -(far + near) * fn,     -1.0f,
            0.0f,               0.0f,                -(2.0f * far * near) * fn, 0.0f
        );

        return targetMatrix;
    }

    /**
     * Mengatur FOV secara interaktif untuk efek dolly zoom.
     *
     * @param delta perubahan FOV dalam derajat
     */
    public void adjustFov(float delta) {
        this.fov += delta;
        // Batasi rentang FOV agar tetap realistis
        if (this.fov < 10.0f) this.fov = 10.0f;
        if (this.fov > 120.0f) this.fov = 120.0f;
        updateProjectionMatrix();
    }

    /**
     * Memperbarui rasio aspek saat ukuran jendela (viewport) berubah.
     *
     * @param width  lebar jendela
     * @param height tinggi jendela
     */
    public void updateAspect(int width, int height) {
        if (height <= 0) height = 1;
        this.aspect = (float) width / (float) height;
        updateProjectionMatrix();
    }

    /**
     * Reset FOV ke nilai default awal.
     */
    public void reset() {
        this.fov = defaultFov;
        updateProjectionMatrix();
    }

    // --- Getter dan Setter ---

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = Math.max(10.0f, Math.min(120.0f, fov));
        updateProjectionMatrix();
    }

    public float getAspect() {
        return aspect;
    }

    public void setAspect(float aspect) {
        this.aspect = aspect;
        updateProjectionMatrix();
    }

    public float getNear() {
        return zNear;
    }

    public void setNear(float zNear) {
        this.zNear = zNear;
        updateProjectionMatrix();
    }

    public float getFar() {
        return zFar;
    }

    public void setFar(float zFar) {
        this.zFar = zFar;
        updateProjectionMatrix();
    }
}
