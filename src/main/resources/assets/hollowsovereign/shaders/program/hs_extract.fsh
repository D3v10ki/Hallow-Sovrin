#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;

uniform float Threshold;

out vec4 fragColor;

void main() {
    vec4 c = texture(DiffuseSampler, texCoord);
    float luma = dot(c.rgb, vec3(0.2126, 0.7152, 0.0722));
    // soft knee: only pixels brighter than the threshold contribute to bloom
    float k = smoothstep(Threshold, Threshold + 0.25, luma);
    fragColor = vec4(c.rgb * k, 1.0);
}
