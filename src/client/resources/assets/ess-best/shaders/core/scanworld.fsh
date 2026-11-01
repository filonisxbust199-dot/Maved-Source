#version 150
#moj_import <ess-best:depth.glsl>

// ScanWorld — expanding spherical scan band reconstructed from scene depth (Nightix port).
uniform sampler2D DepthSampler;

layout(std140) uniform ScanData {
    mat4 InvViewProj;   // inverse(projection * modelView) — NDC -> camera-relative world
    vec4 Center;        // xyz = camera-relative origin, w = current radius
    vec4 OuterColor;
    vec4 MidColor;
    vec4 InnerColor;
    vec4 ScanlineColor;
    vec4 Params;        // x = band width, y = fade, z = time, w = shimmer amount
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float d = texture(DepthSampler, texCoord).r;
    if (d <= 0.000001) discard;

    vec4 ndc = vec4(texCoord * 2.0 - 1.0, essDepthToNdc(d), 1.0);
    vec4 wp = InvViewProj * ndc;
    vec3 world = wp.xyz / wp.w;

    float radius = Center.w;
    float band = max(Params.x, 0.5);
    float fade = clamp(Params.y, 0.0, 1.0);
    float t = Params.z;

    float dist = length(world - Center.xyz);
    float delta = dist - radius;
    float absD = abs(delta);

    // soft band around the expanding front
    float outer = 1.0 - smoothstep(0.0, band * 1.35, absD);
    float mid   = 1.0 - smoothstep(0.0, band * 0.75, absD);
    float inner = 1.0 - smoothstep(0.0, band * 0.28, absD);
    // thin scanline slightly ahead of the front
    float line  = 1.0 - smoothstep(0.0, band * 0.12, abs(delta - band * 0.15));
    line = pow(line, 2.2);

    // gentle vertical shimmer on the ring
    float shimmerWave = 0.85 + 0.15 * sin(world.y * 3.5 + t * 6.0 + dist * 0.08);
    float shimmer = mix(1.0, shimmerWave, clamp(Params.w, 0.0, 1.0));

    vec3 rgb = OuterColor.rgb * outer * OuterColor.a
             + MidColor.rgb   * mid   * MidColor.a
             + InnerColor.rgb * inner * InnerColor.a
             + ScanlineColor.rgb * line * ScanlineColor.a;
    float a = (outer * OuterColor.a + mid * MidColor.a + inner * InnerColor.a + line * ScanlineColor.a)
            * fade * shimmer;

    if (a <= 0.004) discard;
    fragColor = vec4(rgb, clamp(a, 0.0, 1.0));
}
