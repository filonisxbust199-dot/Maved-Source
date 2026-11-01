#version 150

// BagaEsp combine — Nightix ShaderEsp outline/glow/fill composite
uniform sampler2D maskTexture;
uniform sampler2D glowTexture;

layout(std140) uniform EspData {
    vec4 ColorOpacity;      // rgb color, a opacity
    vec4 FriendFlag;        // rgb friend color, a friendEnabled
    vec4 Params1;           // texelX, texelY, outlineWidth, outlineMode
    vec4 Params2;           // glowStrength, glowFalloff, fillOpacity, innerGlow
    vec4 Params3;           // outlineStrength, outlineWhite, glowEnabled, fillEnabled
    vec4 Params4;           // shimmerT, shimmerEnabled, shimmerWidth, shimmerBrightness
    vec4 Params5;           // pulseAmount, pulseT, saturation, outlineEnabled
};

in vec2 texCoord;
out vec4 fragColor;

vec3 applySat(vec3 c, float sat) {
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(l), c, sat);
}

void main() {
    vec4 maskSample = texture(maskTexture, texCoord);
    float mask = maskSample.a;
    vec4 glowSample = texture(glowTexture, texCoord);
    float glowA = glowSample.a;

    vec2 texel = Params1.xy;
    float outlineW = max(Params1.z, 1.0);
    int outlineMode = int(Params1.w + 0.5);

    float glowStrength = Params2.x;
    float glowFalloff = max(Params2.y, 0.05);
    float fillOpacity = Params2.z;
    float innerGlow = Params2.w;

    float outlineStrength = Params3.x;
    float outlineWhite = clamp(Params3.y, 0.0, 1.0);
    bool glowOn = Params3.z > 0.5;
    bool fillOn = Params3.w > 0.5;
    bool outlineOn = Params5.w > 0.5;

    // early out far from silhouettes
    if (mask <= 0.0 && glowA <= 0.001) {
        vec2 reach = texel * outlineW * 2.0;
        float probe =
              texture(maskTexture, texCoord + vec2(reach.x, 0.0)).a
            + texture(maskTexture, texCoord - vec2(reach.x, 0.0)).a
            + texture(maskTexture, texCoord + vec2(0.0, reach.y)).a
            + texture(maskTexture, texCoord - vec2(0.0, reach.y)).a
            + glowA;
        if (probe <= 0.0) discard;
    }

    // base tint from mask RGB (friends write green into the outline FBO)
    vec3 base = maskSample.rgb;
    if (dot(base, base) < 1.0e-4) base = ColorOpacity.rgb;
    // friend override when mask is mostly green-ish and friend flag on
    if (FriendFlag.a > 0.5) {
        float greenBias = base.g - max(base.r, base.b);
        if (greenBias > 0.15) base = FriendFlag.rgb;
    }

    float pulse = 1.0;
    if (Params5.x > 0.001) {
        float p = 0.5 + 0.5 * sin(Params5.y * 6.2831853);
        pulse = 1.0 - Params5.x + Params5.x * p;
    }

    float sat = max(Params5.z, 0.0);
    base = applySat(base, sat);

    // outer glow from kawase pyramid
    float outerGlow = 0.0;
    if (glowOn) {
        outerGlow = pow(clamp(glowA, 0.0, 1.0), glowFalloff) * glowStrength * (1.0 - mask);
    }

    // fill + soft inner light
    float fill = 0.0;
    if (fillOn) {
        fill = mask * fillOpacity;
        fill += mask * (1.0 - glowA) * innerGlow * 0.35;
    }

    // edge outline via neighborhood max
    float outline = 0.0;
    if (outlineOn) {
        float edge = 0.0;
        float interior = 1.0;
        for (int i = 0; i < 8; i++) {
            float a = float(i) * 0.78539816339;
            vec2 o = vec2(cos(a), sin(a)) * texel * outlineW;
            float neighbor = texture(maskTexture, texCoord + o).a;
            edge = max(edge, neighbor);
            interior = min(interior, neighbor);
        }
        float outer = clamp(edge - mask, 0.0, 1.0);
        float inner = clamp(mask - interior, 0.0, 1.0) * mask;
        if (outlineMode == 0) outline = outer;
        else if (outlineMode == 1) outline = inner;
        else outline = max(outer, inner * 0.85);
        outline *= outlineStrength;
    }

    // diagonal shimmer band
    float shimmer = 0.0;
    if (Params4.y > 0.5 && mask > 0.01) {
        float aspect = Params1.y / max(Params1.x, 1.0e-6);
        float band = fract(texCoord.x * aspect * 0.35 + texCoord.y * 0.65 - Params4.x);
        float w = max(Params4.z, 0.005);
        shimmer = (1.0 - smoothstep(0.0, w, abs(band - 0.5))) * Params4.w * mask;
    }

    float intensity = (fill + outerGlow + outline + shimmer) * ColorOpacity.a * pulse;
    if (intensity <= 0.003) discard;

    vec3 rgb = base;
    rgb = mix(rgb, vec3(1.0), clamp(outline * outlineWhite + shimmer * 0.5, 0.0, 1.0));
    rgb += base * outerGlow * 0.55;

    fragColor = vec4(rgb, clamp(intensity, 0.0, 1.0));
}
