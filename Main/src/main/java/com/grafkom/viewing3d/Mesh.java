package com.grafkom.viewing3d;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Representasi Geometri 3D dengan Vertex Array Object (VAO).
 * Layout Vertex Buffer (8 float per vertex):
 * [pos.x, pos.y, pos.z, norm.x, norm.y, norm.z, tex.u, tex.v]
 * - Location 0: aPos (vec3)
 * - Location 1: aNormal (vec3)
 * - Location 2: aTexCoords (vec2)
 */
public class Mesh {
    private final int vaoID;
    private final int vboID;
    private final int eboID;
    private final int vertexCount;
    private final boolean indexed;

    public Mesh(float[] vertices, int[] indices) {
        vaoID = glGenVertexArrays();
        vboID = glGenBuffers();

        glBindVertexArray(vaoID);

        // Upload vertex data ke VBO
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        FloatBuffer vBuf = BufferUtils.createFloatBuffer(vertices.length);
        vBuf.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vBuf, GL_STATIC_DRAW);

        // Konfigurasi EBO jika indexed
        if (indices != null && indices.length > 0) {
            indexed = true;
            vertexCount = indices.length;
            eboID = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
            IntBuffer iBuf = BufferUtils.createIntBuffer(indices.length);
            iBuf.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuf, GL_STATIC_DRAW);
        } else {
            indexed = false;
            vertexCount = vertices.length / 8;
            eboID = 0;
        }

        int stride = 8 * Float.BYTES; // 32 bytes

        // Attribute 0: Position (vec3)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);

        // Attribute 1: Normal (vec3)
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);

        // Attribute 2: TexCoords (vec2)
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);

        glBindVertexArray(0);
    }

    public void draw() {
        glBindVertexArray(vaoID);
        if (indexed) {
            glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        } else {
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteVertexArrays(vaoID);
        glDeleteBuffers(vboID);
        if (eboID != 0) {
            glDeleteBuffers(eboID);
        }
    }

    // ==========================================================
    // FACTORY METHODS
    // ==========================================================

    /**
     * Membuat mesh bola (Sphere) dengan opsi Smooth Shading (per-vertex normal)
     * atau Flat Shading (per-face normal).
     */
    public static Mesh sphere(float r, int stacks, int sectors, boolean flat) {
        List<Float> v = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();

        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks; // 0 .. PI (kutub atas -> kutub bawah)
            for (int j = 0; j <= sectors; j++) {
                double th = 2 * Math.PI * j / sectors; // 0 .. 2PI
                float x = (float) (Math.sin(phi) * Math.cos(th));
                float y = (float) Math.cos(phi);
                float z = (float) (Math.sin(phi) * Math.sin(th));

                // 1. Position (posisi di ruang lokal)
                v.add(r * x);
                v.add(r * y);
                v.add(r * z);

                // 2. Normal (pada bola, normal smooth = posisi ternormalisasi)
                v.add(x);
                v.add(y);
                v.add(z);

                // 3. UV Coordinates (u mengelilingi bola, v dari atas ke bawah)
                v.add((float) j / sectors);
                v.add(1.0f - (float) i / stacks);
            }
        }

        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < sectors; j++) {
                int a = i * (sectors + 1) + j;
                int b = a + sectors + 1;

                // Segitiga 1 (CCW dilihat dari luar objek)
                idx.add(a);
                idx.add(a + 1);
                idx.add(b);

                // Segitiga 2 (CCW)
                idx.add(a + 1);
                idx.add(b + 1);
                idx.add(b);
            }
        }

        float[] vertices = toArray(v);
        int[] indices = idx.stream().mapToInt(Integer::intValue).toArray();

        return flat ? new Mesh(toFlat(vertices, indices), null)
                    : new Mesh(vertices, indices);
    }

    /**
     * Mengonversi mesh terindeks menjadi flat shading mesh non-terindeks,
     * di mana setiap segitiga memiliki vektor normal face tersendiri.
     */
    private static float[] toFlat(float[] vtx, int[] idx) {
        float[] out = new float[idx.length * 8];
        for (int t = 0; t < idx.length; t += 3) {
            Vector3f p0 = pos(vtx, idx[t]);
            Vector3f p1 = pos(vtx, idx[t + 1]);
            Vector3f p2 = pos(vtx, idx[t + 2]);

            // Normal face dihitung dari cross product dua sisi segitiga
            Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(p2).sub(p0));
            if (n.lengthSquared() < 1e-8f) { // Tangani segitiga degenerate di kutub bola
                n.set(p0).add(p1).add(p2);
            }
            n.normalize();

            // Salin 3 vertex untuk segitiga ini dengan face normal yang identik
            for (int k = 0; k < 3; k++) {
                int i = idx[t + k] * 8;
                int o = (t + k) * 8;
                out[o]     = vtx[i];     // pos x
                out[o + 1] = vtx[i + 1]; // pos y
                out[o + 2] = vtx[i + 2]; // pos z
                out[o + 3] = n.x;        // face normal x
                out[o + 4] = n.y;        // face normal y
                out[o + 5] = n.z;        // face normal z
                out[o + 6] = vtx[i + 6]; // uv u
                out[o + 7] = vtx[i + 7]; // uv v
            }
        }
        return out;
    }

    private static Vector3f pos(float[] v, int idx) {
        int i = idx * 8;
        return new Vector3f(v[i], v[i + 1], v[i + 2]);
    }

    private static float[] toArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    /**
     * Membuat ground plane (lantai) horisontal penerima bayangan.
     * @param s setengah panjang sisi plane
     * @param y elevasi vertikal bidang lantai
     * @param t faktor pengulangan koordinat tekstur (UV tiling)
     */
    public static Mesh plane(float s, float y, float t) {
        float[] planeVertices = {
            // Pos (3)           // Normal (3)    // UV (2)
            -s, y, -s,           0.0f, 1.0f, 0.0f,  0.0f, 0.0f,
            -s, y,  s,           0.0f, 1.0f, 0.0f,  0.0f, t,
             s, y,  s,           0.0f, 1.0f, 0.0f,  t,    t,
             s, y, -s,           0.0f, 1.0f, 0.0f,  t,    0.0f
        };
        int[] planeIndices = {
            0, 1, 2,
            0, 2, 3
        };
        return new Mesh(planeVertices, planeIndices);
    }
}
