#version 150

uniform sampler2D Sampler0;

layout(std140) uniform EspSettings {
    vec4 EspColor;   // outline / base color, a = master alpha
    vec4 EspColor2;  // second stop of the flowing fill gradient
    vec4 EspParams;  // x: texel.x, y: texel.y, z: glow width in px, w: mode
    vec4 EspParams2; // x: fill opacity, y: time
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 center = texture(Sampler0, texCoord);
    float mask = center.a;
    vec2 texel = EspParams.xy;
    float width = EspParams.z;
    int mode = int(EspParams.w + 0.5);

    // early out: most pixels are nowhere near a silhouette — probe 8 points before
    // paying for the full dilation loop (anything a probe can miss is too faint to see)
    if (mask <= 0.0) {
        vec2 reach = texel * width;
        vec2 half_ = reach * 0.55;
        float probe =
              texture(Sampler0, texCoord + vec2(reach.x, 0.0)).a
            + texture(Sampler0, texCoord - vec2(reach.x, 0.0)).a
            + texture(Sampler0, texCoord + vec2(0.0, reach.y)).a
            + texture(Sampler0, texCoord - vec2(0.0, reach.y)).a
            + texture(Sampler0, texCoord + half_).a
            + texture(Sampler0, texCoord - half_).a
            + texture(Sampler0, texCoord + vec2(half_.x, -half_.y)).a
            + texture(Sampler0, texCoord + vec2(-half_.x, half_.y)).a;
        if (probe <= 0.0) {
            discard;
        }
    }

    // soft dilation of the silhouette mask: 3 rings x 12 taps, staggered angles;
    // the mask RGB carries each entity's own color (friends green), accumulate it too
    float blur = mask;
    float total = 1.0;
    vec3 tint = center.rgb * center.a;
    for (int ring = 1; ring <= 3; ring++) {
        float ringRadius = width * float(ring) / 3.0;
        float weight = 1.0 - (float(ring) - 1.0) * 0.28;
        for (int i = 0; i < 12; i++) {
            float angle = (float(i) + float(ring) * 0.5) * 0.5235987756;
            vec2 offset = vec2(cos(angle), sin(angle)) * ringRadius * texel;
            vec4 tap = texture(Sampler0, texCoord + offset);
            blur += tap.a * weight;
            tint += tap.rgb * tap.a * weight;
            total += weight;
        }
    }
    // dominant silhouette color reaching this pixel (inside the mask it is its own color)
    vec3 baseColor = tint / max(blur, 1.0e-4);
    blur /= total;

    // glowing rim hugging the silhouette edge from both sides, bleeding outward
    float outer = clamp(blur * 2.3, 0.0, 1.0) * (1.0 - mask);
    float inner = clamp((1.0 - blur) * 2.3, 0.0, 1.0) * mask;
    float outline = max(outer, inner * 0.8);

    float fill = mask * EspParams2.x;

    float intensity;
    if (mode == 0) {
        intensity = fill;
    } else if (mode == 1) {
        intensity = outline;
    } else {
        intensity = min(fill + outline, 1.0);
    }

    if (intensity <= 0.003) {
        discard;
    }

    // flowing diagonal gradient across the fill; the outline keeps the entity color
    float timeValue = EspParams2.y;
    float aspect = EspParams.y / max(EspParams.x, 1.0e-6);
    vec2 flowUv = vec2(texCoord.x * aspect, texCoord.y);
    float flow = 0.5 + 0.5 * sin(flowUv.x * 6.0 + flowUv.y * 4.5 + timeValue * 1.7);
    flow = 0.65 * flow + 0.35 * (0.5 + 0.5 * sin(flowUv.y * 8.0 - flowUv.x * 3.0 - timeValue * 1.1));
    vec3 fillColor = mix(baseColor, vec3(1.0), 0.6 * flow);

    vec3 rgb = mix(baseColor, fillColor, mode == 1 ? 0.0 : mask);
    fragColor = vec4(rgb, intensity * EspColor.a);
}
