#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform ChamsBlur { vec4 Params; };
uniform sampler2D SceneSampler;
uniform sampler2D MaskSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
const float GOLDEN=2.39996323;
void main() {
    vec4 mask=texture(MaskSampler,texCoord);
    if(mask.a<0.001) discard;
    vec2 texel=1.0/vec2(textureSize(SceneSampler,0));
    vec2 sampleUv=Params.y>0.5?vec2(1.0-texCoord.x,texCoord.y):texCoord;
    vec3 blurred=vec3(0.0);
    float total=0.0;
    for(int i=0;i<16;i++) {
        float t=(float(i)+0.5)/16.0;
        float weight=exp(-t*1.8);
        float distance=sqrt(t)*max(0.5,Params.x);
        vec2 offset=vec2(cos(float(i)*GOLDEN),sin(float(i)*GOLDEN))*texel*distance;
        blurred+=texture(SceneSampler,clamp(sampleUv+offset,vec2(0.0),vec2(1.0))).rgb*weight;
        total+=weight;
    }
    blurred/=max(total,0.0001);
    float luminance=dot(blurred,vec3(0.2126,0.7152,0.0722));
    vec3 tinted=mask.rgb*(0.32+luminance*0.92)+blurred*mask.rgb*0.16;
    vec3 glass=mix(blurred,tinted,clamp(Params.z,0.0,1.0));
    vec3 original=texture(SceneSampler,texCoord).rgb;
    fragColor=vec4(mix(original,glass,smoothstep(0.0,0.82,mask.a)),1.0);
}
