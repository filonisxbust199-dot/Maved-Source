#version 330
#extension GL_ARB_separate_shader_objects : require

#include <dimasik:weather_frame.glsl>



// Единое небо погоды. Раньше на каждый режим был свой фрагментник со своими вшитыми
// константами: восемь файлов, в которых заново повторялись градиент, звёзды и облака, и любая
// правка общего приёма требовала восьми одинаковых правок. Здесь приём один, а режим — это
// набор значений, который присылает Java: палитра, покрытие облаков, доля звёзд, сияния,
// метеоров, дымки и вспышка молнии. Добавить погоду теперь значит дописать константу в
// WeatherPreset, а не завести девятый шейдер.
//
// Все ветвления идут по uniform-ам, то есть одинаковы для всего кадра: варп не расходится, и
// выключенный слой стоит ровно одно сравнение, а не выполненный впустую цикл.

#include <dimasik:weather_sky_color.glsl>

// Кадр под небом: в мире это то, что уже нарисовано, в карточке — ничего.
uniform sampler2D InSampler;

// Больше нуля — небо гоняют в карточке меню. Там кадра под нами нет, и подмешивать нечего:
// небо кладётся альфой поверх подложки карточки, а Opacity гасит его вместе с её выездом.

layout(location = 0) in vec2 TexCoord;
#define texCoord TexCoord

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec3 viewDirection = normalize(vec3(ndc.x * Aspect * TanHalfFov, ndc.y * TanHalfFov, -1.0));
    vec3 skyDirection = getSkyDirection(viewDirection);
    vec3 shaderColor = weatherSky(skyDirection);
    float amount = clamp(max(Intensity, 0.0), 0.0, 1.0);
    float blend = fullSkyAmount(skyDirection.y, amount);

    if (Opacity > 0.0) {
        fragColor = vec4(shaderColor, blend * Opacity);
    } else {
        vec4 source = texture(InSampler, texCoord);
        fragColor = vec4(mix(source.rgb, shaderColor, blend), source.a);
    }
}
