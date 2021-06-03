precision mediump float;

uniform sampler2D textureDiff;
uniform sampler2D textureDissolve;

uniform vec3 matSpec, matAmbi, matEmit;
uniform float matSh;
uniform vec3 srcDiffL, srcSpecL, srcAmbiL;
uniform vec3 srcDiffR, srcSpecR, srcAmbiR;
uniform float threshold;

varying vec3 v_normal;
varying vec2 v_texCoord;
varying vec3 v_view, v_lightL, v_lightR;
varying float v_attL, v_attR;

uniform vec4 a_alpha;

void main() {
    //-------------------------------------------------------
    // Problem 2
    // Put a texture on the teapot.
    // Change the code below to get the texture value.

    vec3 color = vec3(1.0, 1.0, 1.0);

    //-------------------------------------------------------

    //-------------------------------------------------------
    // Problem 3
    // Implement the phong shader using 2 color point lights.

    // diffuse term
    vec3 matDiff = texture2D(textureDiff,v_texCoord);
    vec3 diffL = max(dot(v_normal, v_lightL), 0.0);
    vec3 diffR = max(dot(v_normal, v_lightR), 0.0);
    vec3 diff = diffL*diffR*matDiff;

    // specular term

    vec3 reflL = max(dot(v_normal, srcSpecL), 0.0);;
    vec3 reflR = max(dot(v_normal, srcSpecR), 0.0);;
    vec3 specL = pow(reflL,matSh);
    vec3 specR = pow(reflR,matSh);
    vec3 spec = specL*specR;

    // ambient term
    vec3 ambiL = srcAmbiL;
    vec3 ambiR = srcAmbiR;
    vec3 ambi = srcAmbiL*srcAmbiR;

    color = diff+spec+ambi;

    //-------------------------------------------------------

    float alpha = 1.0;
    //-------------------------------------------------------
    // Problem 4
    // Implement the alpha blending using an extra dissolve texture.

    alpha = a_alpha.z;
    //-------------------------------------------------------

    // final output color with alpha
    gl_FragColor = vec4(color, alpha);
}