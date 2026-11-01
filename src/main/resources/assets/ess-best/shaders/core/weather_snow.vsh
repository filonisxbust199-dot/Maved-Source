#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(std140) uniform SnowData {
    mat4 InverseView;
    vec4 Origin4;
    vec4 SunDir4;
    vec4 SunColor4;
    vec4 SkyColor4;
    vec4 Snow0;     // GrainScale, WrapPeriod, GrainStrength, CrustAmount
    vec4 Snow1;     // SparkleAmount, Wetness, Sheen
};
#define Origin Origin4.xyz
#define SunDir SunDir4.xyz
#define SunColor SunColor4.xyz
#define SkyColor SkyColor4.xyz
#define GrainScale Snow0.x
#define WrapPeriod Snow0.y
#define GrainStrength Snow0.z
#define CrustAmount Snow0.w
#define SparkleAmount Snow1.x
#define Wetness Snow1.y
#define Sheen Snow1.z



layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;

layout(location = 0) out vec3 WorldPos;
layout(location = 1) out vec3 ViewPos;
layout(location = 2) out vec4 FragColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vec3 relative = (InverseView * vec4(Position, 1.0)).xyz;
    WorldPos = relative + Origin;
    ViewPos = relative;
    FragColor = Color;
}
