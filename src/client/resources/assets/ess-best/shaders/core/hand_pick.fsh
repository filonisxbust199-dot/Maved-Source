#version 150

// Подсветка предмета в руке для редактора ViewModel: наружный контур по силуэту
// из изолированного таргета рук + лёгкая заливка. Рисуется аддитивно (ONE, ONE,
// только RGB), поэтому выход — премультиплицированный свет, как у shader_hands.
uniform sampler2D HandsSampler;

layout(std140) uniform PickData {
    vec4 uColor;   // rgb = цвет подсветки, w = сила обводки
    vec4 uParams;  // x = сила заливки, y = шаг текселя по X, z = по Y
    vec4 uRegion;  // xy = UV region, z = KillEffect style, w = time
    vec4 uEffect;  // x = disappearance 0..1, y = sand mode, zw = framebuffer size
};

in vec2 texCoord;
out vec4 fragColor;

float pickHash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    vec2 uv = texCoord;
    if (uv.x < uRegion.x || uv.x > uRegion.y) {
        fragColor = vec4(0.0);
        return;
    }
    float a = texture(HandsSampler, uv).a;
    vec2 t = uParams.yz;

    // наружный контур: максимум альфы восьми соседей минус своя — светится только кромка
    float n = 0.0;
    n = max(n, texture(HandsSampler, uv + vec2( t.x, 0.0)).a);
    n = max(n, texture(HandsSampler, uv + vec2(-t.x, 0.0)).a);
    n = max(n, texture(HandsSampler, uv + vec2(0.0,  t.y)).a);
    n = max(n, texture(HandsSampler, uv + vec2(0.0, -t.y)).a);
    n = max(n, texture(HandsSampler, uv + vec2( t.x,  t.y)).a);
    n = max(n, texture(HandsSampler, uv + vec2(-t.x,  t.y)).a);
    n = max(n, texture(HandsSampler, uv + vec2( t.x, -t.y)).a);
    n = max(n, texture(HandsSampler, uv + vec2(-t.x, -t.y)).a);
    float edge = clamp(n - a, 0.0, 1.0);

    float outline = edge * uColor.w;
    float body = a * uParams.x;
    if (uRegion.z > 0.5) {
        // Inner edge makes the contour stay readable over a bright sky. Sparse stable cells
        // pulse independently, resembling tiny particles running around model corners.
        float low = 1.0;
        low = min(low, texture(HandsSampler, uv + vec2( t.x, 0.0)).a);
        low = min(low, texture(HandsSampler, uv + vec2(-t.x, 0.0)).a);
        low = min(low, texture(HandsSampler, uv + vec2(0.0,  t.y)).a);
        low = min(low, texture(HandsSampler, uv + vec2(0.0, -t.y)).a);
        float inner = clamp(a - low, 0.0, 1.0);
        float seed = pickHash(floor(gl_FragCoord.xy / 3.0));
        float pulse = pow(max(0.0, sin(uRegion.w * 7.5 + seed * 51.0)), 13.0);
        float sparkle = (edge + inner) * step(0.72, seed) * pulse;
        outline += inner * uColor.w * 0.42 + sparkle * uColor.w * 2.35;

        if (uEffect.y > 0.5) {
            float progress = clamp(uEffect.x, 0.0, 1.0);
            float cell = pickHash(floor(gl_FragCoord.xy / 4.0));
            float threshold = progress * 1.08 - 0.08;
            float keep = smoothstep(threshold, threshold + 0.12, cell);
            outline *= keep;
            body *= keep;

            // Sparse shifted copies of the real silhouette form grains falling down.
            // texCoord.y grows upward, therefore sampling +fall places the source above
            // the current fragment and renders the grain below its original edge.
            float grains = 0.0;
            for (int i = 0; i < 6; i++) {
                float layer = (float(i) + 1.0) / 6.0;
                float fall = progress * progress * (0.025 + layer * 0.17);
                float sway = sin(uRegion.w * (1.7 + layer) + layer * 29.0)
                        * (0.0015 + layer * 0.0045);
                vec2 sourceUv = uv + vec2(sway, fall);
                float source = texture(HandsSampler, sourceUv).a;
                float releaseSeed = pickHash(floor(gl_FragCoord.xy / 2.0)
                        + vec2(layer * 37.0, layer * 19.0));
                float released = step(releaseSeed * 0.92, progress);
                float grainDot = step(0.76, pickHash(floor(gl_FragCoord.xy / 2.0)
                        + vec2(layer * 73.0, uRegion.w * 0.15)));
                grains += source * released * grainDot * (0.32 + layer * 0.22);
            }
            grains = min(grains, 1.0) * (1.0 - a * 0.72);
            outline += grains * uColor.w * (1.35 - progress * 0.35);
        }
    }

    vec3 col = uColor.rgb * (outline + body);
    fragColor = vec4(col, 1.0);
}
