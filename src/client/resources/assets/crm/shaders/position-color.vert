#version auto

#inputs {
    vec4 pos;
    vec4 color;
};

#include<matrices>

#outputs {
    vec4 vertexColor;
};

void main() {
    gl_Position = projMat * modelViewMat * pos;

    vertexColor = color;
}
