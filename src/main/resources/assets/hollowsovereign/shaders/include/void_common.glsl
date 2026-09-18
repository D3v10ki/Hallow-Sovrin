// Shared helpers for the void surface shaders (floor + accretion disc).
// Edit here once — both the floor and disc shaders #moj_import this file, so the displacement,
// ripple-normal and surface-shading logic lives in a single place instead of two copies.

// Layered sine "water" height field over continuous (centre-relative) coords.
float hs_ambient(vec2 p, float t) {
    float h = 0.0;
    h += sin(p.x * 0.60 + t * 1.3) * 0.50;
    h += sin(p.y * 0.55 - t * 1.1) * 0.50;
    h += sin((p.x + p.y) * 0.40 + t * 0.9) * 0.35;
    h += sin((p.x - p.y) * 0.50 - t * 1.7) * 0.25;
    return h;
}

// 3D organic noise for displacing a SPHERE surface (the 2D helpers above don't wrap onto a sphere).
// Sum of moving 3D sine lobes — cheap, seamless on a sphere, output roughly in [-1.5, 1.5].
float hs_sphereNoise(vec3 p, float t) {
    float n = sin(p.x * 3.0 + t * 0.9) * sin(p.y * 3.0 - t * 0.7) * sin(p.z * 3.0 + t * 0.5);
    n += 0.5 * sin(p.x * 6.0 - t * 1.1) * sin(p.y * 6.0 + t * 0.8) * sin(p.z * 6.0 - t * 0.6);
    return n;
}

// Fine scrolling ripple detail as a perturbation normal (the "scrolling normal map", procedural).
vec3 hs_rippleNormal(vec2 p, float t) {
    float nx = cos(p.x * 1.7 + t * 2.1) * 0.20 + cos((p.x + p.y) * 1.1 + t * 1.4) * 0.14;
    float nz = cos(p.y * 1.5 - t * 1.9) * 0.20 + cos((p.x - p.y) * 1.3 - t * 1.6) * 0.14;
    return normalize(vec3(nx, 1.0, nz));
}

// Water-like surface shading: fresnel edges, faked star reflection, tinted base + pulsing emissive.
vec4 hs_shade(vec3 viewPos, vec3 normal, float time, vec3 tint, float edgeAlpha, sampler2D reflTex) {
    vec3 viewDir = normalize(-viewPos);
    float fres = pow(1.0 - max(dot(normal, viewDir), 0.0), 3.0);

    vec3 refl = reflect(-viewDir, normal);
    vec2 ruv = clamp(refl.xz * 0.5 + 0.5, 0.0, 1.0);
    vec3 reflCol = texture(reflTex, ruv).rgb;

    float pulse = 0.85 + 0.15 * sin(time * 2.0);
    vec3 col = tint * 0.08;                    // dark base derived from the configured colour
    col += tint * fres * 1.3 * pulse;          // fresnel edges glow
    col += reflCol * (0.20 + 0.55 * fres);     // fake reflection, stronger at glancing angles
    col += tint * 0.12 * pulse;                // faint constant emissive

    float alpha = edgeAlpha * (0.55 + 0.45 * fres);
    return vec4(col, clamp(alpha, 0.0, 1.0));
}
