package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Kelas Camera3D merepresentasikan model kamera sintetis 3D (Indikator 1).
 * Mengimplementasikan sistem koordinat kamera (u, v, n), perhitungan View Matrix
 * (LookAt) secara manual sesuai formulasi matematis grafika komputer, serta
 * sistem navigasi orbital (turntable) berbasis koordinat sferis.
 *
 * @author Tim Praktikum Grafika Komputer
 */
public class Camera3D {
    // Parameter dasar kamera
    private Vector3f eye;     // Posisi kamera di ruang dunia (E)
    private Vector3f target;  // Titik yang dilihat oleh kamera (At / LookAt center)
    private Vector3f up;      // Vektor penunjuk arah atas kamera (Up)

    // Sistem koordinat kamera ortonormal (u, v, n)
    private final Vector3f u = new Vector3f(); // Sumbu horizontal kamera (Right)
    private final Vector3f v = new Vector3f(); // Sumbu vertikal kamera (Up kamera)
    private final Vector3f n = new Vector3f(); // Sumbu kedalaman kamera (arah berlawanan arah pandang)

    // Matriks Transformasi View (LookAt)
    private final Matrix4f viewMatrix = new Matrix4f();

    // Parameter navigasi orbital (koordinat sferis)
    private float radius = 6.0f;       // Jarak kamera dari titik target
    private float azimuth = 45.0f;     // Sudut rotasi horizontal (theta / yaw) dalam derajat
    private float elevation = 30.0f;   // Sudut rotasi vertikal (phi / pitch) dalam derajat

    // Batas sudut elevasi untuk mencegah fenomena gimbal lock / flip kamera
    private static final float MIN_ELEVATION = -89.0f;
    private static final float MAX_ELEVATION = 89.0f;

    // Nilai default untuk operasi reset
    private final float defaultRadius;
    private final float defaultAzimuth;
    private final float defaultElevation;

    /**
     * Konstruktor default dengan posisi orbital awal standar.
     */
    public Camera3D() {
        this(new Vector3f(0.0f, 0.5f, 0.0f), 6.0f, 45.0f, 30.0f);
    }

    /**
     * Konstruktor kamera dengan target dan posisi orbital yang dapat ditentukan.
     *
     * @param target          titik fokus pandangan kamera
     * @param initialRadius   jarak awal kamera dari target
     * @param initialAzimuth  sudut azimuth awal (derajat)
     * @param initialElevation sudut elevasi awal (derajat)
     */
    public Camera3D(Vector3f target, float initialRadius, float initialAzimuth, float initialElevation) {
        this.target = new Vector3f(target);
        this.up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.eye = new Vector3f();

        this.radius = initialRadius;
        this.azimuth = initialAzimuth;
        this.elevation = initialElevation;

        this.defaultRadius = initialRadius;
        this.defaultAzimuth = initialAzimuth;
        this.defaultElevation = initialElevation;

        updateOrbitalPosition();
    }

    /**
     * Memperbarui posisi 'eye' kamera dari koordinat sferis (radius, azimuth, elevasi)
     * ke koordinat Kartesian relatif terhadap target:
     * x = target.x + radius * cos(elevation) * sin(azimuth)
     * y = target.y + radius * sin(elevation)
     * z = target.z + radius * cos(elevation) * cos(azimuth)
     */
    public void updateOrbitalPosition() {
        // Normalisasi elevasi agar tidak melebihi batas sudut yang ditentukan
        if (elevation > MAX_ELEVATION) elevation = MAX_ELEVATION;
        if (elevation < MIN_ELEVATION) elevation = MIN_ELEVATION;

        // Jaga azimuth tetap dalam rentang [0, 360)
        azimuth = (azimuth % 360.0f + 360.0f) % 360.0f;

        float radAzimuth = (float) Math.toRadians(azimuth);
        float radElevation = (float) Math.toRadians(elevation);

        float cosElev = (float) Math.cos(radElevation);
        float sinElev = (float) Math.sin(radElevation);
        float sinAzim = (float) Math.sin(radAzimuth);
        float cosAzim = (float) Math.cos(radAzimuth);

        eye.x = target.x + radius * cosElev * sinAzim;
        eye.y = target.y + radius * sinElev;
        eye.z = target.z + radius * cosElev * cosAzim;

        updateViewMatrix();
    }

    /**
     * Menghitung View Matrix (LookAt) secara manual sesuai teori:
     * 1. n = normalize(eye - target)
     * 2. u = normalize(up x n)
     * 3. v = n x u
     *
     * Matriks View (V = R * T):
     * [  u.x   u.y   u.z  -dot(u, eye) ]
     * [  v.x   v.y   v.z  -dot(v, eye) ]
     * [  n.x   n.y   n.z  -dot(n, eye) ]
     * [   0     0     0         1      ]
     */
    public void updateViewMatrix() {
        // 1. Vektor sumbu kedalaman (n) mengarah dari target ke eye
        n.set(eye).sub(target).normalize();

        // Antisipasi bila eye berimpit dengan target
        if (n.lengthSquared() < 1e-6f) {
            n.set(0.0f, 0.0f, 1.0f);
        }

        // 2. Vektor sumbu horizontal (u) tegak lurus terhadap bidang (up, n)
        u.set(up).cross(n).normalize();

        // Antisipasi bila n sejajar dengan up (kondisi kutub)
        if (u.lengthSquared() < 1e-6f) {
            u.set(1.0f, 0.0f, 0.0f);
        }

        // 3. Vektor sumbu vertikal (v) ortogonal terhadap n dan u
        v.set(n).cross(u).normalize();

        // 4. Susun matriks LookAt secara manual (Column-major pada JOML Matrix4f)
        float dotU = u.dot(eye);
        float dotV = v.dot(eye);
        float dotN = n.dot(eye);

        // JOML Matrix4f: m[col][row]
        viewMatrix.set(
            u.x,   v.x,   n.x,   0.0f,   // Kolom 0
            u.y,   v.y,   n.y,   0.0f,   // Kolom 1
            u.z,   v.z,   n.z,   0.0f,   // Kolom 2
            -dotU, -dotV, -dotN, 1.0f    // Kolom 3 (Translasi)
        );
    }

    /**
     * Melakukan navigasi orbit dengan mengubah azimuth dan elevasi kamera.
     *
     * @param deltaAzimuth   perubahan sudut azimuth (derajat)
     * @param deltaElevation perubahan sudut elevasi (derajat)
     */
    public void orbit(float deltaAzimuth, float deltaElevation) {
        this.azimuth += deltaAzimuth;
        this.elevation += deltaElevation;
        updateOrbitalPosition();
    }

    /**
     * Mengatur jarak zoom kamera (radius orbital).
     *
     * @param deltaRadius perubahan radius (nilai positif menjauh, negatif mendekat)
     */
    public void zoom(float deltaRadius) {
        this.radius += deltaRadius;
        if (this.radius < 1.0f) this.radius = 1.0f;
        if (this.radius > 50.0f) this.radius = 50.0f;
        updateOrbitalPosition();
    }

    /**
     * Mereset parameter kamera ke kondisi default.
     */
    public void reset() {
        this.radius = defaultRadius;
        this.azimuth = defaultAzimuth;
        this.elevation = defaultElevation;
        updateOrbitalPosition();
    }

    // --- Getter dan Setter ---

    public Vector3f getEye() {
        return eye;
    }

    public void setEye(Vector3f newEye) {
        this.eye.set(newEye);
        updateViewMatrix();
    }

    public Vector3f getTarget() {
        return target;
    }

    public void setTarget(Vector3f target) {
        this.target.set(target);
        updateOrbitalPosition();
    }

    public Vector3f getUp() {
        return up;
    }

    public void setUp(Vector3f up) {
        this.up.set(up);
        updateOrbitalPosition();
    }

    public Vector3f getU() {
        return u;
    }

    public Vector3f getV() {
        return v;
    }

    public Vector3f getN() {
        return n;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = Math.max(1.0f, Math.min(50.0f, radius));
        updateOrbitalPosition();
    }

    public float getAzimuth() {
        return azimuth;
    }

    public void setAzimuth(float azimuth) {
        this.azimuth = azimuth;
        updateOrbitalPosition();
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
        updateOrbitalPosition();
    }
}
