package com.example.gg2021_hw2_2015104163

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Matrix4f
import android.util.Log
import java.lang.Exception
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.charset.Charset
import java.util.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt
import kotlin.math.tan

class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = MyGLSurfaceView(this)

        setContentView(glView)
    }
}


class MyGLSurfaceView(context: Context): GLSurfaceView(context){
    private val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)
        setRenderer(renderer)
    }
}


class MyGLRenderer(context: Context): GLSurfaceView.Renderer{
    private val mContext: Context = context

    private val vertexShaderStream = context.resources.openRawResource(R.raw.vertex)
    private val vertexShaderCode = vertexShaderStream.readBytes().toString(Charset.defaultCharset())

    private val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment)
    private val fragmentShaderCode = fragmentShaderStream.readBytes().toString(Charset.defaultCharset())

    private var mProgram: Int = 0

    private var vPMatrix = FloatArray(16)
    private var projectionMatrix = FloatArray(16)
    private var viewMatrix = FloatArray(16)
    private var teapotModelMatrix = Matrix4f(floatArrayOf(1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f))
    private val fov = Math.toRadians(60.0)

    private var eyePos = floatArrayOf(1.5f, 3.5f, 5f)
    private lateinit var teapot: Obj
    private lateinit var teapotMaterial: Material
    private val lightL: Light = Light(direction = 'L')
    private val lightR: Light = Light(direction = 'R')

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        mProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        //-------------------------------------------------------
        // Problem 2
        // Put a texture on the teapot.

        // % Note
        //   The number parameter of the Texture object must be incremented by 1 from 0
        //   for each texture construction.\
        // Also, change the teapot Obj construction code below to get the texture parameters.

        val teapotTexture = GLES20.glGetAttribLocation(mProgram, "texCoord")
        val dissolveTexture = GLES20.glGetAttribLocation(mProgram, "v_texCoord")
        // teapotMaterial =


        teapot = Obj(mContext, "teapot.obj", mProgram)

        //-------------------------------------------------------

        lightL.program = mProgram
        lightR.program = mProgram
        lightL.position = floatArrayOf(-5f, 0f, 5f)
        lightL.diffuse = floatArrayOf(0.6f, 0.1f, 0.2f)
        lightR.position = floatArrayOf(5f, 2f, 5f)
        lightR.diffuse = floatArrayOf(0.2f, 0.7f, 0.9f)
    }

    var alpha = 1f;
    var colorA = floatArrayOf(1f,1f,1f,alpha);

    override fun onDrawFrame(p0: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        viewMatrix = mySetLookAtM(eyePos[0], eyePos[1], eyePos[2], 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(mProgram)

        val eyePosHandle = GLES20.glGetUniformLocation(mProgram, "eyePos")
        GLES20.glUniform3fv(eyePosHandle, 1, eyePos, 0)

        lightL.update()
        lightR.update()

        teapot.draw(teapotModelMatrix, vPMatrix)

        //-------------------------------------------------------
        // Problem 4
        // Implement the alpha blending using an extra dissolve texture.

        // % Note
        //   You should first construct a dissolve Texture object and put
        //   it into the teapot material first.

        val mColorHandle = GLES20.glGetUniformLocation(mProgram, "a_alpha")

        if(alpha>0){
            alpha -= 0.01f;
        }

        GLES20.glUniform4fv(mColorHandle, 1, colorA, 0)
        //-------------------------------------------------------
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0,0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        projectionMatrix = myFrustumM(ratio, fov.toFloat(), 2f, 12f)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}

fun vecNormalize(vec:FloatArray){
    val size = sqrt(vec[0].toDouble()*vec[0].toDouble() + vec[1].toDouble()*vec[1].toDouble() + vec[2].toDouble()*vec[2].toDouble()).toFloat()
    vec[0] = vec[0] / size
    vec[1] = vec[1] / size
    vec[2] = vec[2] / size
}

fun mySetLookAtM(eyeX: Float, eyeY: Float, eyeZ: Float, centerX: Float, centerY: Float, centerZ: Float, upX: Float, upY: Float, upZ: Float): FloatArray{
    val viewM = FloatArray(16)
    val n = floatArrayOf(eyeX-centerX, eyeY-centerY, eyeZ-centerZ)
    vecNormalize(n)
    val u = floatArrayOf(upY*n[2] - upZ*n[1], upZ*n[0] - upX*n[2], upX*n[1] - upY*n[0])
    vecNormalize(u)
    val v = floatArrayOf(n[1]*u[2] - n[2]*u[1], n[2]*u[0] - n[0]*u[2], n[0]*u[1] - n[1]*u[0])

    val translateM = floatArrayOf(1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        -eyeX, -eyeY, -eyeZ, 1f
    )
    val rotateM = floatArrayOf(u[0], v[0], n[0], 0f,
        u[1], v[1], n[1], 0f,
        u[2], v[2], n[2], 0f,
        0f, 0f, 0f, 1f
    )
    Matrix.multiplyMM(viewM, 0, rotateM, 0, translateM, 0)

    return viewM
}

fun myFrustumM(aspect:Float, fov:Float, near:Float, far:Float):FloatArray{
    val cotFOV = 1.0f / tan((fov/2.0f).toDouble()).toFloat()
    val projectM = floatArrayOf(cotFOV / aspect, 0f, 0f, 0f,
        0f, cotFOV, 0f, 0f,
        0f, 0f, (far+near)/(far-near), -1f,
        0f, 0f, 2f*near*far/(far-near), 0f
    )
    return projectM
}

class Obj(context: Context, filename: String, program: Int, private var material: Material? = null){

    private var vertices = mutableListOf<Float>()
    private var triangleVertices = mutableListOf<Float>()
    private lateinit var verticesBuffer: FloatBuffer

    private var verticesNormal = mutableListOf<Float>()
    private lateinit var verticesNormalBuffer : FloatBuffer

    private var texCoords = mutableListOf<Float>()
    private lateinit var texCoordsBuffer : FloatBuffer

    private var faces = mutableListOf<Short>()
    private lateinit var facesBuffer : ShortBuffer

    val COORDS_PER_VERTEX = 3 //삼각형단위 계산

    private var vbo: IntArray = IntArray(1)

    init {
        try {
            GLES20.glGenBuffers(1, vbo, 0)
            val scanner = Scanner(context.assets.open(filename))
            while (scanner.hasNextLine()){
                val line = scanner.nextLine()
                when {
                    line.startsWith("v  ") -> {
                        val vertex = line.split(" ")
                        val x = vertex[2].toFloat()
                        val y = vertex[3].toFloat()
                        val z = vertex[4].toFloat()
                        vertices.add(x)
                        vertices.add(y)
                        vertices.add(z)
                    }

                    // Problem 1

                    line.startsWith("vn ") -> {
                        //버텍스 노멀 파싱
                        Log.d("디버깅 코드","버텍스 노멀 파싱시작")

                        val vertex = line.split(" ")
                        val nx = vertex[1].toFloat()
                        val ny = vertex[2].toFloat()
                        val nz = vertex[3].toFloat()
                        verticesNormal.add(nx)
                        verticesNormal.add(ny)
                        verticesNormal.add(nz)

                        Log.d("버텍스노멀x",nx.toString())
                        Log.d("버텍스노멀y",ny.toString())
                        Log.d("버텍스노멀z",nz.toString())

                        //-------------------------------------------------------


                    }
                    line.startsWith("vt ") -> {
                        //texture coord파싱

                        Log.d("디버깅 코드","텍스쳐 쿳 파싱시작")

                        val vertex = line.split(" ")

                        val tx = vertex[1].toFloat()
                        val ty = vertex[2].toFloat()
                        val tz = vertex[3].toFloat()
                        texCoords.add(tx)
                        texCoords.add(ty)
                        texCoords.add(tz)

                        Log.d("텍스쳐x",tx.toString())
                        Log.d("텍스쳐y",ty.toString())
                        Log.d("텍스쳐z",tz.toString())
                        //-------------------------------------------------------
                    }
                    line.startsWith("f ") -> {
                        //-------------------------------------------------------
                        Log.d("디버깅 코드","폴리곤 파싱시작")

                        val face = line.split(" ")
                        val vertex1 = face[1].split("/")[0].toShort()
                        val vertex2 = face[2].split("/")[0].toShort()
                        val vertex3 = face[3].split("/")[0].toShort()
                        val vertex4 = face[4].split("/")[0].toShort()
                        faces.add(vertex1)
                        faces.add(vertex2)
                        faces.add(vertex3)
                        faces.add(vertex3)
                        faces.add(vertex4)
                        faces.add(vertex1) //faces 는 한 줄에 6개, 총 6*6=36

                        //-------------------------------------------------------
                    }
                }
            }
            //-------------------------------------------------------
            // Problem 1
            // Implement .obj loader, parsing vertex (v), texture (vt), and normal (n).

            // % Note
            //   vertex: v(xyz) - vn(xyz) - vt(uv) / 3 - 3 - 2 float structure
            //   1 triangle: 3 vertices

            //   Here, store triangles in the buffer in order based on the obj file.
            //   We already implemented the buffer part using "triangleVertices".
            //   Implement the rest of the code using "triangleVertices".

            // Code

            //-------------------------------------------------------
            verticesBuffer = ByteBuffer.allocateDirect(triangleVertices.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in triangleVertices){
                        put(vertex)
                    }
                    position(0)
                }
            }

            verticesNormalBuffer= ByteBuffer.allocateDirect(verticesNormal.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in verticesNormal){
                        put(vertex)
                    }
                    position(0)
                }
            }


            texCoordsBuffer= ByteBuffer.allocateDirect(texCoords.size * 4).run {
                order(ByteOrder.nativeOrder())
                asFloatBuffer().apply {
                    for (vertex in texCoords){
                        put(vertex)
                    }
                    position(0)
                }
            }


            facesBuffer = ByteBuffer.allocateDirect(faces.size * 2).run {
                order(ByteOrder.nativeOrder())
                asShortBuffer().apply {
                    for (face in faces){
                        put((face-1).toShort())
                    }
                    position(0)
                }
            }

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, 4 * triangleVertices.size, verticesBuffer, GLES20.GL_STATIC_DRAW)




        } catch (e: Exception){
            Log.e("file_read", e.message.toString())
        }
    }

    private var vPMatrixHandle: Int = 0
    private var modelMatrixHandle: Int = 0

    private var positionHandle: Int = 0
    private var texCoordHandle: Int = 0
    private var normalHandle: Int = 0

    var mProgram = program

    fun draw(modelMatrix: Matrix4f, mVPMatrix: FloatArray){
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)

        material?.update(mProgram)

        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 8*4, 0)

        normalHandle = GLES20.glGetAttribLocation(mProgram,"normal")
        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,false, 8*4, 3*4)

        texCoordHandle = GLES20.glGetAttribLocation(mProgram,"texCoord")
        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT,false, 8*4, 6*4)

        modelMatrixHandle = GLES20.glGetUniformLocation(mProgram, "worldMat")
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix.array, 0)

        modelMatrix.inverseTranspose()
        val tpInvModelMatrixHandle = GLES20.glGetUniformLocation(mProgram, "tpInvWorldMat")
        GLES20.glUniformMatrix3fv(tpInvModelMatrixHandle, 1, false, modelMatrix.array, 0)

        vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
        GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mVPMatrix, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, triangleVertices.size / 8)
    }

}

class Texture(context: Context, private val number: Int, resourceId: Int, private val name: String){
    private val textureHandle = IntArray(1)

    init {
        try {
            GLES20.glGenTextures(1, textureHandle, 0)
            if (textureHandle[0] != 0) {
                val options = BitmapFactory.Options()
                options.inScaled = false
                val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_MIRRORED_REPEAT)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_MIRRORED_REPEAT)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)

                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

                bitmap.recycle()
            }
            if (textureHandle[0] == 0) {
                throw RuntimeException("Error loading texture.")
            }
        } catch (e: Exception){
            Log.e("Texture creating ", e.message.toString())
        }
    }

    fun update(program: Int){
        val textureLoc: Int = GLES20.glGetUniformLocation(program, name)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + number)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

        try {
            if (textureLoc >= 0)
                GLES20.glUniform1i(textureLoc, number)
            else
                throw RuntimeException("Error updating texture.")
        }
        catch (e: Exception){
            Log.e("Texture Update ", e.message.toString())
        }
    }
}

class Light(
    var position: FloatArray = floatArrayOf(0f, 0f, 0f),
    var attenuation: FloatArray = floatArrayOf(0.005f, 0.01f, 0.015f),
    var diffuse: FloatArray = floatArrayOf(0.4f, 0.4f, 0.4f),
    var specular: FloatArray = floatArrayOf(0.1f, 0.1f, 0.1f),
    var ambient: FloatArray = floatArrayOf(0.05f, 0.05f, 0.05f),
    val direction: Char){
    var program: Int = 0
    fun update() {
        val srcDiffLoc = GLES20.glGetUniformLocation(program, "srcDiff$direction")
        val srcSpecLoc = GLES20.glGetUniformLocation(program, "srcSpec$direction")
        val srcAmbiLoc = GLES20.glGetUniformLocation(program, "srcAmbi$direction")
        val lightPosLoc = GLES20.glGetUniformLocation(program, "lightPos$direction")
        val lightAttLoc = GLES20.glGetUniformLocation(program, "lightAtt$direction")

        try {
            if (srcDiffLoc >= 0)
                GLES20.glUniform3fv(srcDiffLoc, 1, diffuse, 0)
            else
                throw RuntimeException("Fail to find uniform location: srcDiff$direction")
            if (srcSpecLoc >= 0)
                GLES20.glUniform3fv(srcSpecLoc, 1, specular, 0)
            else
                throw RuntimeException("Fail to find uniform location: srcSpec$direction")
            if (srcAmbiLoc >= 0)
                GLES20.glUniform3fv(srcAmbiLoc, 1, ambient, 0)
            else
                throw RuntimeException("Fail to find uniform location: srcAmbi$direction")
            if (lightPosLoc >= 0)
                GLES20.glUniform3fv(lightPosLoc, 1, position, 0)
            else
                throw RuntimeException("Fail to find uniform location: lightPos$direction")
            if (lightAttLoc >= 0)
                GLES20.glUniform3fv(lightAttLoc, 1, attenuation, 0)
            else
                throw RuntimeException("Fail to find uniform location: lightAtt$direction")
        } catch (e: Exception) {
            Log.e("Light Update ", e.message.toString())
        }
    }
}

class Material(
    private var textureDiff: Texture? = null, private var textureDissolve: Texture? = null,
    private var specular: FloatArray = floatArrayOf(1f, 1f, 1f),
    private var ambient: FloatArray = floatArrayOf(0.6f, 0.6f, 0.6f),
    private var emissive: FloatArray = floatArrayOf(0.3f, 0.3f, 0.3f),
    private var shininess: Float = 5.0f
){
    var threshold = 0f

    fun update(program: Int){
        textureDiff?.update(program)
        textureDissolve?.update(program)

        val matSpecLoc = GLES20.glGetUniformLocation(program, "matSpec")
        val matAmbiLoc = GLES20.glGetUniformLocation(program, "matAmbi")
        val matEmitLoc = GLES20.glGetUniformLocation(program, "matEmit")
        val matShLoc = GLES20.glGetUniformLocation(program, "matSh")
        val thresholdLoc = GLES20.glGetUniformLocation(program, "threshold")

        try {
            if (matSpecLoc >= 0)
                GLES20.glUniform3fv(matSpecLoc, 1, specular, 0)
            else
                throw RuntimeException("Fail to find uniform location: matSpec")
            if (matAmbiLoc >= 0)
                GLES20.glUniform3fv(matAmbiLoc, 1, ambient, 0)
            else
                throw RuntimeException("Fail to find uniform location: matAmbi")
            if (matEmitLoc >= 0)
                GLES20.glUniform3fv(matEmitLoc, 1, emissive, 0)
            else
                throw RuntimeException("Fail to find uniform location: matEmit")
            if (matShLoc >= 0)
                GLES20.glUniform1f(matShLoc, shininess)
            else
                throw RuntimeException("Fail to find uniform location: matSh")
            if (thresholdLoc >= 0)
                GLES20.glUniform1f(thresholdLoc, threshold)
            else
                throw RuntimeException("Fail to find uniform location: threshold")
        }
        catch (e: Exception) {
            Log.e("Material Update", e.message.toString())
        }
    }
}