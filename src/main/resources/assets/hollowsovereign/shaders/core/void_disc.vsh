#version 150

#moj_import <hollowsovereign:void_common.glsl>

in vec3 Position;   // disc-LOCAL coords in the XZ plane (y unused); ModelViewMat carries tilt+placement
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;   // view * translate(discCentre) * tiltX  (set from Java)
uniform mat4 ProjMat;
uniform float Time;
uniform float InnerR;
uniform float OuterR;

out vec4 vColor;
out vec2 vLocal;
out vec3 vViewPos;

// Rotating spiral height field — displacement driven by swirl motion, not distance ripples.
float swirl(vec2 p, float t) {
    float r = length(p);
    float ang = atan(p.y, p.x);
    float h = sin(ang * 2.0 - r * 0.35 + t * 2.6);
    h += 0.5 * sin(ang * 3.0 + r * 0.25 - t * 1.7);
    return h;
}

void main() {
    vec2 p = Position.xz;                 // disc-local (small, precise — same coord fix as the floor)
    float r = length(p);

    float fin = smoothstep(InnerR, InnerR + 3.0, r);
    float fout = 1.0 - smoothstep(OuterR - 6.0, OuterR, r);
    float fall = fin * fout;              // fade at the inner hole and the outer rim

    float disp = swirl(p, Time) * 0.6 * fall;

    vec3 local = vec3(p.x, disp, p.y);    // displace along the disc's own normal (local Y)
    vec4 viewPos = ModelViewMat * vec4(local, 1.0);
    vViewPos = viewPos.xyz;
    vLocal = p;
    vColor = vec4(Color.rgb, fall);
    gl_Position = ProjMat * viewPos;
}
