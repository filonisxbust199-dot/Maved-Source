#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float textureAlpha = texture(Sampler0, texCoord0).a;
    if (textureAlpha < 0.1) discard;

    // Preserve the pre-26.2 Flat face-lighting orientation and ignore skin RGB.
    vec4 faceColor = gl_FrontFacing ? vertexPerFaceColorBack : vertexPerFaceColorFront;
    vec4 color = vec4(faceColor.rgb, faceColor.a * textureAlpha) * ColorModulator;
    // Chams supplies NO_OVERLAY; damage indication is already part of the model tint.
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance,
            FogEnvironmentalStart, FogEnvironmentalEnd,
            FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
