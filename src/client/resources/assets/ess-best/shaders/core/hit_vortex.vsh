#version 150

// Up to 24 hit-vortex quads in one draw: gl_VertexID picks the marker and the corner,
// the UBO carries per-marker screen rect, spin angle and color. Screen = (w, h, _, count).
layout(std140) uniform HitVortexData {
    vec4 Screen;
    vec4 Marker[24]; // cx, cy, half-size (px), spin angle (rad)
    vec4 Color[24];  // rgb, fade alpha
};

out vec2 localUV;
flat out float vAng;
flat out vec4 vColor;

void main() {
    int id = gl_VertexID / 6;
    int corner = gl_VertexID % 6;
    vec2 uv = vec2(
        (corner == 1 || corner == 2 || corner == 4) ? 1.0 : 0.0,
        (corner == 2 || corner == 4 || corner == 5) ? 1.0 : 0.0
    );

    vec4 marker = Marker[id];
    vec2 pixel = marker.xy + (uv * 2.0 - 1.0) * marker.z;
    gl_Position = vec4(pixel.x / Screen.x * 2.0 - 1.0, 1.0 - pixel.y / Screen.y * 2.0, 0.0, 1.0);

    localUV = uv;
    vAng = marker.w;
    vColor = Color[id];
}
