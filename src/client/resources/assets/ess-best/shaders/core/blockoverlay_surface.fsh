#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform OverlayParams {
    vec4 Primary;   // rgb theme, a fill opacity
    vec4 Secondary; // rgb theme, a halo strength
    vec4 Style;     // time, pattern scale, style index, line width in face units
    vec4 Details;   // corner length, camera-relative box center xyz
};
layout(location = 0) in vec2 faceUV;
layout(location = 1) in vec2 faceSize;
layout(location = 2) in vec3 surfacePosition;
layout(location = 3) in float presence;
layout(location = 0) out vec4 fragColor;

float segment(vec2 p, vec2 a, vec2 b) {
    vec2 d = b - a;
    return length(p - a - d * clamp(dot(p - a, d) / max(dot(d, d), 0.00001), 0.0, 1.0));
}
float hexDistance(vec2 p) {
    p = abs(p);
    return max(dot(p, vec2(0.8660254, 0.5)), p.y);
}
float hash3(vec3 p) {
    // Arithmetic hash avoids dozens of transcendental operations per plasma pixel.
    p = fract(p * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yxz + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    vec3 s = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash3(i), hash3(i + vec3(1, 0, 0)), s.x),
                   mix(hash3(i + vec3(0, 1, 0)), hash3(i + vec3(1, 1, 0)), s.x), s.y),
               mix(mix(hash3(i + vec3(0, 0, 1)), hash3(i + vec3(1, 0, 1)), s.x),
                   mix(hash3(i + vec3(0, 1, 1)), hash3(i + vec3(1)), s.x), s.y), s.z);
}
void main() {
    int mode = int(Style.z + 0.5);
    vec2 uv = faceUV;
#ifndef OVERLAY_PATTERN
    vec2 surface = uv * faceSize;
    float aa = max(max(fwidth(surface.x), fwidth(surface.y)), 0.0005);
    float outside = max(max(-surface.x, surface.x - faceSize.x), max(-surface.y, surface.y - faceSize.y));
    float inside = 1.0 - smoothstep(-aa, aa, outside);
    float rim = min(min(segment(surface, vec2(0), vec2(faceSize.x, 0)), segment(surface, vec2(faceSize.x, 0), faceSize)),
                    min(segment(surface, faceSize, vec2(0, faceSize.y)), segment(surface, vec2(0, faceSize.y), vec2(0))));
    float width = Style.w;
    float distance = rim;
    if (mode == 6) {
        distance = 10.0;
        for (int i = 0; i < 4; ++i) {
            vec2 corner = vec2(float(i == 1 || i == 2), float(i >= 2));
            vec2 c = corner * faceSize;
            vec2 dx = vec2(corner.x < 0.5 ? faceSize.x : -faceSize.x, 0);
            vec2 dy = vec2(0, corner.y < 0.5 ? faceSize.y : -faceSize.y);
            distance = min(distance, min(segment(surface, c, c + dx * Details.x),
                                         segment(surface, c, c + dy * Details.x)));
        }
    }
    float core = 1.0 - smoothstep(width, width + aa, distance);
#endif
#ifdef OVERLAY_GLOW
    // Add light to the scene instead of replacing it with a translucent colored rectangle.
    // A narrow luminous fringe and a wide soft aura share the rounded segment distance.
    float nearHalo = exp(-distance * distance / (0.00065 + width * width * 2.0));
    float wideHalo = exp(-distance * distance / (0.0045 + width * width * 4.0));
    float halo = (nearHalo * 0.65 + wideHalo * 0.32) * Secondary.a;
    halo *= 1.0 - smoothstep(0.11, 0.155, max(outside, 0.0));
    float alpha = min(halo, 1.2) * presence;
    if (alpha < 0.002) discard;
    vec3 hot = mix(Primary.rgb, vec3(1.0), 0.7);
    fragColor = vec4(mix(Primary.rgb, hot, nearHalo * 0.65), min(alpha, 1.0));
#elif defined(OVERLAY_PATTERN)
    float t = Style.x;
    float pattern = 0.0;
    vec3 tint = Primary.rgb;
    vec2 p = uv * (4.0 / max(Style.y, 0.1));
    vec3 domain = (surfacePosition - Details.yzw) * (4.0 / max(Style.y, 0.1));
    if (mode == 0) { // One continuous 3D field shared by every face, no UV restart at edges.
        vec3 warp = vec3(noise3(domain + vec3(t * 0.12, 0, 0)),
                         noise3(domain + vec3(4.7, -t * 0.09, 1.3)),
                         noise3(domain + vec3(2.1, 3.4, t * 0.07)));
        float n = noise3(domain * 1.7 + warp * 1.8);
        pattern = smoothstep(0.25, 0.8, n);
        tint = mix(Secondary.rgb, Primary.rgb, pattern);
    } else if (mode == 1) { // Topography: thin nested contours, slow relief motion.
        float h = noise3(domain * 0.6 + vec3(t * 0.035, 0, 0)) + 0.35 * noise3(domain * 1.5);
        float phase = h * 7.0 - t * 0.07;
        float d = abs(fract(phase) - 0.5);
        pattern = 1.0 - smoothstep(0.025, 0.025 + max(fwidth(phase), 0.01), d);
        tint = mix(Secondary.rgb, Primary.rgb, clamp(h, 0.0, 1.0));
    } else if (mode == 3) { // Hex lattice: stable cells with a travelling energy pulse.
        vec2 cell = p * 1.5;
        vec2 tile = vec2(1.7320508, 1.0);
        vec2 a = mod(cell, tile) - tile * 0.5;
        vec2 b = mod(cell - tile * 0.5, tile) - tile * 0.5;
        vec2 h = dot(a, a) < dot(b, b) ? a : b;
        float edge = abs(hexDistance(h) - 0.5);
        pattern = (1.0 - smoothstep(0.025, 0.025 + max(fwidth(edge), 0.01), edge))
                * (0.55 + 0.45 * sin(length(uv - 0.5) * 10.0 - t * 1.7));
        tint = mix(Secondary.rgb, Primary.rgb, 0.6);
    }
    // Opacity is constant across the closed shader surface, including shared edges.
    tint = mix(tint * 0.45, mix(tint, Secondary.rgb, 0.25), clamp(pattern, 0.0, 1.0));
    float alpha = Primary.a * presence;
    if (alpha < 0.002) discard;
    fragColor = vec4(tint, alpha);
#else
    float fill = Primary.a * inside * 0.35;
    float light = core * 0.9;
    float alpha = (fill + light * (1.0 - fill)) * presence;
    vec3 hot = mix(Primary.rgb, vec3(1.0), 0.7);
    vec3 color = mix(Primary.rgb, hot, light / max(fill + light, 0.001));
    if (alpha < 0.002) discard;
    fragColor = vec4(color, alpha);
#endif
}
