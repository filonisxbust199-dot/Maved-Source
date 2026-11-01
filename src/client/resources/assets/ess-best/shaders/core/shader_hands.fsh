#version 150
#moj_import <ess-best:hands_item_color.glsl>

// Аддитивный композит свечения + шлейфа (порт hands_glow.fsh + trail-composite).
// Пайплайн: blend (ONE, ONE), запись только RGB — выход премультиплицированный свет.
uniform sampler2D BloomSampler;  // результат kawase-пирамиды
uniform sampler2D HandsSampler;  // силуэт рук (a) — вырезает свечение изнутри
uniform sampler2D TrailSampler;  // накопленный шлейф (premultiplied)

layout(std140) uniform HandsSettings {
    vec4 HandsColor;   // rgb: цвет темы 1, w: exposure (glow*1.8 + outline*1.35)
    vec4 HandsColor2;  // rgb: цвет темы 2, w: use item color
    vec4 HandsParams;  // x: trail on/off
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 bloom = texture(BloomSampler, texCoord);
    float mask = texture(HandsSampler, texCoord).a;

    // свечение: размытая альфа снаружи силуэта, вертикальный градиент темы либо цвет предмета.
    // Reference coverage: do not amplify barely covered texels with sqrt.
    float softBloom = clamp(bloom.a, 0.0, 1.0);
    float a = clamp(softBloom * (1.0 - mask) * HandsColor.w, 0.0, 1.0);
    vec3 grad = mix(HandsColor.rgb, HandsColor2.rgb, texCoord.y);
    vec3 glowColor = grad;
    float itemMix = clamp(HandsColor2.w, 0.0, 1.0);
    if (itemMix > 0.001) {
        vec3 itemColor = bloom.a > 0.001 ? handsBloomColor(bloom) : grad;
        glowColor = mix(grad, itemColor, itemMix);
    }
    vec3 add = glowColor * a;

    // шлейф: premultiplied, добавляется вне силуэта (руки поверх шлейфа)
    if (HandsParams.x > 0.5) {
        vec4 trail = texture(TrailSampler, texCoord);
        add += trail.rgb * (1.0 - mask);
    }

    if (dot(add, add) <= 1.0e-6) {
        discard;
    }
    fragColor = vec4(add, 0.0);
}
