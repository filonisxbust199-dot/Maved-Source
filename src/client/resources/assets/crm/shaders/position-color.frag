#version auto

precision lowp float;

#include<shaderColor>

#inputs {
    vec4 vertexColor;
};

out vec4 fragColor;

void main() {
    fragColor = shaderColor * vertexColor;
}
