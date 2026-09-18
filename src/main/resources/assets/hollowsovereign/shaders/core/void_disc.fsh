#version 150

#moj_import <hollowsovereign:void_common.glsl>

uniform sampler2D Sampler0;
uniform float Time;
uniform vec3 TintColor;   // independent disc colour (e.g. orange), separate from the floor

in vec4 vColor;
in vec2 vLocal;
in vec3 vViewPos;

out vec4 fragColor;

void main() {
    vec3 gn = normalize(cross(dFdx(vViewPos), dFdy(vViewPos)));
    if (dot(gn, normalize(-vViewPos)) < 0.0) gn = -gn;

    vec3 rn = hs_rippleNormal(vLocal, Time);
    vec3 normal = normalize(gn + vec3(rn.x, 0.0, rn.z) * 0.5);

    fragColor = hs_shade(vViewPos, normal, Time, TintColor, vColor.a, Sampler0);
}
