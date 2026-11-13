# 3D Visual Realism Suite (Praktikum Grafika Komputer - Minggu ke-11)



## 1. Ringkasan Proyek

Proyek ini merupakan pengembangan dan perluasan komprehensif dari aplikasi *3D Viewer* (Minggu ke-10) menjadi **3D Visual Realism Suite**, yang mengimplementasikan 5 pilar utama rendering permukaan (*rendering faces*) untuk mencapai realisme visual (*visual realism*) berbasis Modern OpenGL Pipeline (Shader-based GLSL 330 core) dan arsitektur Object-Oriented Programming (OOP) modular pada Java.

### Matriks Pemenuhan 5 Indikator Penilaian (Target Skor 100 — "Sangat Baik")

| # | Indikator | Kelas / Shader Utama | Kontrol | Implementasi Teknis & Pembuktian Matematis |
|---|---|---|---|---|
| **a** | **Prasyarat Mesh** | `Mesh.java` | — | Layout vertex terpadu `[pos(3) + normal(3) + uv(2)]` = 8 float per vertex (stride 32 byte) pada VBO & VAO. Mendukung bentuk Sphere dan Ground Plane. |
| **1** | **Shading Models (20%)** | `phong.frag` | **B** | Implementasi ganda **Phong** ($I_{spec} = k_s I_s (\mathbf{R} \cdot \mathbf{V})^n$) dan **Blinn-Phong** ($I_{spec} = k_s I_s (\mathbf{N} \cdot \mathbf{H})^n$) dengan Halfway Vector $\mathbf{H} = \frac{\mathbf{L}+\mathbf{V}}{\|\mathbf{L}+\mathbf{V}\|}$. Switch real-time tanpa reload shader. |
| **2** | **Flat vs Smooth (20%)** | `Mesh.java`, `phong.vert` | **F** | **Smooth Shading**: Normal per-vertex hasil interpolasi; **Flat Shading**: Normal per-face segitiga mandiri ($\mathbf{N}_f = \frac{(\mathbf{p}_1-\mathbf{p}_0) \times (\mathbf{p}_2-\mathbf{p}_0)}{\|\dots\|}$). Menghilangkan efek Mach Banding pada mode smooth. |
| **3** | **Hidden Surface Removal (20%)** | `HSRManager.java` | **Z**, **C** | Konfigurasi **Z-Buffer Depth Test** (`glEnable(GL_DEPTH_TEST)`, `glDepthFunc(GL_LESS)`) dan **Back-Face Culling** (`glCullFace(GL_BACK)`, `glFrontFace(GL_CCW)`). Mitigasi Z-fighting dengan rasio near/far presisi (0.1 / 100.0). |
| **4** | **Texture Mapping (20%)** | `Texture2D.java`, `phong.frag` | **T**, **N**, **X** | Pemuatan tekstur PNG menggunakan LWJGL STBImage, koordinat UV perspektif-korektif. Pilihan filter real-time: **Nearest Neighbor** (aliasing/moiré terlihat) vs **Bilinear + Trilinear Mipmapping** (`GL_LINEAR_MIPMAP_LINEAR`). Tekstur bata dan papan catur. |
| **5** | **Shadow Mapping 2-Pass (20%)** | `ShadowMapFBO.java`, `depth.*`, `phong.*` | **H**, **P**, **K** | **Pass 1**: Render depth map 2048×2048 dari sudut pandang cahaya ortografis ke FBO. **Pass 2**: Proyeksi depth map ke fragmen scene dengan transformasi light-space, **Dynamic Slope-Scale Bias** $\text{bias} = \max(0.05 \cdot (1 - \mathbf{N}\cdot\mathbf{L}), 0.005)$ untuk eliminasi Shadow Acne & Peter Panning, serta filter **PCF 3×3** untuk soft shadows. |

---

## 2. Struktur Direktori Proyek

```
Grafikom M10/
├── pom.xml
├── README.md
├── build.bat                  <- Skrip kompilasi otomatis
├── run.bat                    <- Skrip eksekusi aplikasi utama
├── run_benchmark.bat          <- Skrip benchmark & generator screenshot
├── screenshots/               <- Folder 15 tangkapan layar pengujian resmi & data FPS
│   ├── 1a_Phong.png
│   ├── 1b_BlinnPhong.png
│   ├── 2a_FlatShading.png
│   ├── 2b_SmoothShading.png
│   ├── 3a_DepthTest_ON.png
│   ├── 3b_DepthTest_OFF.png
│   ├── 3c_CullFace_ON.png
│   ├── 3d_CullFace_OFF.png
│   ├── 4a_Texture_Nearest.png
│   ├── 4b_Texture_Mipmap.png
│   ├── 5a_Shadow_ON.png
│   ├── 5b_Shadow_OFF.png
│   ├── 5c_Shadow_Acne_Bias0.png
│   ├── 5d_Shadow_PeterPanning.png
│   ├── 5e_Shadow_PCF_vs_Hard.png
│   └── fps_benchmark.txt
└── src/
    └── main/
        ├── java/com/grafkom/viewing3d/
        │   ├── Main.java              <- Window, loop, 2-pass rendering pipeline
        │   ├── AutoTestRunner.java    <- Automated test suite & FPS benchmark runner
        │   ├── InputState.java        <- State management & GLFW key callbacks
        │   ├── Mesh.java              <- VAO/VBO/EBO, sphere smooth/flat & ground plane
        │   ├── HSRManager.java        <- Z-Buffer depth test & back-face culling
        │   ├── Texture2D.java         <- STBImage texture loader & filter switcher
        │   ├── ShadowMapFBO.java      <- Framebuffer Object & 2048x2048 depth texture
        │   ├── ShaderProgram.java     <- GLSL compilation, linking & uniform setters
        │   ├── Camera3D.java          <- Orbital LookAt camera
        │   └── PerspectiveProjection.java <- Symmetric perspective frustum
        └── resources/
            ├── shaders/
            │   ├── phong.vert         <- Vertex shader utama (normal matrix, light space)
            │   ├── phong.frag         <- Fragment shader (Phong/Blinn, shadows, texture)
            │   ├── depth.vert         <- Pass 1 vertex shader
            │   └── depth.frag         <- Pass 1 fragment shader (empty, depth-only)
            └── textures/
                ├── brick.png          <- Tekstur batu bata terakota (512x512)
                └── checker.png        <- Tekstur papan catur (512x512 untuk tes moiré)
```

---

## 3. Panduan Kontrol & Navigasi Interaktif

| Tombol | Fungsi Interaktif |
|---|---|
| **B** | Beralih model pencahayaan: **Phong** $\longleftrightarrow$ **Blinn-Phong** |
| **F** | Beralih tipe shading: **Smooth Shading** $\longleftrightarrow$ **Flat Shading** |
| **Z** | Beralih Z-Buffer: **Depth Test ON** $\longleftrightarrow$ **Depth Test OFF** |
| **C** | Beralih culling: **Back-Face Culling ON** $\longleftrightarrow$ **Back-Face Culling OFF** |
| **T** | Beralih tekstur: **Texture Mapping ON** $\longleftrightarrow$ **Solid Color Mode** |
| **N** | Beralih filter tekstur: **Nearest Neighbor** $\longleftrightarrow$ **Bilinear + Mipmap** |
| **X** | Beralih file tekstur: **Tekstur Batu Bata** $\longleftrightarrow$ **Tekstur Papan Catur** |
| **H** | Beralih bayangan: **Real-Time Shadow Mapping ON** $\longleftrightarrow$ **OFF** |
| **P** | Siklus Bias Bayangan: **Dynamic Bias** $\to$ **Zero Bias (Shadow Acne)** $\to$ **Large Bias (Peter Panning)** |
| **K** | Beralih kehalusan bayangan: **PCF 3×3 Soft Shadow** $\longleftrightarrow$ **Hard Shadow 1×1** |
| **SPASI** | Nyalakan / Matikan rotasi otomatis model bola *(Turntable)* |
| **F12** | Ambil tangkapan layar instan (*Screenshot*) berformat PNG ke folder `/screenshots` |
| **Drag Mouse Kiri** | Orbit kamera (mengubah Azimuth dan Elevasi secara bebas) |
| **Scroll Mouse** | Zoom in / Zoom out jarak pandang kamera |
| **ESC** | Keluar dari aplikasi |

---

## 4. Cara Menjalankan & Menguji Proyek

### Opsi A: Menggunakan Skrip Batch Cepat (Disarankan)
1. **Kompilasi Proyek:**
   ```cmd
   build.bat
   ```
2. **Jalankan Aplikasi Interaktif:**
   ```cmd
   run.bat
   ```
3. **Jalankan Pengujian Otomatis & Benchmark FPS:**
   ```cmd
   run_benchmark.bat
   ```

### Opsi B: Menggunakan Apache Maven Standar
```bash
mvn clean compile exec:java
```

---

## 5. Hasil Pengukuran Kinerja Rendering (FPS Benchmark)

Pengujian dilakukan pada resolusi viewport 1280×720 dengan V-Sync dinonaktifkan (`glfwSwapInterval(0)`):

| Konfigurasi Pengujian | Rata-rata FPS | Karakteristik Beban Komputasi |
|---|---|---|
| **Blinn-Phong Shading Model** | **19.044,0 FPS** | Komputasi specular via Halfway Vector $\mathbf{H}$ sangat ringan di GPU. |
| **Phong Shading Model** | **4.825,4 FPS** | Evaluasi vektor pantulan $\mathbf{R} = 2(\mathbf{N}\cdot\mathbf{L})\mathbf{N}-\mathbf{L}$ membutuhkan operasi aritmatika per-fragment lebih berat. |
| **Smooth Shading (Per-Vertex Normal)** | **18.279,3 FPS** | Menggunakan indexed EBO rendering yang sangat ramah terhadap Vertex Post-Transform Cache. |
| **Flat Shading (Per-Face Normal)** | **16.982,7 FPS** | Menggunakan non-indexed array dengan duplikasi vertex per-face (throughput vertex lebih banyak). |
| **Back-Face Culling ON** | **26.384,1 FPS** | **Paling Cepat!** Mengeliminasi 50% segitiga sebelum tahap fragment shading dieksekusi. |
| **Back-Face Culling OFF** | **18.867,3 FPS** | Penurunan performa karena GPU harus memproses seluruh poligon tampak belakang. |
| **Z-Buffer Depth Test ON** | **14.657,0 FPS** | Early-Z test memastikan urutan rendering kedalaman pixel tertangani akurat. |
| **Z-Buffer Depth Test OFF** | **19.217,8 FPS** | Cepat namun menghasilkan artifak visual parah (lantai menimpa bola). |
| **Texture Nearest Neighbor** | **19.757,6 FPS** | Hanya membaca 1 texel tunggal per fragment, namun menimbulkan aliasing/moiré. |
| **Texture Bilinear + Mipmap** | **9.332,9 FPS** | Trilinear filtering membaca 8 texel antar 2 mip-level (kualitas visual sempurna tanpa aliasing). |
| **Shadow Mapping ON (Dynamic + PCF 3×3)** | **11.220,0 FPS** | Dua pass render + 9 kali texture lookup sampler2D per fragment menghasilkan bayangan halus. |
| **Shadow Mapping OFF** | **19.913,7 FPS** | Tanpa Pass 1 FBO dan tanpa kalkulasi shadow factor di fragment shader. |
| **Hard Shadow (Single Sample 1×1)** | **16.992,4 FPS** | Jauh lebih cepat dari PCF karena hanya 1 texture lookup per fragment. |
| **Shadow Bias = 0 (Shadow Acne)** | **17.383,2 FPS** | Muncul pola moiré gelap/bercak hitam pada permukaan objek karena self-shadowing numerik. |
| **Shadow Bias Terlalu Besar (Peter Panning)** | **10.146,8 FPS** | Bayangan terlepas (*detached*) dari pangkal kontak dasar objek. |
