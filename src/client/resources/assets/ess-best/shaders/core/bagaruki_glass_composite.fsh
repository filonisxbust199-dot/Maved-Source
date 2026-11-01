#version 150

// Glass under hand silhouette: blurred scene, ice refraction, tint, soft edges, frost smoke
uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform GlassData {
    vec4 ResolutionSatReflect; // w, h, saturation, reflect
    vec4 TintColor;            // rgba
    vec4 Settings;             // tintIntensity, edgeGlowIntensity, edgeSoftness, 0
    vec4 IceParams;            // time, iceIntensity, frostScale, crackIntensity
    vec4 SmokeParams;          // amount, scale, speed, reach
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += noise(p) * a;
        p = p * 2.05 + vec2(11.3, 5.7);
        a *= 0.5;
    }
    return v;
}

vec3 applySat(vec3 c, float sat) {
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(l), c, sat);
}

float sampleMask(vec2 uv) {
    return texture(MaskSampler, clamp(uv, vec2(0.001), vec2(0.999))).a;
}

float softMask(vec2 uv, float softnessPx) {
    float m = sampleMask(uv);
    if (softnessPx <= 0.35) return m;
    vec2 texel = 1.0 / max(ResolutionSatReflect.xy, vec2(1.0));
    float r = softnessPx;
    float s = m * 2.0;
    float wsum = 2.0;
    for (int i = 0; i < 8; i++) {
        float a = float(i) * 0.78539816;
        vec2 o = vec2(cos(a), sin(a)) * texel * r;
        float w = 1.0;
        s += sampleMask(uv + o) * w;
        s += sampleMask(uv + o * 0.55) * w * 0.65;
        wsum += w * 1.65;
    }
    // diagonals closer
    s += sampleMask(uv + texel * r * vec2(0.7, 0.7)) * 0.8;
    s += sampleMask(uv + texel * r * vec2(-0.7, 0.7)) * 0.8;
    s += sampleMask(uv + texel * r * vec2(0.7, -0.7)) * 0.8;
    s += sampleMask(uv + texel * r * vec2(-0.7, -0.7)) * 0.8;
    wsum += 3.2;
    return clamp(s / wsum, 0.0, 1.0);
}

void main() {
    float softness = Settings.z;
    float mask = softMask(texCoord, softness);
    if (mask <= 0.003) discard;

    vec2 texel = 1.0 / max(ResolutionSatReflect.xy, vec2(1.0));
    float time = IceParams.x;
    float ice = IceParams.y;
    float frostScale = max(IceParams.z, 1.0);
    float crack = IceParams.w;

    vec2 uv = texCoord;

    // Ice refraction warp (sin/cos + fbm noise)
    if (ice > 0.001) {
        float n1 = sin(uv.x * frostScale + time * 2.1) * cos(uv.y * frostScale * 0.72 - time * 1.35);
        float n2 = fbm(uv * (frostScale * 0.08) + vec2(time * 0.15, -time * 0.11));
        float n3 = fbm(uv * (frostScale * 0.18) + vec2(-time * 0.09, time * 0.13));
        vec2 warp = vec2(n1 + (n2 - 0.5) * 1.4, -n1 * 0.55 + (n3 - 0.5) * 1.2);
        // cracks: sharp directional shear
        if (crack > 0.001) {
            float c = abs(sin((uv.x * 1.7 + uv.y * 3.1) * frostScale * 0.35 + time * 0.4));
            c = pow(1.0 - smoothstep(0.0, 0.08, c), 3.0);
            warp += vec2(c, -c * 0.6) * crack * 2.5;
        }
        uv += warp * texel * ice * 22.0;
        uv = clamp(uv, vec2(0.001), vec2(0.999));
    }

    vec3 scene = texture(SceneSampler, texCoord).rgb;
    vec3 blurred = texture(BlurSampler, uv).rgb;
    // subtle reflection mix of unwarped blur for glass body
    float reflectAmt = clamp(ResolutionSatReflect.w, 0.0, 1.0);
    vec3 body = mix(blurred, texture(BlurSampler, texCoord).rgb, reflectAmt * 0.35);
    body = applySat(body, ResolutionSatReflect.z);

    float tintI = clamp(Settings.x, 0.0, 1.0);
    body = mix(body, TintColor.rgb * max(TintColor.a, 0.0), tintI);

    // Soft fresnel / edge glow from mask gradient
    float edgeInner = softMask(texCoord, max(softness, 1.5));
    float edgeOuter = softMask(texCoord, max(softness * 1.8, 4.0));
    float fresnel = clamp((edgeOuter - edgeInner * 0.85) * 2.4, 0.0, 1.0) * mask;
    float edgeGlowI = Settings.y;
    body = mix(body, body * 1.22 + TintColor.rgb * 0.12 + vec3(0.06), fresnel * (0.35 + edgeGlowI * 0.65));

    // Optional frost smoke above silhouette
    float smokeAmt = SmokeParams.x;
    if (smokeAmt > 0.001 && mask > 0.02) {
        float sc = max(SmokeParams.y, 0.5);
        float sp = SmokeParams.z;
        float reach = SmokeParams.w;
        vec2 suv = texCoord * sc + vec2(0.0, -time * sp);
        float sn = fbm(suv * 3.5);
        float sn2 = fbm(suv * 6.2 + 10.0);
        float plume = smoothstep(0.35, 0.85, sn) * smoothstep(0.2, 0.7, sn2);
        // bias smoke slightly above hand mass
        float above = softMask(texCoord + vec2(0.0, -texel.y * reach * 8.0), softness + 2.0);
        float smoke = plume * smokeAmt * mask * (0.45 + above * 0.55);
        vec3 frost = mix(vec3(0.85, 0.93, 1.0), TintColor.rgb, 0.25);
        body = mix(body, frost, clamp(smoke * 0.55, 0.0, 0.7));
    }

    fragColor = vec4(body, mask);
}
