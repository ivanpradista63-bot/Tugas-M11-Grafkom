package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;

import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Aplikasi Utama: 3D Viewing and Projection Interactive Visualizer
 * Praktikum Grafika Komputer Minggu ke-10.
 *
 * Mengintegrasikan:
 * 1. Camera3D (Model kamera sintetis & matriks LookAt manual & navigasi orbital)
 * 2. PerspectiveProjection (Frustum perspektif & asymmetric off-axis & dynamic FOV)
 * 3. StereoAnaglyphRenderer (Stereo anaglyph merah-cyan off-axis & dynamic IPD)
 * 4. ProjectionTaxonomyManager (7 varian taksonomi proyeksi paralel & perspektif)
 * 5. Mesh3D (Model 3D rumah beratap prisma, sumbu koordinat, dan ground grid)
 *
 * @author Tim Praktikum Grafika Komputer
 */
public class ThreeDViewing {

    // Ukuran window awal
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private long windowHandle;

    // Komponen Sistem Visualisasi
    private Camera3D camera;
    private PerspectiveProjection perspective;
    private StereoAnaglyphRenderer stereoRenderer;
    private ProjectionTaxonomyManager taxonomyManager;
    private Mesh3D mesh;

    // Status Interaksi
    private boolean stereoEnabled = false;
    private boolean autoRotate = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;
    private boolean mousePressed = false;

    // Buffer untuk transfer matriks JOML ke OpenGL
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }

    public void run() {
        printInstructions();
        initWindow();
        initOpenGL();
        initVisualization();
        loop();
        cleanup();
    }

    /**
     * Menampilkan panduan tombol kontrol pada terminal/konsol.
     */
    private void printInstructions() {
        System.out.println("================================================================================");
        System.out.println("   PRAKTIKUM GRAFIKA KOMPUTER MINGGU 10: 3D VIEWING, PROYEKSI & STEREO VIEW     ");
        System.out.println("================================================================================");
        System.out.println("[KONTROL TAKSONOMI PROYEKSI]");
        System.out.println("  1 : Proyeksi Orthographic (Paralel Tegak)");
        System.out.println("  2 : Proyeksi Axonometric - Isometric");
        System.out.println("  3 : Proyeksi Oblique - Cavalier (Sudut 45°, Skala 1.0)");
        System.out.println("  4 : Proyeksi Oblique - Cabinet (Sudut 45°, Skala 0.5)");
        System.out.println("  5 : Proyeksi Perspektif 1 Titik Hilang (Frontal)");
        System.out.println("  6 : Proyeksi Perspektif 2 Titik Hilang (Elevasi 0°)");
        System.out.println("  7 : Proyeksi Perspektif 3 Titik Hilang (Orbital Bebas)");
        System.out.println();
        System.out.println("[KONTROL STEREO ANAGLYPH]");
        System.out.println("  S     : Toggle Stereo Anaglyph Merah-Cyan (ON / OFF)");
        System.out.println("  [ / ] : Kurangi / Tambah IPD (Interpupillary Distance)");
        System.out.println("  - / = : Kurangi / Tambah Jarak Bidang Fokal (Zero Parallax Plane)");
        System.out.println();
        System.out.println("[NAVIGASI KAMERA & FOV]");
        System.out.println("  Klik Kiri Mouse + Drag : Rotasi Orbit Kamera (Azimuth & Elevasi)");
        System.out.println("  Scroll Mouse           : Zoom In / Zoom Out (Jarak Kamera)");
        System.out.println("  Z / X                  : Dolly Zoom (Ubah Field of View / FOV)");
        System.out.println("  SPASI                  : Toggle Rotasi Otomatis (Turntable)");
        System.out.println("  R                      : Reset Kamera & Proyeksi ke Kondisi Awal");
        System.out.println();
        System.out.println("[VISUALISASI]");
        System.out.println("  G : Toggle Ground Grid");
        System.out.println("  A : Toggle Sumbu Koordinat");
        System.out.println("  W : Toggle Wireframe / Solid Mode");
        System.out.println("  ESC : Keluar dari Aplikasi");
        System.out.println("================================================================================");
    }

    /**
     * Inisialisasi jendela GLFW dan event listener.
     */
    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, "3D Viewing and Projection Visualizer", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Gagal membuat jendela GLFW!");
        }

        // Setup Callback Keyboard
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                handleKeyEvent(key, action);
            }
        });

        // Setup Callback Mouse Button
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    mousePressed = true;
                    DoubleBuffer xpos = BufferUtils.createDoubleBuffer(1);
                    DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);
                    glfwGetCursorPos(windowHandle, xpos, ypos);
                    lastMouseX = xpos.get(0);
                    lastMouseY = ypos.get(0);
                } else if (action == GLFW_RELEASE) {
                    mousePressed = false;
                }
            }
        });

        // Setup Callback Gerakan Mouse
        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            if (mousePressed) {
                float dx = (float) (xpos - lastMouseX);
                float dy = (float) (ypos - lastMouseY);

                float sensitivity = 0.35f;
                camera.orbit(dx * sensitivity, -dy * sensitivity);

                lastMouseX = xpos;
                lastMouseY = ypos;
            }
        });

        // Setup Callback Mouse Scroll (Zoom)
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            if (taxonomyManager.getCurrentType() == ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC ||
                taxonomyManager.getCurrentType() == ProjectionTaxonomyManager.ProjectionType.ISOMETRIC ||
                taxonomyManager.getCurrentType() == ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER ||
                taxonomyManager.getCurrentType() == ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CABINET) {
                taxonomyManager.adjustOrthoSize((float) -yoffset * 0.3f);
            } else {
                camera.zoom((float) -yoffset * 0.5f);
            }
        });

        // Setup Callback Ukuran Framebuffer
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            this.windowWidth = width;
            this.windowHeight = height;
            glViewport(0, 0, width, height);
            perspective.updateAspect(width, height);
        });

        // Posisikan window di tengah layar monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - windowWidth) / 2,
                (vidmode.height() - windowHeight) / 2
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // V-Sync aktif
        glfwShowWindow(windowHandle);
    }

    /**
     * Inisialisasi OpenGL State.
     */
    private void initOpenGL() {
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        // Blending diaktifkan untuk anaglyph stereoskopik (dua pass color blending)
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE); // Additive blending untuk penggabungan Red dan Cyan

        glClearColor(0.12f, 0.12f, 0.15f, 1.0f);
        glViewport(0, 0, windowWidth, windowHeight);
    }

    /**
     * Inisialisasi Objek Visualisasi.
     */
    private void initVisualization() {
        camera = new Camera3D();
        perspective = new PerspectiveProjection(45.0f, (float) windowWidth / windowHeight, 0.1f, 100.0f);
        stereoRenderer = new StereoAnaglyphRenderer(0.12f, 6.0f);
        taxonomyManager = new ProjectionTaxonomyManager();
        mesh = new Mesh3D();

        // Terapkan tipe proyeksi awal (Perspektif 3 Titik Hilang)
        taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3_POINT, camera);
    }

    /**
     * Menangani input tombol keyboard.
     */
    private void handleKeyEvent(int key, int action) {
        if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
            glfwSetWindowShouldClose(windowHandle, true);
        }

        // Pilihan Taksonomi Proyeksi (1 s.d 7)
        if (key == GLFW_KEY_1) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC, camera);
        } else if (key == GLFW_KEY_2) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.ISOMETRIC, camera);
        } else if (key == GLFW_KEY_3) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER, camera);
        } else if (key == GLFW_KEY_4) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CABINET, camera);
        } else if (key == GLFW_KEY_5) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1_POINT, camera);
        } else if (key == GLFW_KEY_6) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_2_POINT, camera);
        } else if (key == GLFW_KEY_7) {
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3_POINT, camera);
        }

        // Toggle Stereo Anaglyph
        if (key == GLFW_KEY_S && action == GLFW_PRESS) {
            stereoEnabled = !stereoEnabled;
            if (stereoEnabled) {
                // Pastikan berada pada mode perspektif saat stereo diaktifkan
                if (taxonomyManager.getCurrentType() != ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3_POINT) {
                    taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3_POINT, camera);
                }
            }
        }

        // Kontrol IPD Dinamis
        if (key == GLFW_KEY_LEFT_BRACKET) {
            stereoRenderer.adjustIpd(-0.01f);
        } else if (key == GLFW_KEY_RIGHT_BRACKET) {
            stereoRenderer.adjustIpd(0.01f);
        }

        // Kontrol Jarak Bidang Fokal Dinamis
        if (key == GLFW_KEY_MINUS) {
            stereoRenderer.adjustFocalDistance(-0.25f);
        } else if (key == GLFW_KEY_EQUAL) {
            stereoRenderer.adjustFocalDistance(0.25f);
        }

        // Kontrol FOV Interaktif (Dolly Zoom)
        if (key == GLFW_KEY_Z) {
            perspective.adjustFov(-1.5f);
        } else if (key == GLFW_KEY_X) {
            perspective.adjustFov(1.5f);
        }

        // Toggle Visual
        if (key == GLFW_KEY_G && action == GLFW_PRESS) mesh.toggleGrid();
        if (key == GLFW_KEY_A && action == GLFW_PRESS) mesh.toggleAxes();
        if (key == GLFW_KEY_W && action == GLFW_PRESS) mesh.toggleWireframe();
        if (key == GLFW_KEY_SPACE && action == GLFW_PRESS) autoRotate = !autoRotate;

        // Reset
        if (key == GLFW_KEY_R && action == GLFW_PRESS) {
            camera.reset();
            perspective.reset();
            stereoRenderer.setIpd(0.12f);
            stereoRenderer.setFocalDistance(6.0f);
            taxonomyManager.setProjectionType(ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3_POINT, camera);
            autoRotate = false;
        }
    }

    /**
     * Main Render Loop.
     */
    private void loop() {
        long lastTime = System.currentTimeMillis();

        while (!glfwWindowShouldClose(windowHandle)) {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastTime) / 1000.0f;
            lastTime = currentTime;

            // Update auto-rotasi kamera jika diaktifkan
            if (autoRotate) {
                camera.orbit(25.0f * deltaTime, 0.0f);
            }

            // Bersihkan Color Buffer dan Depth Buffer
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (stereoEnabled) {
                // ==========================================
                // STEREO ANAGLYPH RENDERING (OFF-AXIS)
                // ==========================================
                stereoRenderer.render(camera, perspective, (proj, view) -> mesh.draw());
            } else {
                // ==========================================
                // STANDARD MONO RENDERING
                // ==========================================
                Matrix4f projMatrix = taxonomyManager.computeProjectionMatrix(perspective);
                Matrix4f viewMatrix = taxonomyManager.computeViewMatrix(camera);

                // Muat matriks ke OpenGL Pipeline
                loadProjectionMatrix(projMatrix);
                loadViewMatrix(viewMatrix);

                // Render geometri scene
                mesh.draw();
            }

            // Update info status pada judul jendela
            updateWindowTitle();

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    /**
     * Memperbarui judul jendela untuk memonitor parameter grafika secara real-time.
     */
    private void updateWindowTitle() {
        String stereoStatus = stereoEnabled ?
            String.format("[STEREO ON | IPD: %.3fm | FocDist: %.1fm]", stereoRenderer.getIpd(), stereoRenderer.getFocalDistance()) :
            "[STEREO OFF]";

        String title = String.format("3D Viewing Visualizer | Mode: %s | FOV: %.1f° | Cam: (Az: %.0f°, El: %.0f°, R: %.1f) | %s",
            taxonomyManager.getCurrentType().getDescription(),
            perspective.getFov(),
            camera.getAzimuth(),
            camera.getElevation(),
            camera.getRadius(),
            stereoStatus
        );

        glfwSetWindowTitle(windowHandle, title);
    }

    /**
     * Memuat Matrix4f ke GL_PROJECTION.
     */
    private void loadProjectionMatrix(Matrix4f proj) {
        glMatrixMode(GL_PROJECTION);
        proj.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    /**
     * Memuat Matrix4f ke GL_MODELVIEW.
     */
    private void loadViewMatrix(Matrix4f view) {
        glMatrixMode(GL_MODELVIEW);
        view.get(matrixBuffer);
        glLoadMatrixf(matrixBuffer);
    }

    /**
     * Membersihkan alokasi memori dan window GLFW.
     */
    private void cleanup() {
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }
}
