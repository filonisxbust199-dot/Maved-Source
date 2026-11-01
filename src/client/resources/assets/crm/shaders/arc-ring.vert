#version auto

#inputs {
    vec4 pos;
    vec4 color;
    vec2 rectPosition;
    vec2 _ring;
    vec2 _rot;
    vec2 _cap;
};

#include<matrices>

#outputs {
    vec4 vertexColor;
    vec2 offset;
    vec2 ring;
    vec2 rot;
    vec2 cap;
};

void main() {
    gl_Position = projMat * modelViewMat * pos;
    vertexColor = color;
    offset = pos.xy - rectPosition;
    ring = _ring;
    rot = _rot;
    cap = _cap;
}
