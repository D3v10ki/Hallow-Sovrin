#version 150

#moj_import <hollowsovereign:void_common.glsl>

in vec3 Position;   // camera-relative vertex position (used only for gl_Position)
in vec4 Color;      // .a = edge falloff (1 centre -> 0 rim), precomputed on CPU
in vec2 UV0;        // CENTRE-RELATIVE world XZ (small, precise) — fixes the patchy-wave bug

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float Time;    // seconds
uniform float Radius;

// Up to 16 recent impacts: xy = centre-relative XZ, z = start time (s), w = active flag.
uniform vec4 Impact0,  Impact1,  Impact2,  Impact3,  Impact4,  Impact5,  Impact6,  Impact7;
uniform vec4 Impact8,  Impact9,  Impact10, Impact11, Impact12, Impact13, Impact14, Impact15;

out vec4 vColor;
out vec2 vRel;
out vec3 vViewPos;

// Strictly transient expanding ring: zero before start, zero after expiry, fades to 0 at end of life.
float impactWave(vec4 imp, vec2 rel, float t) {
    if (imp.w < 0.5) return 0.0;
    float age = t - imp.z;
    if (age < 0.0 || age > 1.6) return 0.0;
    float d = distance(rel, imp.xy);
    float ring = age * 7.0;
    float g = exp(-pow((d - ring) * 1.2, 2.0));
    return g * (1.0 - age / 1.6);
}

void main() {
    vec2 rel = UV0;
    float t = Time;

    float dc = length(rel);
    float falloff = smoothstep(0.0, 1.0, clamp(1.0 - dc / Radius, 0.0, 1.0));

    float h = hs_ambient(rel, t) * 0.16 * falloff;
    h += sin(dc * 0.7 - t * 2.2) * 0.06 * falloff;

    float imp = 0.0;
    imp += impactWave(Impact0, rel, t);   imp += impactWave(Impact1, rel, t);
    imp += impactWave(Impact2, rel, t);   imp += impactWave(Impact3, rel, t);
    imp += impactWave(Impact4, rel, t);   imp += impactWave(Impact5, rel, t);
    imp += impactWave(Impact6, rel, t);   imp += impactWave(Impact7, rel, t);
    imp += impactWave(Impact8, rel, t);   imp += impactWave(Impact9, rel, t);
    imp += impactWave(Impact10, rel, t);  imp += impactWave(Impact11, rel, t);
    imp += impactWave(Impact12, rel, t);  imp += impactWave(Impact13, rel, t);
    imp += impactWave(Impact14, rel, t);  imp += impactWave(Impact15, rel, t);
    h += imp * 0.35 * falloff;

    vec3 disp = Position + vec3(0.0, h, 0.0);
    vViewPos = disp;
    vRel = rel;
    vColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(disp, 1.0);
}
