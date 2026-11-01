#version auto

#include<shaderColor>

#uniforms {
    sampler2D u_Texture; // solid silhouette mask: rgb = per-entity colour, a = coverage
    vec4 EspColor;       // a = master intensity (rgb unused, the mask carries the hue)
    vec2 texelSize;      // 1/width, 1/height of the mask
    float glowWidth;     // dilation radius in px
    float mode;          // 0 fill, 1 outline, 2 fill+outline
    float fillOpacity;
    float time;
};

out vec4 fragColor;

void main() {
    // gl_FragCoord and the outline framebuffer share the bottom-left origin, so no y-flip
    vec2 texCoord = gl_FragCoord.xy * texelSize;
    vec4 center = texture(u_Texture, texCoord);
    float mask = center.a;
    vec2 texel = texelSize;
    float width = glowWidth;
    int espMode = int(mode + 0.5);

    // early out: most pixels are nowhere near a silhouette — probe 8 points before
    // paying for the full dilation loop
    if (mask <= 0.0) {
        vec2 reach = texel * width;
        vec2 diag = reach * 0.55;
        float probe =
              texture(u_Texture, texCoord + vec2(reach.x, 0.0)).a
            + texture(u_Texture, texCoord - vec2(reach.x, 0.0)).a
            + texture(u_Texture, texCoord + vec2(0.0, reach.y)).a
            + texture(u_Texture, texCoord - vec2(0.0, reach.y)).a
            + texture(u_Texture, texCoord + diag).a
            + texture(u_Texture, texCoord - diag).a
            + texture(u_Texture, texCoord + vec2(diag.x, -diag.y)).a
            + texture(u_Texture, texCoord + vec2(-diag.x, diag.y)).a;
        if (probe <= 0.0) {
            discard;
        }
    }

    // soft dilation of the silhouette: 3 rings x 12 taps, accumulating both coverage and
    // the mask's own colour (friends green) so the dominant entity hue reaches this pixel
    float blur = mask;
    float total = 1.0;
    vec3 tint = center.rgb * center.a;
    for (int ring = 1; ring <= 3; ring++) {
        float ringRadius = width * float(ring) / 3.0;
        float weight = 1.0 - (float(ring) - 1.0) * 0.28;
        for (int i = 0; i < 12; i++) {
            float angle = (float(i) + float(ring) * 0.5) * 0.5235987756;
            vec2 offset = vec2(cos(angle), sin(angle)) * ringRadius * texel;
            vec4 tap = texture(u_Texture, texCoord + offset);
            blur += tap.a * weight;
            tint += tap.rgb * tap.a * weight;
            total += weight;
        }
    }
    vec3 baseColor = tint / max(blur, 1.0e-4);
    blur /= total;

    // crisp outline LINE hugging the silhouette: a thin band right outside the solid edge.
    // sample the immediate neighbourhood (scaled a touch by glowWidth) — if any neighbour is
    // solid while we are not, we sit on the contour.
    float lineW = max(1.0, width * 0.35);
    vec2 e = texel * lineW;
    float edge = 0.0;
    edge = max(edge, texture(u_Texture, texCoord + vec2(e.x, 0.0)).a);
    edge = max(edge, texture(u_Texture, texCoord - vec2(e.x, 0.0)).a);
    edge = max(edge, texture(u_Texture, texCoord + vec2(0.0, e.y)).a);
    edge = max(edge, texture(u_Texture, texCoord - vec2(0.0, e.y)).a);
    edge = max(edge, texture(u_Texture, texCoord + e).a);
    edge = max(edge, texture(u_Texture, texCoord - e).a);
    edge = max(edge, texture(u_Texture, texCoord + vec2(e.x, -e.y)).a);
    edge = max(edge, texture(u_Texture, texCoord + vec2(-e.x, e.y)).a);
    float line = edge * (1.0 - mask);                                  // bright contour line
    float glow = pow(clamp(blur, 0.0, 1.0) * (1.0 - mask), 1.6);       // colored halo fading out
    float outlineI = clamp(line + glow * 0.85, 0.0, 1.0);
    // the line runs a little hot (toward white) so it reads as a bright neon edge; the glow
    // halo keeps the pure entity colour so friends still glow green etc.
    vec3 lineColor = mix(baseColor, vec3(1.0), 0.35);
    vec3 outlineColor = mix(baseColor, lineColor, line);

    // clean volumetric fill: the entity's own hue with only a gentle same-colour sheen — no
    // white streaks, so it reads as a smooth translucent chams like the reference client
    float aspect = texel.y / max(texel.x, 1.0e-6);
    vec2 flowUv = vec2(texCoord.x * aspect, texCoord.y);
    float flow = 0.5 + 0.5 * sin(flowUv.x * 5.0 + flowUv.y * 4.0 + time * 1.3);
    vec3 fillColor = baseColor * (0.85 + 0.3 * flow);
    float fill = mask * fillOpacity;

    // soft bloom edge so the filled body glows outward with a soft halo (not a hard line)
    float softGlow = pow(clamp(blur, 0.0, 1.0) * (1.0 - mask), 1.8);

    float intensity;
    vec3 rgb;
    if (espMode == 0) {
        // fill + soft glow edge, all in the entity colour
        intensity = clamp(fill + softGlow * 0.6, 0.0, 1.0);
        rgb = fillColor;
    } else if (espMode == 1) {
        // crisp bright contour line + glow
        intensity = outlineI;
        rgb = outlineColor;
    } else {
        // clean fill under a bright contour line
        intensity = clamp(fill + outlineI, 0.0, 1.0);
        rgb = mix(fillColor, outlineColor, clamp(outlineI / max(fill + outlineI, 1.0e-4), 0.0, 1.0));
    }

    if (intensity <= 0.003) {
        discard;
    }

    fragColor = vec4(rgb, intensity * EspColor.a) * shaderColor;
}
