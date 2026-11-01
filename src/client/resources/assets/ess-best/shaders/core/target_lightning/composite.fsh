#version 330

uniform sampler2D Sampler0; // размытый слой пушинок (premultiplied: капча блендилась в чёрный)

in vec2 texCoord0;
layout(std140) uniform LightningSettings {
    vec4 Settings;
};

out vec4 fragColor;

// Композит пушинок поверх кадра (premultiplied-over, ONE / ONE_MINUS_SRC_ALPHA):
// видно ТОЛЬКО bloom — мягкое размытое пятно, цвет усиливается сверх альфы и
// складывается с фоном как свечение; резкая фигура в кадр не попадает.
void main() {
    fragColor = vec4(0.0);
    vec4 blurred = texture(Sampler0, texCoord0);
    float alpha = min(blurred.a * 1.25, 1.0);
    if (alpha <= 0.001) discard;
    fragColor = vec4(blurred.rgb * (1.0 + Settings.w * 3.0), alpha);
}
