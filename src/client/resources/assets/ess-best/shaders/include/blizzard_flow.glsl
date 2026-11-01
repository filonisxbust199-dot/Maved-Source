// World-anchored, softly overlapping gusts. settings.z = adaptive, .w = wind speed.
vec4 blizzardFlow(vec3 p, float time, vec2 wind, vec4 settings) {
    if (settings.z < 0.5) return vec4(wind.x, 0.0, wind.y, 1.0);
    const float spacing = 32.0;
    vec2 grid = floor(p.xz / spacing - 0.5);
    vec3 variation = vec3(0.0);
    float coverage = 0.0;
    for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
        vec2 cell = grid + vec2(float(x), float(z));
        vec2 id = mod(cell, 128.0); // Same 4096-block period as the renderer.
        vec3 hash = fract(vec3(id.xyx) * 0.1031);
        hash += dot(hash, hash.yzx + 33.33);
        float seed = fract((hash.x + hash.y) * hash.z);
        float phase = seed * 6.2831853;
        vec2 offset = p.xz - (cell + 0.5) * spacing;
        // Support vanishes before a cell leaves the four-neighbor stencil: no boundary jumps.
        float weight = 1.0 - smoothstep(0.0, 31.0, length(offset));
        weight *= 0.65 + 0.35 * sin(time * (0.07 + seed * 0.04) + phase);
        float turn = sin(time * (0.06 + seed * 0.05) + phase) * 0.65;
        vec2 rotatedWind = mat2(cos(turn), -sin(turn), sin(turn), cos(turn)) * wind;
        vec2 tangent = vec2(-offset.y, offset.x) / sqrt(dot(offset, offset) + 16.0);
        vec2 localWind = rotatedWind + tangent * (seed < 0.5 ? -1.0 : 1.0) * settings.w * 0.22;
        variation += vec3(localWind.x - wind.x,
                sin(time * 0.35 + phase) * settings.w * 0.04, localWind.y - wind.y) * weight;
        coverage += weight;
    }
    vec3 velocity = vec3(wind.x, 0.0, wind.y) + variation / max(coverage, 1.0);
    return vec4(velocity, 0.65 + 0.35 * clamp(coverage, 0.0, 1.0));
}

// Conservative sphere against side planes and the camera plane, including full sheet bounds.
bool blizzardOutsideView(vec3 center, float radius, mat4 view, mat4 projection) {
    vec3 p = (view * vec4(center, 1.0)).xyz;
    if (p.z > radius) return true;
    vec4 clip = projection * vec4(p, 1.0);
    vec3 rowW = vec3(projection[0][3], projection[1][3], projection[2][3]);
    vec3 rowX = vec3(projection[0][0], projection[1][0], projection[2][0]);
    vec3 rowY = vec3(projection[0][1], projection[1][1], projection[2][1]);
    return clip.x + clip.w < -radius * length(rowX + rowW)
        || clip.w - clip.x < -radius * length(rowW - rowX)
        || clip.y + clip.w < -radius * length(rowY + rowW)
        || clip.w - clip.y < -radius * length(rowW - rowY);
}
