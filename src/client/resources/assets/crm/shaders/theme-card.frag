#version auto

precision highp float;

#include<shaderColor>

#uniforms {
    vec2 Screen;
    vec4 Rect;
    float Radius;
    float Time;
    float Seed;
    float Style;
    vec4 ColorA;
    vec4 ColorB;
};

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, Screen.y - gl_FragCoord.y);
    vec2 local = frag - Rect.xy;
    vec2 uv = local / max(Rect.zw, vec2(1.0));

    float sd = sdRoundBox(local - Rect.zw * 0.5, Rect.zw * 0.5, Radius);
    float mask = 1.0 - smoothstep(-1.0, 0.5, sd);
    if (mask <= 0.001) {
        discard;
    }

    float aspect = Rect.z / max(Rect.w, 1.0);
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);
    float t = Time * 0.6 + Seed * 37.0;

    vec3 base = mix(ColorB.rgb * 0.72, ColorB.rgb * 1.42, uv.y);
    vec3 accent = ColorA.rgb;
    vec3 color = base;

    int style = int(Style + 0.5);
    if (style == 0) {
        // liquid waves: layered sine crests flowing sideways
        float w1 = sin(p.x * 5.0 + t * 1.1 + sin(p.y * 3.0 + t * 0.7) * 1.2);
        float w2 = sin(p.x * 8.5 - t * 0.8 + p.y * 4.0);
        float crest = 0.5 + 0.5 * sin(w1 * 1.6 + w2 * 0.9 + p.y * 3.5);
        color = mix(color, accent * 0.55, smoothstep(0.45, 0.95, crest) * 0.75);
        color += accent * smoothstep(0.82, 1.0, crest) * 0.55;
    } else if (style == 1) {
        // drifting orbs: three soft glows wandering on lissajous paths
        for (int i = 0; i < 3; i++) {
            float fi = float(i);
            vec2 c = vec2(
                sin(t * (0.5 + fi * 0.17) + fi * 2.4 + Seed) * 0.42 * aspect,
                cos(t * (0.4 + fi * 0.13) + fi * 1.7 + Seed * 2.0) * 0.34
            );
            float d = length(p - c);
            color += accent * exp(-d * d * 22.0) * (0.34 - fi * 0.07);
        }
    } else {
        // diagonal beams sweeping across the card
        float diag = p.x * 0.8 - p.y * 0.6;
        float b1 = 0.5 + 0.5 * sin(diag * 9.0 - t * 1.6);
        float b2 = 0.5 + 0.5 * sin(diag * 17.0 + t * 1.1 + 2.1);
        float beam = smoothstep(0.72, 0.98, b1) * 0.8 + smoothstep(0.86, 1.0, b2) * 0.5;
        color = mix(color, accent * 0.5, smoothstep(0.35, 0.9, b1) * 0.35);
        color += accent * beam * 0.5;
    }

    // soft key light in the upper-left corner + edge falloff
    color += accent * pow(max(0.0, 1.0 - length(uv - vec2(0.18, 0.16))), 3.0) * 0.12;
    float vignette = 1.0 - smoothstep(0.35, 1.25, length(p));
    color *= 0.6 + 0.4 * vignette;

    // ColorA.a carries the panel/category fade (globalAlpha folded in by ColorUtils.of); the
    // card fill is otherwise fully opaque, so without this it would not fade out with the rest
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0) * shaderColor * mask;
    fragColor.a *= ColorA.a;
}
