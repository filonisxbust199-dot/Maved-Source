#version auto

#inputs {
    vec4 pos;
    vec2 local;
};

#include<matrices>

#outputs {
    vec2 point;
};

void main() {
    gl_Position = projMat * modelViewMat * pos;
    point = local;
}
