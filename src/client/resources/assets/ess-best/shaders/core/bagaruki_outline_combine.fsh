#version 150

// Soft outer glow + sharp rim + diagonal shimmer on hand mask
uniform sampler2D MaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D SceneSampler;

layout(std140) uniform OutlineData {
    vec4 Color;     // rgb, a = intensity
    vec4 Params;    // texel.x, texel.y, widthPx, shimmerT
    vec4 Shimmer;   // enabled, width, brightness, 0
    vec4 Extra;     // rimStrength, outerBoost, 0, 0
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float mask = texture(MaskSampler, texCoord).a;
    float glowA = texture(GlowSampler, texCoord).a;
    vec2 texel = Params.xy;
    float width = max(Params.z, 1.0);

    // neighborhood rim
    float edge = 0.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.78539816;
        vec2 o = vec2(cos(a), sin(a)) * texel * width;
        edge = max(edge, texture(MaskSampler, texCoord + o).a);
    }
    float outerRim = clamp(edge - mask, 0.0, 1.0);
    float softOuter = clamp(glowA * Extra.y, 0.0, 1.0) * (1.0 - mask);

    float intensity = softOuter * Color.a + outerRim * Extra.x * Color.a;
    if (intensity <= 0.002 && mask <= 0.01) discard;

    vec3 rgb = Color.rgb;
    // slight scene lift on rim for glass edge
    vec3 scene = texture(SceneSampler, texCoord).rgb;
    rgb = mix(rgb, mix(rgb, scene + rgb * 0.35, 0.25), outerRim);

    if (Shimmer.x > 0.5 && (mask > 0.01 || outerRim > 0.01)) {
        float aspect = Params.y / max(Params.x, 1e-6);
        float band = fract(texCoord.x * aspect * 0.38 + texCoord.y * 0.62 - Params.w);
        float w = max(Shimmer.y, 0.005);
        float sh = (1.0 - smoothstep(0.0, w, abs(band - 0.5))) * Shimmer.z;
        sh *= max(mask, outerRim * 1.2);
        rgb = mix(rgb, vec3(1.0), clamp(sh * 0.85, 0.0, 1.0));
        intensity += sh * 0.4 * Color.a;
    }

    fragColor = vec4(rgb, clamp(intensity, 0.0, 1.0));
}
