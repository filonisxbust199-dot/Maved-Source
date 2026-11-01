#version 150

// Nightix burn_trail — rising smoke/ember trail (ping-pong buffer).
uniform sampler2D TrailSampler;
uniform sampler2D GlowSampler;
uniform sampler2D HandSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform BurnData {
    vec4 ResTime;   // width, height, time, fillMode
    vec4 Flame;     // radiusPx, strength, flameSpeed, colorMix
    vec4 Trail;     // decay, flameHeightPx, flowSpeed, trailStrength
    vec4 FireColor; // rgb, glowStrength
};

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
        mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
        f.y);
}

vec4 tap(vec2 p) {
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return vec4(0.0);
    return texture(TrailSampler, p);
}

vec4 softTrail(vec2 p, vec2 px) {
    vec4 c = tap(p) * 0.34;
    c += tap(p + vec2(px.x, 0.0)) * 0.14;
    c += tap(p + vec2(-px.x, 0.0)) * 0.14;
    c += tap(p + vec2(0.0, px.y)) * 0.14;
    c += tap(p + vec2(0.0, -px.y)) * 0.14;
    c += tap(p + px) * 0.05;
    c += tap(p - px) * 0.05;
    return c;
}

void main() {
    float time = ResTime.z;
    float heightPx = max(Trail.y, 4.0);
    float flow = max(Trail.z, 0.05);
    float decay = clamp(Trail.x, 0.85, 0.995);
    float trailStr = max(Trail.w, 0.0);

    vec2 px = 1.0 / max(ResTime.xy, vec2(1.0));

    // smoke rises UP on screen (negative UV.y sample offset = content moves up)
    float riseUv = (heightPx * 0.55) * px.y * flow * 0.085;
    float sway = sin(texCoord.y * 22.0 + time * flow * 3.1)
               + 0.45 * sin(texCoord.y * 41.0 - time * flow * 2.2);
    float turb = (noise(texCoord * 18.0 + vec2(time * flow, 0.0)) - 0.5) * 2.0;
    vec2 disp = vec2((sway * 0.35 + turb * 0.55) * px.x * 2.2, riseUv);

    // sample previous trail from "below" so it appears to drift upward
    vec4 prev = softTrail(texCoord + vec2(disp.x, -disp.y), px * 1.2);

    // subtractive fade so the trail always dies (no eternal fog)
    float flick = 0.94 + 0.06 * sin(time * 11.0 + texCoord.x * 30.0);
    float oldA = prev.a;
    float fade = 0.012 + (1.0 - decay) * 0.08;
    float newA = max(0.0, oldA * decay * flick - fade);
    float sc = oldA > 1e-5 ? newA / oldA : 0.0;
    vec4 trail = vec4(prev.rgb * sc, newA);

    // inject current glow (outside the hard silhouette so smoke peels off the edge)
    vec4 glow = texture(GlowSampler, texCoord);
    float mask = texture(MaskSampler, texCoord).a;
    float injectA = clamp(glow.a * (1.0 - mask * 0.55) * trailStr * 0.85, 0.0, 1.0);
    if (injectA > 0.001) {
        vec3 gcol = glow.a > 1e-4 ? glow.rgb / glow.a : FireColor.rgb;
        // cool toward smoke as it ages — slightly desaturate inject
        vec3 smokeCol = mix(gcol, vec3(dot(gcol, vec3(0.3))), 0.25);
        smokeCol = mix(smokeCol, FireColor.rgb, 0.2 * Flame.w);
        vec4 inject = vec4(smokeCol * injectA, injectA);
        trail = inject + trail * (1.0 - inject.a);
    }

    // faint hand ember seed
    vec4 hand = texture(HandSampler, texCoord);
    float ember = hand.a * mask * 0.08 * trailStr;
    if (ember > 0.001) {
        vec3 ec = hand.rgb / max(hand.a, 1e-3);
        trail.rgb = mix(trail.rgb, ec * ember + trail.rgb, 0.35);
        trail.a = max(trail.a, ember);
    }

    fragColor = trail;
}
