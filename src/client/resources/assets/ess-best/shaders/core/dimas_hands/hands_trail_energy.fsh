#version 330

#moj_import <ess-best:dimas_hands_item_color.glsl>

uniform sampler2D BloomSampler;

layout(std140) uniform TrailEnergy {
    vec4 Params;
    vec4 EnergyParams;
    vec4 GlowColor1;
    vec4 GlowColor2;
};

in vec2 TexCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = texture(BloomSampler, TexCoord);
    float mask = handsBloomMask(bloom, Params.y);
    if (mask < 0.001) discard;

    float time = EnergyParams.x;
    float strength = EnergyParams.y;
    float pulseStrength = EnergyParams.z;
    float exposure = Params.x * strength;
    float halo = smoothstep(0.002, 0.24, mask);
    float body = smoothstep(0.10, 0.58, mask);
    float core = smoothstep(0.38, 0.92, mask * exposure);

    float gradient = smoothstep(0.18, 0.92, TexCoord.y);
    vec3 color1 = Params.y > 0.5 ? handsBloomColor(bloom) : GlowColor1.rgb;
    vec3 color2 = Params.y > 0.5 ? color1 : GlowColor2.rgb;
    vec3 baseColor = mix(color1, color2, gradient);
    vec3 outerColor = mix(color2, baseColor, 0.35);

    float filamentA = 0.5 + 0.5 * sin(
            TexCoord.y * 92.0 + sin(TexCoord.x * 31.0 - time * 1.7) * 2.2 - time * 8.0
    );
    float filamentB = 0.5 + 0.5 * sin(
            TexCoord.x * 67.0 - TexCoord.y * 29.0 + time * 5.3
    );
    float filament = pow(max(filamentA, filamentB * 0.82), 7.0) * halo * pulseStrength;
    float pulse = 1.0 + 0.12 * pulseStrength
            * sin(time * (3.5 + pulseStrength * 2.0) + TexCoord.y * 18.0);

    vec3 color = mix(outerColor * 0.68, baseColor * 1.18, body);
    color = mix(color, min(baseColor * 1.22, vec3(1.0)), core * 0.72);
    color += mix(baseColor, color1, 0.55) * filament * 0.42;

    float sourceAlpha = clamp(
            (halo * 0.22 + body * 0.38 + core * 0.34 + filament * 0.12)
                    * exposure * pulse,
            0.0, 0.94
    );
    float frameScale = clamp(Params.w * 60.0, 0.0, 6.0);
    float alpha = 1.0 - pow(1.0 - sourceAlpha, frameScale);
    float lightBoost = 1.20 + core * 0.48 + filament * 0.20;

    fragColor = vec4(color * alpha * lightBoost, alpha);
}
