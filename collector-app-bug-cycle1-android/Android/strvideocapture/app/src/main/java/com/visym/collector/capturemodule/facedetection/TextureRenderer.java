package com.visym.collector.capturemodule.facedetection;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class TextureRenderer {
    private final String vertexShaderCode =
            "precision highp float;\n" +
                    "attribute vec3 vertexPosition;\n" +
                    "attribute vec2 uvs;\n" +
                    "varying vec2 varUvs;\n" +
                    "uniform mat4 mvp;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "\tvarUvs = uvs;\n" +
                    "\tgl_Position = mvp * vec4(vertexPosition, 1.0);\n" +
                    "}";

    private final String fragmentShaderCode =
            "precision mediump float;\n" +
                    "\n" +
                    "varying vec2 varUvs;\n" +
                    "uniform sampler2D texSampler;\n" +
                    "\n" +
                    "void main()\n" +
                    "{\t\n" +
                    "\tgl_FragColor = texture2D(texSampler, varUvs);\n" +
                    "}";

    private float[] vertices = new float[]{-1.0F, -1.0F, 0.0F, 0.0F, 0.0F, -1.0F, 1.0F, 0.0F,
            0.0F, 1.0F, 1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, -1.0F, 0.0F, 1.0F, 0.0F};
    private int[] indices = new int[]{2, 1, 0, 0, 3, 2};

    private int program;
    private int vertexHandle;
    private int[] bufferHandles = new int[2];
    private int uvsHandle;
    private int mvpHandle;
    private final int[] textureHandle = new int[1];
    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;

    public TextureRenderer() {
        vertexBuffer = ByteBuffer.allocateDirect(
                vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        indexBuffer = ByteBuffer.allocateDirect(
                indices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        indexBuffer.put(indices).position(0);

        initGl();
    }

    private void initGl() {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        program = GLES20.glCreateProgram();
        if (program != 0){
            GLES20.glAttachShader(program, vertexShader);
            GLES20.glAttachShader(program, fragmentShader);
            GLES20.glLinkProgram(program);

            vertexHandle = GLES20.glGetAttribLocation(program, "vertexPosition");
            uvsHandle = GLES20.glGetAttribLocation(program, "uvs");
            mvpHandle = GLES20.glGetUniformLocation(program, "mvp");
        }

        // Initialize buffers
        GLES20.glGenBuffers(2, bufferHandles, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandles[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.length * 4,
                vertexBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferHandles[1]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.length * 4,
                indexBuffer, GLES20.GL_DYNAMIC_DRAW);

        // Init texture handle
        GLES20.glGenTextures(1, textureHandle, 0);

        // Ensure I can draw transparent stuff that overlaps properly
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader != 0) {
            GLES20.glShaderSource(shader, source);
            GLES20.glCompileShader(shader);
        }
        return shader;
    }

    public void renderBitmap(int width, int height, Bitmap bitmap, float[] mvpMatrix){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glClearColor(0f, 0f, 0f, 0f);

        GLES20.glViewport(0, 0, width, height);

        GLES20.glUseProgram(program);

        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0);

        // Prepare texture for drawing
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Prepare buffers with vertices and indices & draw
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferHandles[0]);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferHandles[1]);

        GLES20.glEnableVertexAttribArray(vertexHandle);
        GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT, false,
                4 * 5, 0);

        GLES20.glEnableVertexAttribArray(uvsHandle);
        GLES20.glVertexAttribPointer(uvsHandle, 2, GLES20.GL_FLOAT,
                false, 4 * 5, 3 * 4);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_INT, 0);
    }
}
