#version 330
#extension GL_ARB_separate_shader_objects : require

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

layout(location = 0) out vec3 skyDirection;

void main() {
    vec2 positions[6] = vec2[](
        vec2(-1.0, -1.0),
        vec2( 1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0, -1.0),
        vec2( 1.0,  1.0),
        vec2(-1.0,  1.0)
    );

    vec2 clipPosition = positions[gl_VertexIndex];
    vec4 clipRay = vec4(clipPosition, 1.0, 1.0);
    vec4 viewRay = inverse(ProjMat) * clipRay;
    vec3 viewDirection = normalize(viewRay.xyz / viewRay.w);
    skyDirection = normalize(transpose(mat3(ModelViewMat)) * viewDirection);

    gl_Position = vec4(clipPosition, 0.0, 1.0);
}
