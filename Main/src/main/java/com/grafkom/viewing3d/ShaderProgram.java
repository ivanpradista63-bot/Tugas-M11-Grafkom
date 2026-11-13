package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

/**
 * Kelas utilitas untuk mengompilasi, me-link, dan mengelola GLSL Shader Program.
 */
public class ShaderProgram {
    private final int programID;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();
    private final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(String vertPath, String fragPath) {
        String vertSource = loadShaderSource(vertPath);
        String fragSource = loadShaderSource(fragPath);

        int vertShader = compileShader(GL_VERTEX_SHADER, vertSource, vertPath);
        int fragShader = compileShader(GL_FRAGMENT_SHADER, fragSource, fragPath);

        programID = glCreateProgram();
        glAttachShader(programID, vertShader);
        glAttachShader(programID, fragShader);
        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programID);
            throw new RuntimeException("Gagal me-link shader program: " + log);
        }

        glDeleteShader(vertShader);
        glDeleteShader(fragShader);
    }

    private int compileShader(int type, String source, String name) {
        int shaderID = glCreateShader(type);
        glShaderSource(shaderID, source);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderID);
            throw new RuntimeException("Gagal mengompilasi shader [" + name + "]: " + log);
        }
        return shaderID;
    }

    private String loadShaderSource(String path) {
        // Coba baca dari file sistem langsung
        File file = new File(path);
        if (!file.exists()) {
            File alt = new File("src/main/resources/" + path);
            if (alt.exists()) file = alt;
            else {
                File alt2 = new File("Grafikom M10/" + path);
                if (alt2.exists()) file = alt2;
            }
        }

        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            } catch (Exception e) {
                System.err.println("Gagal membaca file shader: " + path + ": " + e.getMessage());
            }
        }

        // Coba baca dari ClassLoader / Resource
        String resPath = path.startsWith("/") ? path : "/" + path;
        try (InputStream in = getClass().getResourceAsStream(resPath)) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("Gagal membaca resource shader: " + resPath + ": " + e.getMessage());
        }

        throw new RuntimeException("File shader tidak ditemukan: " + path);
    }

    public void use() {
        glUseProgram(programID);
    }

    public void stop() {
        glUseProgram(0);
    }

    public int getUniformLocation(String name) {
        if (uniformLocationCache.containsKey(name)) {
            return uniformLocationCache.get(name);
        }
        int location = glGetUniformLocation(programID, name);
        uniformLocationCache.put(name, location);
        return location;
    }

    public void setMat4(String name, Matrix4f matrix) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            matrix.get(matrixBuffer);
            glUniformMatrix4fv(loc, false, matrixBuffer);
        }
    }

    public void setVec3(String name, Vector3f vec) {
        setVec3(name, vec.x, vec.y, vec.z);
    }

    public void setVec3(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform3f(loc, x, y, z);
        }
    }

    public void setFloat(String name, float value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform1f(loc, value);
        }
    }

    public void setInt(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc != -1) {
            glUniform1i(loc, value);
        }
    }

    public void setBool(String name, boolean value) {
        setInt(name, value ? 1 : 0);
    }

    public void cleanup() {
        stop();
        if (programID != 0) {
            glDeleteProgram(programID);
        }
    }

    public int getProgramID() {
        return programID;
    }
}
