#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform LightningLine {
    mat4 MvpMat;
    vec4 ColorModulator;
    vec2 ScreenSize;
    vec2 Padding;
};

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec3 Normal;
layout(location = 3) in float LineWidth;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out float edgePx;
layout(location = 2) out float halfPx;

const float FEATHER = 1.0;
const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);

void main() {
    vec4 startClip = MvpMat * vec4(Position * VIEW_SHRINK, 1.0);
    vec4 endClip = MvpMat * vec4((Position + Normal) * VIEW_SHRINK, 1.0);

    vec3 ndcStart = startClip.xyz / startClip.w;
    vec3 ndcEnd = endClip.xyz / endClip.w;
    vec2 screenDirection = (ndcEnd.xy - ndcStart.xy) * ScreenSize;
    float screenLength = length(screenDirection);
    vec2 direction = screenLength > 0.00001
        ? screenDirection / screenLength
        : vec2(1.0, 0.0);
    vec2 perpendicular = vec2(-direction.y, direction.x);

    halfPx = LineWidth * 0.5;
    float extentPx = halfPx + FEATHER;
    vec2 offset = perpendicular * (2.0 * extentPx) / ScreenSize;
    if (offset.x < 0.0) {
        offset *= -1.0;
    }

    float side = (gl_VertexIndex % 2 == 0) ? 1.0 : -1.0;
    edgePx = side * extentPx;
    vec3 ndc = ndcStart + side * vec3(offset, 0.0);
    gl_Position = vec4(ndc * startClip.w, startClip.w);
    vertexColor = Color * ColorModulator;
}

