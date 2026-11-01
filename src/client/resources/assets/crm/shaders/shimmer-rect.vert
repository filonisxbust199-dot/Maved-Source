#version auto

#inputs {
    vec4 pos;
    vec4 color;
    vec2 rectPosition;
    vec2 _halfSize;
    float _radius;
    vec4 color2;
    vec2 _flow;
};

#include<matrices>

#outputs {
    vec4 vertexColor;
    vec4 vertexColor2;
    vec2 offset;
    vec2 halfSize;
    float radius;
    vec2 flow;
};

void main() {
    gl_Position = projMat * modelViewMat * pos;
    vertexColor = color;
    vertexColor2 = color2;
    offset = pos.xy - rectPosition;
    halfSize = _halfSize;
    radius = _radius;
    flow = _flow;
}
