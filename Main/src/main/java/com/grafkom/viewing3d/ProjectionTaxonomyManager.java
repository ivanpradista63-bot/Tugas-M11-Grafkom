package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kelas ProjectionTaxonomyManager mengelola klasifikasi taksonomi proyeksi
 * dalam Grafika Komputer (Indikator 4):
 *
 * 1. Proyeksi Paralel:
 *    - Orthographic (Ortogonal)
 *    - Axonometric: Isometric (Isometrik)
 *    - Oblique: Cavalier (skala kedalaman 1.0) dan Cabinet (skala kedalaman 0.5)
 *
 * 2. Proyeksi Perspektif:
 *    - 1 Titik Hilang (One-Point Perspective)
 *    - 2 Titik Hilang (Two-Point Perspective)
 *    - 3 Titik Hilang (Three-Point Perspective)
 *
 * Menyediakan matriks proyeksi dan konfigurasi sudut pandang kamera yang sesuai
 * dengan prinsip teoritis masing-masing kategori proyeksi.
 *
 * @author Tim Praktikum Grafika Komputer
 */
public class ProjectionTaxonomyManager {

    /**
     * Enum jenis-jenis proyeksi sesuai taksonomi grafika komputer.
     */
    public enum ProjectionType {
        ORTHOGRAPHIC("Orthographic (Paralel Tegak)"),
        ISOMETRIC("Axonometric - Isometric (Sudut 120°)"),
        OBLIQUE_CAVALIER("Oblique - Cavalier (Sudut 45°, Skala 1.0)"),
        OBLIQUE_CABINET("Oblique - Cabinet (Sudut 45°, Skala 0.5)"),
        PERSPECTIVE_1_POINT("Perspektif 1 Titik Hilang (Frontal)"),
        PERSPECTIVE_2_POINT("Perspektif 2 Titik Hilang (Elevasi 0°)"),
        PERSPECTIVE_3_POINT("Perspektif 3 Titik Hilang (Sudut Tiga Sumbu)");

        private final String description;

        ProjectionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private ProjectionType currentType = ProjectionType.PERSPECTIVE_3_POINT;

    // Parameter proyeksi ortografis / oblique
    private float orthoSize = 3.5f;
    private float orthoNear = -20.0f;
    private float orthoFar = 20.0f;

    // Matriks hasil kalkulasi
    private final Matrix4f calculatedProjection = new Matrix4f();
    private final Matrix4f calculatedView = new Matrix4f();

    /**
     * Mengatur tipe proyeksi yang aktif dan mengonfigurasi kamera bila perlu.
     *
     * @param type   tipe proyeksi yang dipilih
     * @param camera kamera untuk disesuaikan sudut pandangnya
     */
    public void setProjectionType(ProjectionType type, Camera3D camera) {
        this.currentType = type;

        switch (type) {
            case ORTHOGRAPHIC:
                // Sudut bebas atau pandangan tampak atas/depan
                break;

            case ISOMETRIC:
                // Isometrik: Azimuth 45°, Elevasi = asin(tan(30°)) ≈ 35.264°
                float isoElevation = (float) Math.toDegrees(Math.asin(Math.tan(Math.toRadians(30.0))));
                camera.setAzimuth(45.0f);
                camera.setElevation(isoElevation);
                break;

            case OBLIQUE_CAVALIER:
            case OBLIQUE_CABINET:
                // Oblique: Kamera menghadap lurus ke bidang frontal (Azimuth 0°, Elevasi 0°)
                camera.setAzimuth(0.0f);
                camera.setElevation(0.0f);
                break;

            case PERSPECTIVE_1_POINT:
                // 1 Titik Hilang: Menghadap tegak lurus bidang depan (Azimuth 0°, Elevasi 0°)
                // Garis kedalaman sumbu Z konvergen ke satu titik
                camera.setAzimuth(0.0f);
                camera.setElevation(0.0f);
                break;

            case PERSPECTIVE_2_POINT:
                // 2 Titik Hilang: Kamera berputar horizontal (Azimuth 45°), elevasi tetap 0°
                // Garis vertikal tetap tegak paralel, garis X dan Z konvergen ke 2 titik horizon
                camera.setAzimuth(45.0f);
                camera.setElevation(0.0f);
                break;

            case PERSPECTIVE_3_POINT:
                // 3 Titik Hilang: Kamera berada pada sudut elevasi dan azimuth (birds-eye view)
                // Ketiga sumbu koordinat konvergen ke 3 titik hilang
                camera.setAzimuth(45.0f);
                camera.setElevation(30.0f);
                break;
        }
    }

    /**
     * Menghitung matriks proyeksi berdasarkan tipe taksonomi yang sedang aktif.
     *
     * @param perspective default perspective projection calculator
     * @return matriks proyeksi hasil kalkulasi
     */
    public Matrix4f computeProjectionMatrix(PerspectiveProjection perspective) {
        float aspect = perspective.getAspect();

        switch (currentType) {
            case ORTHOGRAPHIC:
            case ISOMETRIC:
                // Proyeksi Ortogonal standar tanpa distorsi perspektif
                buildOrthoMatrix(aspect, calculatedProjection);
                break;

            case OBLIQUE_CAVALIER:
                // Cavalier: Skala kedalaman = 1.0, Sudut shear = 45 derajat
                buildObliqueMatrix(aspect, 1.0f, 45.0f, calculatedProjection);
                break;

            case OBLIQUE_CABINET:
                // Cabinet: Skala kedalaman = 0.5, Sudut shear = 45 derajat (tampak lebih natural)
                buildObliqueMatrix(aspect, 0.5f, 45.0f, calculatedProjection);
                break;

            case PERSPECTIVE_1_POINT:
            case PERSPECTIVE_2_POINT:
            case PERSPECTIVE_3_POINT:
            default:
                // Matriks perspektif standar dari PerspectiveProjection
                calculatedProjection.set(perspective.getProjectionMatrix());
                break;
        }

        return calculatedProjection;
    }

    /**
     * Membentuk matriks Orthographic secara manual:
     * [ 2/(r-l)     0          0       -(r+l)/(r-l) ]
     * [   0      2/(t-b)       0       -(t+b)/(t-b) ]
     * [   0         0      -2/(f-n)    -(f+n)/(f-n) ]
     * [   0         0          0             1      ]
     */
    private void buildOrthoMatrix(float aspect, Matrix4f target) {
        float left = -orthoSize * aspect;
        float right = orthoSize * aspect;
        float bottom = -orthoSize;
        float top = orthoSize;
        float near = orthoNear;
        float far = orthoFar;

        float rl = 1.0f / (right - left);
        float tb = 1.0f / (top - bottom);
        float fn = 1.0f / (far - near);

        target.set(
            2.0f * rl,        0.0f,             0.0f,              0.0f,
            0.0f,             2.0f * tb,        0.0f,              0.0f,
            0.0f,             0.0f,             -2.0f * fn,        0.0f,
            -(right+left)*rl, -(top+bottom)*tb, -(far+near)*fn,    1.0f
        );
    }

    /**
     * Membentuk matriks Proyeksi Oblique (Miring) dengan mengalikan matriks Orthographic
     * dengan Matriks Shear (Geser) kedalaman:
     *
     * Transformasi Shear:
     * x' = x + z * (depthScale * cos(alpha))
     * y' = y + z * (depthScale * sin(alpha))
     * z' = z
     *
     * @param aspect     rasio aspek layar
     * @param depthScale faktor skala sumbu kedalaman (1.0 untuk Cavalier, 0.5 untuk Cabinet)
     * @param angleDeg   sudut orientasi sumbu kedalaman (derajat, umumnya 45°)
     * @param target     matriks tujuan
     */
    private void buildObliqueMatrix(float aspect, float depthScale, float angleDeg, Matrix4f target) {
        // 1. Buat matriks ortografis dasar
        Matrix4f ortho = new Matrix4f();
        buildOrthoMatrix(aspect, ortho);

        // 2. Buat matriks shear
        float rad = (float) Math.toRadians(angleDeg);
        float shearX = depthScale * (float) Math.cos(rad);
        float shearY = depthScale * (float) Math.sin(rad);

        Matrix4f shear = new Matrix4f().identity();
        // JOML column-major: kolom 2 memuat pengaruh z terhadap x dan y
        shear.m20(shearX); // row 0, col 2
        shear.m21(shearY); // row 1, col 2

        // 3. Kalikan: Proyeksi Oblique = Ortho * Shear
        ortho.mul(shear, target);
    }

    /**
     * Menghitung View Matrix yang disesuaikan dengan tipe proyeksi.
     * Untuk proyeksi oblique, posisi kamera disesuaikan pada sumbu frontal.
     *
     * @param camera kamera orbital utama
     * @return matriks view aktif
     */
    public Matrix4f computeViewMatrix(Camera3D camera) {
        if (currentType == ProjectionType.OBLIQUE_CAVALIER || currentType == ProjectionType.OBLIQUE_CABINET) {
            // Frontal view looking straight along -Z
            Vector3f eye = new Vector3f(0.0f, 0.5f, 6.0f);
            Vector3f target = new Vector3f(0.0f, 0.5f, 0.0f);
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

            Vector3f n = new Vector3f(eye).sub(target).normalize();
            Vector3f u = new Vector3f(up).cross(n).normalize();
            Vector3f v = new Vector3f(n).cross(u).normalize();

            calculatedView.set(
                u.x, v.x, n.x, 0.0f,
                u.y, v.y, n.y, 0.0f,
                u.z, v.z, n.z, 0.0f,
                -u.dot(eye), -v.dot(eye), -n.dot(eye), 1.0f
            );
            return calculatedView;
        }

        return camera.getViewMatrix();
    }

    // --- Getter & Setter ---

    public ProjectionType getCurrentType() {
        return currentType;
    }

    public void setOrthoSize(float orthoSize) {
        this.orthoSize = Math.max(0.5f, Math.min(20.0f, orthoSize));
    }

    public float getOrthoSize() {
        return orthoSize;
    }

    public void adjustOrthoSize(float delta) {
        this.orthoSize += delta;
        if (this.orthoSize < 0.5f) this.orthoSize = 0.5f;
        if (this.orthoSize > 20.0f) this.orthoSize = 20.0f;
    }
}
