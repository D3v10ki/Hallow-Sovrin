#version 150

#moj_import <hollowsovereign:void_common.glsl>

in vec3 Position;   // sphere-LOCAL point = unit direction * Radius (small, precise)
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;   // view * translate(player centre)
uniform mat4 ProjMat;
uniform float Time;
uniform float Radius;

// Up to 4 melee impacts on the dome: xyz = UNIT direction of the incoming hit, w = start time (s).
uniform vec4 Impact0, Impact1, Impact2, Impact3;

out vec3 vViewPos;
out float vFlare;   // brightness boost near a fresh impact, passed to the fragment stage

// Expanding angular ring around the impact direction (mirrors the floor's impactWave, but on a sphere).
float wardImpact(vec4 imp, vec3 dir, float t) {
    if (dot(imp.xyz, imp.xyz) < 0.0001) return 0.0;   // empty slot
    float age = t - imp.w;
    if (age < 0.0 || age > 0.7) return 0.0;
    float ang = acos(clamp(dot(dir, normalize(imp.xyz)), -1.0, 1.0));
    float ring = age * 4.0;                             // radians/sec
    float g = exp(-pow((ang - ring) * 2.2, 2.0));
    return g * (1.0 - age / 0.7);
}

void main() {
    vec3 dir = normalize(Position);
    float n = hs_sphereNoise(dir * 2.0, Time);          // gentle organic shimmer
    float disp = n * Radius * 0.03;

    float flare = 0.0;
    flare += wardImpact(Impact0, dir, Time);
    flare += wardImpact(Impact1, dir, Time);
    flare += wardImpact(Impact2, dir, Time);
    flare += wardImpact(Impact3, dir, Time);
    vFlare = flare;

    vec3 local = dir * (Radius + disp + flare * Radius * 0.12);  // bulge outward at the hit
    vec4 viewPos = ModelViewMat * vec4(local, 1.0);
    vViewPos = viewPos.xyz;
    gl_Position = ProjMat * viewPos;
}
