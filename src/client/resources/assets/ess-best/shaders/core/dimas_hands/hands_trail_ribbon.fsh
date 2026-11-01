#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D BloomSampler;

layout(std140) uniform TrailRibbon {
    vec4 Params;
    vec4 RibbonParams;
    vec4 GlowColor1;
    vec4 GlowColor2;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = texture(BloomSampler, TexCoord);
    float mask = handsBloomMask(bloom, RibbonParams.z);
    if (mask < 0.003 || mask > 0.98) discard;
    float time = Params.y;
    float width = clamp(Params.w, 0.25, 1.5);
    vec2 position = TexCoord * vec2(RibbonParams.y, 1.0);
    float bend = sin(position.y * 19.0 - time * 2.3
            + sin(position.x * 8.0 + time) * 1.2) * RibbonParams.x;
    float contour = mask * 21.0 + bend * 0.65;
    float ribbon = pow(0.5 + 0.5 * cos(contour), 5.0 / width);
    float halo = smoothstep(0.003, 0.045, mask) * (1.0 - smoothstep(0.6, 0.98, mask));
    float flow = 0.82 + 0.18 * sin(position.y * 22.0 - time * 4.0 + contour * 0.4);
    float gradient = 0.5 + 0.5 * sin(position.y * 5.0 - time * 0.8 + mask * 3.0);
    vec3 color = mix(GlowColor1.rgb, GlowColor2.rgb, gradient);
    float colorAlpha = mix(GlowColor1.a, GlowColor2.a, gradient);
    if (RibbonParams.z > 0.5) {
        color = handsBloomColor(bloom);
        colorAlpha = 1.0;
    }
    float sourceAlpha = clamp(halo * (0.045 + ribbon * 0.58) * flow * Params.x * colorAlpha, 0.0, 0.85);
    float alpha = 1.0 - pow(1.0 - sourceAlpha, clamp(Params.z * 60.0, 0.0, 6.0));
    fragColor = vec4(color * alpha, alpha);
}
