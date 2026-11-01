#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D BloomSampler;

layout(std140) uniform TrailFlame {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = texture(BloomSampler, TexCoord);
    float mask = handsBloomMask(bloom, Params.y);
    if (mask < 0.002) discard;
    float intensity = mask * Params.x;
    vec3 outerColor = GlowColor2.rgb;
    vec3 middleColor = GlowColor1.rgb;
    vec3 hotColor = mix(GlowColor1.rgb, GlowColor2.rgb, 0.22);
    float body = smoothstep(0.05, 0.45, mask);
    float core = smoothstep(0.60, 1.10, intensity);
    vec3 color = mix(outerColor, middleColor, body);
    color = mix(color, hotColor, core);
    if (Params.y > 0.5) color = handsBloomColor(bloom);
    float sourceAlpha = clamp(intensity, 0.0, 1.0);
    float frameScale = clamp(Params.w * 60.0, 0.0, 6.0);
    float alpha = 1.0 - pow(1.0 - sourceAlpha, frameScale);
    float boost = 1.0 + core * 0.55;
    fragColor = vec4(color * alpha * boost, alpha);
}
