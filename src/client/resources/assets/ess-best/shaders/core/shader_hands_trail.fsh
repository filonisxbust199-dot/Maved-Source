#version 150
#moj_import <ess-best:hands_item_color.glsl>

// Трейл-пасс: порт hands_trail_displace.fsh + hands_trail_glow.fsh + «предмет в шлейфе»,
// слитые в один replace-проход. Ключевое из оригинала: вычитательное затухание
// (oldA*0.985*flick - fade) — хвост гарантированно доходит до нуля, и прозрачная рамка
// при сэмплировании (иначе нижний край экрана размазывается вечной колонной вверх).
uniform sampler2D PrevSampler;   // трейл прошлого кадра
uniform sampler2D BloomSampler;  // kawase-блум текущего кадра
uniform sampler2D HandsSampler;  // силуэт рук (a) + их цвет (rgb)

layout(std140) uniform TrailSettings {
    vec4 TrailParams;  // x: sway uv, y: rise uv, z: fade, w: t (time)
    vec4 TrailParams2; // x: dt, y: turbulence, z: flicker, w: trail exposure
    vec4 TrailColor;   // rgb: цвет темы 1, w: use item color
    vec4 TrailColor2;  // rgb: цвет темы 2, w: item-in-trail opacity (0 = выкл)
};

in vec2 texCoord;
out vec4 fragColor;

float wob(float y, float tt) {
    return sin(y * 9.0 + tt * 4.3) * 0.6
         + sin(y * 17.0 - tt * 2.1) * 0.3
         + sin(y * 4.0 + tt * 1.3) * 0.4
         + sin(y * 28.0 + tt * 5.7) * 0.15;
}

vec4 tapPrev(vec2 p) {
    if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) return vec4(0.0);
    return texture(PrevSampler, p);
}

vec4 blurSample(vec2 p, vec2 px) {
    vec4 c = tapPrev(p) * 0.36;
    c += tapPrev(p + vec2(px.x, 0.0)) * 0.16;
    c += tapPrev(p + vec2(-px.x, 0.0)) * 0.16;
    c += tapPrev(p + vec2(0.0, px.y)) * 0.16;
    c += tapPrev(p + vec2(0.0, -px.y)) * 0.16;
    return c;
}

void main() {
    float t = TrailParams.w;
    float dt = TrailParams2.x;

    // displace: подъём (быстрее к верху экрана) + качание + многосинусная турбулентность
    float dxTurb = (wob(texCoord.y, t) - wob(texCoord.y, t - dt)) * TrailParams2.y;
    float yFactor = 0.7 + texCoord.y * 0.8;
    vec2 disp = vec2(TrailParams.x + dxTurb, TrailParams.y * yFactor);
    vec2 px = 1.0 / vec2(textureSize(PrevSampler, 0));
    vec4 c = blurSample(texCoord - disp, px);

    float flick = 1.0 - TrailParams2.z + TrailParams2.z * (0.5 + 0.5 * sin(t * 14.0) + 0.25 * sin(t * 9.3));
    float oldA = c.a;
    float newA = max(0.0, oldA * (0.985 * flick) - TrailParams.z);
    float scale = oldA > 1e-5 ? newA / oldA : 0.0;
    vec4 trail = vec4(c.rgb * scale, newA);

    // впрыск размытого свечения (hands_trail_glow): premultiplied-over поверх смещённого.
    // sqrt — коррекция Ghost под RGBA8 (см. shader_hands.fsh).
    vec4 bloom = texture(BloomSampler, texCoord);
    float mask = texture(HandsSampler, texCoord).a;
    float ia = clamp(sqrt(clamp(bloom.a, 0.0, 1.0)) * (1.0 - mask) * TrailParams2.w, 0.0, 1.0);
    if (ia > 0.0) {
        vec3 grad = mix(TrailColor.rgb, TrailColor2.rgb, texCoord.y);
        vec3 glowColor = grad;
        float itemMix = clamp(TrailColor.w, 0.0, 1.0);
        if (itemMix > 0.001) {
            vec3 itemColor = bloom.a > 0.001 ? handsBloomColor(bloom) : grad;
            glowColor = mix(grad, itemColor, itemMix);
        }
        vec4 inject = vec4(glowColor * ia, ia);
        trail = inject + trail * (1.0 - inject.a);
    }

    // «предмет в шлейфе»: сами руки/предмет поверх (обычный альфа-овер)
    float itemOpacity = TrailColor2.w;
    if (itemOpacity > 0.001) {
        vec4 hands = texture(HandsSampler, texCoord);
        float ha = hands.a * itemOpacity;
        if (ha > 0.0) {
            vec3 handsColor = hands.a > 0.001 ? hands.rgb / hands.a : vec3(0.0);
            trail.rgb = handsColor * ha + trail.rgb * (1.0 - ha);
            trail.a = ha + trail.a * (1.0 - ha);
        }
    }

    fragColor = trail;
}
