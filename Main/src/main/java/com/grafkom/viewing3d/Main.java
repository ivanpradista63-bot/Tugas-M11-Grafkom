package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Aplikasi Utama: 3D Visual Realism Suite
 * Praktikum Grafika Komputer Minggu ke-11.
 *
 * Mengimplementasikan:
 * 1. Shading Models: Phong vs Blinn-Phong (Key 'B')
 * 2. Flat Shading vs Smooth Shading (Key 'F')
 * 3. Hidden Surface Removal: Z-Buffer (Key 'Z') & Back-Face Culling (Key 'C')
 * 4. Texture Mapping: UV Mapping, Nearest vs Bilinear Mipmapping (Key 'T', 'N', 'X')
 * 5. Real-Time Shadow Mapping: 2-Pass Depth FBO, Bias Tuning, PCF 3x3 (Key 'H', 'P', 'K')
 */
public class Main {
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private long windowHandle;

    private Camera3D camera;
    private PerspectiveProjection perspective;
    private InputState input;

    private ShaderProgram phongShader;
    private ShaderProgram depthShader;

    private Mesh sphereSmooth;
    private Mesh sphereFlat;
    private Mesh groundPlane;

    private Texture2D brickTexture;
    private Texture2D checkerTexture;
    private ShadowMapFBO shadowFBO;

    // Sumber Cahaya & Transformasi Shadow Map
    private final Vector3f lightPos = new Vector3f(-4.0f, 7.0f, -3.0f);
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private final Matrix4f lightProjection = new Matrix4f();
    private final Matrix4f lightView = new Matrix4f();

    // Matriks Model untuk objek
    private final Matrix4f sphereModel = new Matrix4f();
    private final Matrix4f planeModel = new Matrix4f();

    // Kontrol Mouse Navigasi Orbit
    private boolean mousePressed = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    // Perhitungan FPS Real-Time
    private double lastFpsTime = 0.0;
    private int frameCount = 0;
    private double currentFps = 0.0;

    // Rotasi Otomatis
    private float modelRotation = 0.0f;

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        printInstructions();
        initWindow();
        initOpenGL();
        initResources();
        loop();
        cleanup();
    }

    private void printInstructions() {
        System.out.println("================================================================================");
        System.out.println("   PRAKTIKUM GRAFIKA KOMPUTER MINGGU 11: 3D VISUAL REALISM SUITE               ");
        System.out.println("================================================================================");
        System.out.println("[KONTROL VISUAL REALISM]");
        System.out.println("  B     : Toggle Shading Model (Phong <-> Blinn-Phong)");
        System.out.println("  F     : Toggle Shading Type (Smooth Shading <-> Flat Shading)");
        System.out.println("  Z     : Toggle HSR Depth Test (Z-Buffer ON / OFF)");
        System.out.println("  C     : Toggle HSR Back-Face Culling (ON / OFF)");
        System.out.println("  T     : Toggle Texture Mapping (Tekstur ON / OFF)");
        System.out.println("  N     : Toggle Texture Filtering (Nearest <-> Bilinear Mipmap)");
        System.out.println("  X     : Toggle Gambar Tekstur (Batu Bata <-> Papan Catur)");
        System.out.println("  H     : Toggle Real-Time Shadow (Shadow ON / OFF)");
        System.out.println("  P     : Ganti Shadow Bias Mode (Dynamic -> Zero/Acne -> Large/PeterPan)");
        System.out.println("  K     : Toggle PCF 3x3 Soft Shadow vs Single Sample");
        System.out.println();
        System.out.println("[NAVIGASI & UTILITY]");
        System.out.println("  Klik Kiri Mouse + Drag : Orbit Kamera (Azimuth & Elevasi)");
        System.out.println("  Scroll Mouse           : Zoom Jarak Kamera");
        System.out.println("  SPASI                  : Toggle Rotasi Otomatis");
        System.out.println("  F12                    : Ambil Screenshot Layar (.PNG)");
        System.out.println("  ESC                    : Keluar dari Aplikasi");
        System.out.println("================================================================================");
    }

    private void initWindow() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Gagal menginisialisasi GLFW!");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, "3D Visual Realism Suite", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Gagal membuat window GLFW OpenGL 3.3 Core!");
        }

        input = new InputState();

        // Callback Keyboard
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
            input.onKey(key, action);
        });

        // Callback Mouse Button
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

        // Callback Gerakan Mouse
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

        // Callback Mouse Scroll (Zoom)
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            camera.zoom((float) -yoffset * 0.5f);
        });

        // Callback Resize Framebuffer
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            this.windowWidth = width;
            this.windowHeight = height;
            perspective.updateAspect(width, height);
        });

        // Posisikan window di tengah layar
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidmode != null) {
            glfwSetWindowPos(
                    windowHandle,
                    (vidmode.width() - windowWidth) / 2,
                    (vidmode.height() - windowHeight) / 2
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(0); // Matikan VSync agar pengukuran FPS murni kinerja GPU
        glfwShowWindow(windowHandle);
    }

    private void initOpenGL() {
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glClearColor(0.10f, 0.12f, 0.16f, 1.0f);
    }

    private void initResources() {
        // Inisialisasi Kamera & Proyeksi
        camera = new Camera3D(new Vector3f(0.0f, 0.2f, 0.0f), 5.5f, 45.0f, 25.0f);
        perspective = new PerspectiveProjection(45.0f, (float) windowWidth / windowHeight, 0.1f, 100.0f);

        // Kompilasi Shader
        phongShader = new ShaderProgram("shaders/phong.vert", "shaders/phong.frag");
        depthShader = new ShaderProgram("shaders/depth.vert", "shaders/depth.frag");

        // Pembuatan Mesh Geometri
        // Sphere dengan 16 stacks dan 24 sectors: ideal untuk mengamati facet flat vs smooth
        sphereSmooth = Mesh.sphere(1.2f, 16, 24, false);
        sphereFlat   = Mesh.sphere(1.2f, 16, 24, true);
        groundPlane  = Mesh.plane(12.0f, -1.2f, 8.0f);

        // Pemuatan Tekstur
        brickTexture   = new Texture2D("textures/brick.png");
        checkerTexture = new Texture2D("textures/checker.png");

        // Inisialisasi FBO Shadow Map (2048 x 2048 Depth Texture)
        shadowFBO = new ShadowMapFBO();

        // Perhitungan Matriks Orthografik untuk Sumber Cahaya Directional
        float orthoBox = 8.0f;
        lightProjection.identity().ortho(-orthoBox, orthoBox, -orthoBox, orthoBox, 1.0f, 25.0f);
        lightView.identity().lookAt(lightPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        lightProjection.mul(lightView, lightSpaceMatrix); // lightSpaceMatrix = P_light * V_light

        // Matriks Model Lantai (statis)
        planeModel.identity();

        lastFpsTime = glfwGetTime();
    }

    private void loop() {
        double lastFrameTime = glfwGetTime();

        while (!glfwWindowShouldClose(windowHandle)) {
            double currentFrameTime = glfwGetTime();
            float deltaTime = (float) (currentFrameTime - lastFrameTime);
            lastFrameTime = currentFrameTime;

            // Update auto rotasi jika aktif
            if (input.autoRotate) {
                modelRotation += 30.0f * deltaTime;
                if (modelRotation >= 360.0f) modelRotation -= 360.0f;
            }

            // Update matriks model bola
            sphereModel.identity()
                    .translate(0.0f, 0.5f, 0.0f)
                    .rotateY((float) Math.toRadians(modelRotation));

            // Tekstur aktif (Batu Bata vs Catur)
            Texture2D activeTex = input.useChecker ? checkerTexture : brickTexture;
            activeTex.setFilter(input.mipmapped);

            // ==========================================================
            // PASS 1: RENDER DEPTH DARI SUDUT PANDANG CAHAYA (SHADOW MAP)
            // ==========================================================
            if (input.shadow) {
                shadowFBO.bindForWriting();
                glEnable(GL_DEPTH_TEST);
                glDepthFunc(GL_LESS);
                glEnable(GL_CULL_FACE);
                glCullFace(GL_FRONT); // Front-face culling pada depth pass mereduksi shadow acne

                depthShader.use();
                depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

                // Render bola ke depth map
                depthShader.setMat4("model", sphereModel);
                (input.smooth ? sphereSmooth : sphereFlat).draw();

                shadowFBO.unbind(windowWidth, windowHeight);
            }

            // ==========================================================
            // PASS 2: RENDER SCENE UTAMA KE LAYAR (CAMERA VIEW)
            // ==========================================================
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Konfigurasi HSR (Z-Buffer Depth Test & Back-Face Culling)
            HSRManager.setupDepthTest(input.depthTest);
            HSRManager.setupBackFaceCulling(input.cullFace);

            phongShader.use();
            phongShader.setMat4("view", camera.getViewMatrix());
            phongShader.setMat4("projection", perspective.getProjectionMatrix());
            phongShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);

            phongShader.setVec3("lightPos", lightPos);
            phongShader.setVec3("viewPos", camera.getEye());
            phongShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);

            // Set uniform toggle indikator
            phongShader.setBool("useBlinnPhong", input.blinnPhong);
            phongShader.setBool("useTexture", input.useTexture);
            phongShader.setBool("useShadow", input.shadow);
            phongShader.setInt("shadowBiasMode", input.biasMode);
            phongShader.setBool("usePCF", input.usePCF);

            // Binding Textures: Unit 0 untuk Albedo/Diffuse, Unit 1 untuk Shadow Depth Map
            activeTex.bind(0);
            phongShader.setInt("textureSampler", 0);

            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, shadowFBO.getDepthTextureID());
            phongShader.setInt("shadowMap", 1);

            // 1. Gambar Bola (Sphere)
            phongShader.setMat4("model", sphereModel);
            phongShader.setVec3("objectColor", 0.90f, 0.40f, 0.25f);
            (input.smooth ? sphereSmooth : sphereFlat).draw();

            // 2. Gambar Ground Plane (Lantai Penerima Bayangan)
            // Digambar setelah bola agar efek Z-Buffer OFF terlihat dengan jelas!
            phongShader.setMat4("model", planeModel);
            phongShader.setVec3("objectColor", 0.60f, 0.65f, 0.70f);
            groundPlane.draw();

            // Tangkap screenshot jika diminta (F12)
            if (input.takeScreenshot) {
                input.takeScreenshot = false;
                captureScreen("screenshot_" + System.currentTimeMillis() + ".png");
            }

            // Hitung FPS & Perbarui Judul Window
            updateFpsAndTitle();

            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
    }

    private void updateFpsAndTitle() {
        frameCount++;
        double now = glfwGetTime();
        if (now - lastFpsTime >= 0.5) {
            currentFps = frameCount / (now - lastFpsTime);
            frameCount = 0;
            lastFpsTime = now;

            String title = String.format("FPS: %.0f | %s | %s | Z:%s C:%s | Tex:%s (%s) | Shadow:%s [Bias:%s, PCF:%s]",
                    currentFps,
                    input.blinnPhong ? "Blinn-Phong" : "Phong",
                    input.smooth ? "Smooth" : "Flat",
                    input.depthTest ? "ON" : "OFF",
                    input.cullFace ? "ON" : "OFF",
                    input.useTexture ? (input.useChecker ? "Checker" : "Brick") : "OFF",
                    input.mipmapped ? "Mipmap" : "Nearest",
                    input.shadow ? "ON" : "OFF",
                    input.getBiasName(),
                    input.usePCF ? "3x3" : "1x1"
            );
            glfwSetWindowTitle(windowHandle, title);
        }
    }

    /**
     * Menyimpan tampilan layar aktif ke file gambar PNG.
     */
    public void captureScreen(String fileName) {
        File dir = new File("screenshots");
        if (!dir.exists()) dir.mkdirs();

        File outFile = new File(dir, fileName);

        ByteBuffer buffer = BufferUtils.createByteBuffer(windowWidth * windowHeight * 4);
        glReadPixels(0, 0, windowWidth, windowHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        BufferedImage image = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < windowHeight; y++) {
            for (int x = 0; x < windowWidth; x++) {
                int i = (x + (windowWidth * y)) * 4;
                int r = buffer.get(i) & 0xFF;
                int g = buffer.get(i + 1) & 0xFF;
                int b = buffer.get(i + 2) & 0xFF;
                int a = buffer.get(i + 3) & 0xFF;
                // Balik orientasi sumbu Y (OpenGL bottom-to-top -> Java Image top-to-bottom)
                image.setRGB(x, windowHeight - (y + 1), (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        try {
            ImageIO.write(image, "PNG", outFile);
            System.out.println("[Screenshot] Tersimpan: " + outFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("[Screenshot] Gagal menyimpan: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (sphereSmooth != null) sphereSmooth.cleanup();
        if (sphereFlat != null) sphereFlat.cleanup();
        if (groundPlane != null) groundPlane.cleanup();
        if (phongShader != null) phongShader.cleanup();
        if (depthShader != null) depthShader.cleanup();

        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();

        GLFWErrorCallback callback = glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    // Getter untuk kebutuhan capture otomatis dan pengujian
    public InputState getInput() { return input; }
    public long getWindowHandle() { return windowHandle; }
    public double getCurrentFps() { return currentFps; }
}
