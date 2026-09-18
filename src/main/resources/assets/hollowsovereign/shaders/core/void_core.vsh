#version 150

#moj_import <hollowsovereign:void_common.glsl>

in vec3 Position;   // sphere-LOCAL point = unit direction * Radius (small, precise)
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;   // view * translate(centre)  (set from Java; carries world placement)
uniform mat4 ProjMat;
uniform float Time;
uniform float Radius;

out vec3 vViewPos;

void main() {
    vec3 dir = normalize(Position);
    float n = hs_sphereNoise(dir * 2.0, Time);       // organic 3D distortion over the surface
    float disp = n * Radius * 0.06;                   // small bumps relative to the radius
    vec3 local = dir * (Radius + disp);

    vec4 viewPos = ModelViewMat * vec4(local, 1.0);
    vViewPos = viewPos.xyz;
    gl_Position = ProjMat * viewPos;
}
