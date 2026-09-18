#version 150

#moj_import <hollowsovereign:void_common.glsl>

uniform sampler2D Sampler0;   // fake-reflection star gradient (clamped, UV in [0,1])
uniform float Time;
uniform vec3 TintColor;       // configurable per-frame from Java

in vec4 vColor;
in vec2 vRel;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    vec3 gn = normalize(cross(dFdx(vViewPos), dFdy(vViewPos)));
    if (gn.y < 0.0) gn = -gn;

    vec3 rn = hs_rippleNormal(vRel, Time);
    vec3 normal = normalize(gn + vec3(rn.x, 0.0, rn.z) * 0.6);

    fragColor = hs_shade(vViewPos, normal, Time, TintColor, vColor.a, Sampler0);
}
