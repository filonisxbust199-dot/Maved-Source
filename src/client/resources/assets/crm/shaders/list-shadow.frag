#version auto

#include<shaderColor>

#define MAX_ROWS 128
uniform float Rows[512];
#uniforms {
    int RowCount;
    vec4 Shape;
    vec4 Color;
    vec4 ColorHigh;
    vec4 Wave;
};

#inputs {
    vec2 point;
};

out vec4 fragColor;

vec2 rowMask(int i) {
    int base = i * 4;
    float width = Rows[base];
    float left = Shape.z > 0.5 ? 0.0 : Shape.x - width;
    vec2 outside = max(max(vec2(left, Rows[base + 1]) - point,
                          point - vec2(left + width, Rows[base + 2])), vec2(0.0));
    float distance = length(outside);
    // A broad penumbra retains visible density, while the smaller contact lobe gives the
    // steps definition. Both end with zero slope rather than a clipped/banded outer edge.
    float wide = 1.0 - smoothstep(0.0, Shape.w, distance);
    float contact = 1.0 - smoothstep(0.0, Shape.w * 0.30, distance);
    return vec2(wide, contact * contact) * Rows[base + 3];
}

void main() {
    // Find the first row whose bottom can reach this pixel. Only neighboring rows within
    // the penumbra need evaluating, even for a long list. Collapsing rows remain supported.
    int lo = 0;
    int hi = RowCount;
    for (int search = 0; search < 8; search++) {
        if (lo >= hi) break;
        int mid = (lo + hi) / 2;
        if (Rows[mid * 4 + 2] < point.y - Shape.w) lo = mid + 1;
        else hi = mid;
    }
    vec2 mask = vec2(0.0);
    for (int i = lo; i < MAX_ROWS; i++) {
        if (i >= RowCount || Rows[i * 4 + 1] > point.y + Shape.w) break;
        mask = max(mask, rowMask(i));
        if (all(greaterThanEqual(mask, vec2(1.0)))) break;
    }
    // Fixed eight-row wavelength and screen-aligned phase, shared with names and accent.
    // Undo the cast offset for color only so the glow does not trail the text's animation.
    float across = clamp(point.x / max(Shape.x, 1.0), 0.0, 1.0);
    float position = (point.y + Wave.w) / Wave.y + across * Wave.z - Wave.x;
    float blend = 0.5 - 0.5 * cos(position * 6.283185307179586);
    vec3 waveColor = mix(Color.rgb, ColorHigh.rgb, blend);
    // Darker contact shading gives the broad colored glow depth without another draw pass.
    vec3 shaded = waveColor * mix(1.0, 0.78, mask.y);
    float opacity = dot(mask, vec2(0.58, 0.42)) * Color.a;
    fragColor = vec4(shaded, opacity) * shaderColor;
}
