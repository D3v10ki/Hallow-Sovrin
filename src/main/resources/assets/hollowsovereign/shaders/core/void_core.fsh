#version 150

uniform float Time;
uniform vec3 TintColor;   // core's own rim tint, independent of floor and disc

in vec3 vViewPos;

out vec4 fragColor;

void main() {
    // geometric normal from screen-space derivatives (captures the noise bumps)
    vec3 n = normalize(cross(dFdx(vViewPos), dFdy(vViewPos)));
    vec3 viewDir = normalize(-vViewPos);
    if (dot(n, viewDir) < 0.0) n = -n;

    // fresnel: near-zero facing the camera, near-one at the silhouette edge -> that IS the rim/halo
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 2.2);
    float pulse = 0.85 + 0.15 * sin(Time * 2.0);

    vec3 body = TintColor * 0.03;                  // near-black event-horizon body
    vec3 rim  = TintColor * fres * 2.4 * pulse;    // glowing fresnel rim/halo
    fragColor = vec4(body + rim, 1.0);             // opaque so it occludes the disc behind it
}
