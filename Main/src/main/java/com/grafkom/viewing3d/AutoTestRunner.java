package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Otomasi Pengujian & Pengambilan Screenshot Resmi untuk Laporan Praktikum M11.
 * Menguji seluruh indikator rubrik dan mencatat data FPS secara real.
 */
public class AutoTestRunner {
    private final int windowWidth = 1280;
    private final int windowHeight = 720;
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

    private final Vector3f lightPos = new Vector3f(-4.0f, 7.0f, -3.0f);
    private final Matrix4f lightSpaceMatrix = new Matrix4f();
    private final Matrix4f lightProjection = new Matrix4f();
    private final Matrix4f lightView = new Matrix4f();

    private final Matrix4f sphereModel = new Matrix4f();
    private final Matrix4f planeModel = new Matrix4f();

    public static void main(String[] args) {
        new AutoTestRunner().runTests();
    }

    public void runTests() {
        System.out.println("================================================================");
        System.out.println("   MEMULAI OTOMASI PENGUJIAN & BENCHMARK FPS VISUAL REALISM     ");
        System.out.println("================================================================");

        init();

        Map<String, Double> fpsResults = new LinkedHashMap<>();

        // Konfigurasi pengujian sesuai Bagian 5 Panduan Tugas
        TestScenario[] scenarios = {
            // Indikator 1: Shading Models
            new TestScenario("1a_Phong", "Phong Shading Model", s -> {
                s.blinnPhong = false; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),
            new TestScenario("1b_BlinnPhong", "Blinn-Phong Shading Model", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),

            // Indikator 2: Flat vs Smooth Shading
            new TestScenario("2a_FlatShading", "Flat Shading (Per-Face Normal)", s -> {
                s.blinnPhong = true; s.smooth = false; s.depthTest = true; s.cullFace = true;
                s.useTexture = false; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),
            new TestScenario("2b_SmoothShading", "Smooth Shading (Per-Vertex Normal)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = false; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),

            // Indikator 3: Hidden Surface Removal (Z-Buffer & Culling)
            new TestScenario("3a_DepthTest_ON", "Z-Buffer Depth Test ON", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),
            new TestScenario("3b_DepthTest_OFF", "Z-Buffer Depth Test OFF (Overlap Artifact)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = false; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),
            new TestScenario("3c_CullFace_ON", "Back-Face Culling ON", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),
            new TestScenario("3d_CullFace_OFF", "Back-Face Culling OFF", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = false;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = false;
            }),

            // Indikator 4: Texture Mapping & Filtering
            new TestScenario("4a_Texture_Nearest", "Texture Filtering Nearest (Aliasing/Moiré)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = false; s.shadow = true; s.biasMode = 0; s.useChecker = true;
            }),
            new TestScenario("4b_Texture_Mipmap", "Texture Filtering Bilinear+Mipmap (Smooth Anti-aliased)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.useChecker = true;
            }),

            // Indikator 5: Shadows of Objects & Bias Tuning
            new TestScenario("5a_Shadow_ON", "Real-Time Shadow Mapping ON (Dynamic Bias + PCF)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.usePCF = true; s.useChecker = false;
            }),
            new TestScenario("5b_Shadow_OFF", "Shadow Mapping OFF", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = false; s.biasMode = 0; s.usePCF = true; s.useChecker = false;
            }),
            new TestScenario("5c_Shadow_Acne_Bias0", "Shadow Bias = 0 (Shadow Acne Artifact)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = false; s.mipmapped = true; s.shadow = true; s.biasMode = 1; s.usePCF = false; s.useChecker = false;
            }),
            new TestScenario("5d_Shadow_PeterPanning", "Shadow Bias Terlalu Besar (Peter Panning Artifact)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = false; s.mipmapped = true; s.shadow = true; s.biasMode = 2; s.usePCF = false; s.useChecker = false;
            }),
            new TestScenario("5e_Shadow_PCF_vs_Hard", "Hard Shadow (Tanpa PCF 1x1)", s -> {
                s.blinnPhong = true; s.smooth = true; s.depthTest = true; s.cullFace = true;
                s.useTexture = true; s.mipmapped = true; s.shadow = true; s.biasMode = 0; s.usePCF = false; s.useChecker = false;
            })
        };

        File shotDir = new File("screenshots");
        if (!shotDir.exists()) shotDir.mkdirs();

        for (TestScenario sc : scenarios) {
            sc.apply(input);
            double fps = benchmarkScenario(sc.name, sc.description);
            fpsResults.put(sc.description, fps);
            captureScreen(sc.name + ".png", sc.description, fps);
        }

        // Tulis rangkuman hasil FPS ke file dan konsol
        printSummary(fpsResults);

        cleanup();
        System.out.println("================================================================");
        System.out.println("   SELURUH PENGUJIAN SELESAI! SCREENSHOT TERSIMPAN DI /screenshots");
        System.out.println("================================================================");
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal init GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        windowHandle = glfwCreateWindow(windowWidth, windowHeight, "M11 Benchmark Runner", NULL, NULL);
        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(0); // VSync OFF untuk akurasi FPS
        GL.createCapabilities();

        camera = new Camera3D(new Vector3f(0.0f, 0.2f, 0.0f), 5.5f, 45.0f, 25.0f);
        perspective = new PerspectiveProjection(45.0f, (float) windowWidth / windowHeight, 0.1f, 100.0f);
        input = new InputState();

        phongShader = new ShaderProgram("shaders/phong.vert", "shaders/phong.frag");
        depthShader = new ShaderProgram("shaders/depth.vert", "shaders/depth.frag");

        sphereSmooth = Mesh.sphere(1.2f, 16, 24, false);
        sphereFlat   = Mesh.sphere(1.2f, 16, 24, true);
        groundPlane  = Mesh.plane(12.0f, -1.2f, 8.0f);

        brickTexture   = new Texture2D("textures/brick.png");
        checkerTexture = new Texture2D("textures/checker.png");
        shadowFBO      = new ShadowMapFBO();

        float orthoBox = 8.0f;
        lightProjection.identity().ortho(-orthoBox, orthoBox, -orthoBox, orthoBox, 1.0f, 25.0f);
        lightView.identity().lookAt(lightPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        lightProjection.mul(lightView, lightSpaceMatrix);

        sphereModel.identity().translate(0.0f, 0.5f, 0.0f);
        planeModel.identity();
    }

    private double benchmarkScenario(String id, String desc) {
        // Warm-up 20 frame
        for (int i = 0; i < 20; i++) {
            renderFrame();
            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }

        // Ukur 60 frame
        int frames = 60;
        double start = glfwGetTime();
        for (int i = 0; i < frames; i++) {
            renderFrame();
            glfwSwapBuffers(windowHandle);
            glfwPollEvents();
        }
        double elapsed = glfwGetTime() - start;
        double fps = frames / elapsed;
        System.out.printf("[BENCHMARK] %-25s -> FPS: %6.1f (%s)\n", id, fps, desc);
        return fps;
    }

    private void renderFrame() {
        Texture2D activeTex = input.useChecker ? checkerTexture : brickTexture;
        activeTex.setFilter(input.mipmapped);

        // PASS 1: Shadow Depth
        if (input.shadow) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_FRONT);

            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
            depthShader.setMat4("model", sphereModel);
            (input.smooth ? sphereSmooth : sphereFlat).draw();

            shadowFBO.unbind(windowWidth, windowHeight);
        }

        // PASS 2: Main Camera
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        HSRManager.setupDepthTest(input.depthTest);
        HSRManager.setupBackFaceCulling(input.cullFace);

        phongShader.use();
        phongShader.setMat4("view", camera.getViewMatrix());
        phongShader.setMat4("projection", perspective.getProjectionMatrix());
        phongShader.setMat4("lightSpaceMatrix", lightSpaceMatrix);
        phongShader.setVec3("lightPos", lightPos);
        phongShader.setVec3("viewPos", camera.getEye());
        phongShader.setVec3("lightColor", 1.0f, 1.0f, 1.0f);

        phongShader.setBool("useBlinnPhong", input.blinnPhong);
        phongShader.setBool("useTexture", input.useTexture);
        phongShader.setBool("useShadow", input.shadow);
        phongShader.setInt("shadowBiasMode", input.biasMode);
        phongShader.setBool("usePCF", input.usePCF);

        activeTex.bind(0);
        phongShader.setInt("textureSampler", 0);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowFBO.getDepthTextureID());
        phongShader.setInt("shadowMap", 1);

        // 1. Bola
        phongShader.setMat4("model", sphereModel);
        phongShader.setVec3("objectColor", 0.90f, 0.40f, 0.25f);
        (input.smooth ? sphereSmooth : sphereFlat).draw();

        // 2. Lantai
        phongShader.setMat4("model", planeModel);
        phongShader.setVec3("objectColor", 0.60f, 0.65f, 0.70f);
        groundPlane.draw();
    }

    private void captureScreen(String fileName, String desc, double fps) {
        File outFile = new File("screenshots", fileName);

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
                image.setRGB(x, windowHeight - (y + 1), (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        // Gambar banner teks judul dan FPS di bagian atas screenshot (seperti window title bar)
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setColor(new java.awt.Color(15, 20, 28, 220));
        g2.fillRect(0, 0, windowWidth, 42);
        g2.setColor(java.awt.Color.WHITE);
        g2.setFont(new java.awt.Font("Consolas", java.awt.Font.BOLD, 16));
        String title = String.format("FPS: %.0f | %s | %s | Z:%s C:%s | Tex:%s (%s) | Shadow:%s [Bias:%s, PCF:%s] -- [%s]",
                fps,
                input.blinnPhong ? "Blinn-Phong" : "Phong",
                input.smooth ? "Smooth" : "Flat",
                input.depthTest ? "ON" : "OFF",
                input.cullFace ? "ON" : "OFF",
                input.useTexture ? (input.useChecker ? "Checker" : "Brick") : "OFF",
                input.mipmapped ? "Mipmap" : "Nearest",
                input.shadow ? "ON" : "OFF",
                input.getBiasName(),
                input.usePCF ? "3x3" : "1x1",
                desc
        );
        g2.drawString(title, 14, 26);
        g2.dispose();

        try {
            ImageIO.write(image, "PNG", outFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printSummary(Map<String, Double> results) {
        File txtFile = new File("screenshots/fps_benchmark.txt");
        try (PrintWriter pw = new PrintWriter(new FileWriter(txtFile))) {
            pw.println("=========================================================================");
            pw.println("             HASIL PENGUKURAN KINERJA RENDERING (FPS BENCHMARK)          ");
            pw.println("                 3D VISUAL REALISM SUITE - MINGGU 11                    ");
            pw.println("=========================================================================");
            pw.printf("%-55s | %-12s\n", "Konfigurasi Pengujian", "Rata-rata FPS");
            pw.println("-------------------------------------------------------------------------");

            System.out.println("\n" + "=".repeat(75));
            System.out.println("             HASIL PENGUKURAN KINERJA RENDERING (FPS BENCHMARK)          ");
            System.out.println("=".repeat(75));
            System.out.printf("%-55s | %-12s\n", "Konfigurasi Pengujian", "Rata-rata FPS");
            System.out.println("-".repeat(75));

            for (Map.Entry<String, Double> e : results.entrySet()) {
                String line = String.format("%-55s | %8.1f FPS", e.getKey(), e.getValue());
                pw.println(line);
                System.out.println(line);
            }
            pw.println("=========================================================================");
            System.out.println("=".repeat(75));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        sphereSmooth.cleanup();
        sphereFlat.cleanup();
        groundPlane.cleanup();
        phongShader.cleanup();
        depthShader.cleanup();

        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
    }

    @FunctionalInterface
    interface ScenarioConfig {
        void configure(InputState s);
    }

    static class TestScenario {
        String name;
        String description;
        ScenarioConfig config;

        TestScenario(String name, String description, ScenarioConfig config) {
            this.name = name;
            this.description = description;
            this.config = config;
        }

        void apply(InputState s) {
            config.configure(s);
        }
    }
}
