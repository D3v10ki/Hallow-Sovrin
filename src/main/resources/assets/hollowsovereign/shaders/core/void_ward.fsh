#version 150

uniform float Time;
uniform vec3 TintColor;   // ward tint (void purple)

in vec3 vViewPos;
in float vFlare;

out vec4 fragColor;

void main() {
    // geometric normal from screen-space derivatives
    vec3 n = normalize(cross(dFdx(vViewPos), dFdy(vViewPos)));
    vec3 viewDir = normalize(-vViewPos);
    if (dot(n, viewDir) < 0.0) n = -n;

    // fresnel rim (same technique as the black-hole core), so the dome glows at its silhouette
    float fres = pow(1.0 - max(dot(n, viewDir), 0.0), 2.5);
    float pulse = 0.8 + 0.2 * sin(Time * 3.0);

    vec3 col = TintColor * (0.10 + fres * 1.8 * pulse);   // faint translucent body + glowing rim
    col += TintColor * vFlare * 2.6;                       // bright flare where a hit landed

    // translucent so the player is visible inside; rim + flare drive most of the opacity
    float alpha = clamp(0.10 + fres * 0.5 + vFlare * 0.6, 0.0, 0.85);
    fragColor = vec4(col, alpha);
}
