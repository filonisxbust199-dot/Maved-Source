#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform TargetScan {
    mat4 InvMvp;
    vec4 Centre;    // xyz — цель относительно камеры, w — радиус объёма
    vec4 Band;      // x — высота полосы, y — знак хода, z — скорость, w — досягаемость сетки
    vec4 Tint;      // первый цвет полосы
    vec4 Tint2;     // второй цвет полосы
    vec4 Shape;     // x — рост цели, y — прозрачность, z — запас к габариту
    vec4 Phase;     // синус и косинус обеих временных фаз волны
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

const float BAND_HALF = 0.02;
const float WAVE_AMP = 0.03;
const float TRAIL_LEN = 0.5;
const float TRAIL_STRENGTH = 0.5;
const float GLOW_LEN = 0.12;
const float GLOW_STRENGTH = 0.6;
const float AMBIENT_LEN = 0.55;
const float AMBIENT_STRENGTH = 0.05;
const float HOT_MIX = 0.7;
const float GRID_CELL = 0.25;
const float GRID_STRENGTH = 0.55;
// Ниже этого вклада пиксель на кадре неразличим.
const float MIN_CONTRIBUTION = 0.004;
// Дальше этого расстояния от полосы вклад заведомо ниже порога: самый пологий
// член — общий подсвет, и AMBIENT_STRENGTH * exp(-MAX_REACH / AMBIENT_LEN) уже
// меньше MIN_CONTRIBUTION. Ядро, свечение и шлейф спадают быстрее.
const float MAX_REACH = 1.4;
// Наибольшее отклонение волны: |sin(a) + 0.45 * sin(b)| не превышает 1.45.
const float WAVE_LIMIT = WAVE_AMP * 1.45;
const float FAR_DEPTH = 1.0e-6;
// Бит владельца пикселя в альфа-канале буфера обводки: его ставит только пометка
// цели, см. OutlineMask. Смешивания у прохода обводки нет, поэтому байт доходит
// сюда целым и сравнивается точно.
const int SCAN_MARK = 8;

/** Попадает ли точка в объём цели: габарит плюс запас на оружие и крылья. */
bool withinTarget(vec3 local) {
    return dot(local.xz, local.xz) <= Centre.w * Centre.w
            && local.y >= -Shape.z && local.y <= Shape.x + Shape.z;
}

void main() {
    // Силуэт приходит маской из буфера обводки: там цель отрисована целиком — с
    // оружием, плащом и крыльями, — а блок туда попасть не может. Отбор идёт по
    // пометке, поэтому подсветка других сущностей от EntityESP в маску не входит.
    int owner = int(texture(MaskSampler, texCoord).a * 255.0 + 0.5);
    if ((owner & SCAN_MARK) == 0) {
        discard;
    }

    // Глубина сцены нужна, чтобы узнать мировую точку пикселя.
    float rawDepth = texture(DepthSampler, texCoord).r;
    if (rawDepth <= FAR_DEPTH) {
        discard;
    }

    vec4 clip = vec4(texCoord * 2.0 - 1.0, rawDepth, 1.0);
    vec4 world = InvMvp * clip;
    vec3 pos = world.xyz / world.w;

    // Пиксель уже точно принадлежит цели, объём проверяет только глубину: часть
    // геометрии предметов уходит в отдельный буфер, и в основном буфере на их
    // месте остаётся фон. Такую точку сносим по тому же лучу зрения на дальность
    // цели — фаза полосы для неё получается верной, а дыры не остаётся.
    vec3 local = pos - Centre.xyz;
    if (!withinTarget(local)) {
        pos = pos / max(length(pos.xz), 1.0e-4) * length(Centre.xz);
        local = pos - Centre.xyz;
        if (!withinTarget(local)) {
            discard;
        }
    }
    // Грубый отсев до волны: она смещает полосу не больше чем на WAVE_LIMIT,
    // поэтому всё, что дальше этого запаса, не спасёт никакая её фаза. Так
    // арктангенс и синусы считаются только у самой полосы.
    float reach = max(MAX_REACH, Band.w);
    if (abs(pos.y - Band.x) > reach + WAVE_LIMIT) {
        discard;
    }

    // Волна без арктангенса и синусов: синус и косинус угла берутся прямо из
    // направления, кратные углы раскрываются тождествами, а временные фазы
    // приходят готовыми. Значение получается то же, что у sin(3t + a) и
    // sin(5t - b), но втрое дешевле.
    float radial = length(local.xz);
    float inverse = radial > 1.0e-5 ? 1.0 / radial : 0.0;
    float cosine = local.x * inverse;
    float sine = local.z * inverse;
    float cosineSq = cosine * cosine;
    float sineSq = sine * sine;
    float sin3 = sine * (3.0 - 4.0 * sineSq);
    float cos3 = cosine * (4.0 * cosineSq - 3.0);
    float sin5 = sine * (5.0 - 20.0 * sineSq + 16.0 * sineSq * sineSq);
    float cos5 = cosine * (16.0 * cosineSq * cosineSq - 20.0 * cosineSq + 5.0);
    float wave = WAVE_AMP * (sin3 * Phase.y + cos3 * Phase.x
            + 0.45 * (sin5 * Phase.w - cos5 * Phase.z));

    float d = pos.y - (Band.x + wave);
    float dist = abs(d);
    // Точный отсев до экспонент и сглаживаний.
    if (dist > reach) {
        discard;
    }

    float core = 1.0 - smoothstep(BAND_HALF * 0.7, BAND_HALF, dist);
    float hot = 1.0 - smoothstep(BAND_HALF * 0.25, BAND_HALF * 0.45, dist);
    float glow = exp(-dist / GLOW_LEN) * GLOW_STRENGTH;
    float ambient = exp(-dist / AMBIENT_LEN) * AMBIENT_STRENGTH;

    // Шлейф тянется позади полосы, длина зависит от скорости хода.
    float trail = 0.0;
    float behind = -d * Band.y - BAND_HALF;
    if (behind > 0.0) {
        float trailLen = TRAIL_LEN * (0.25 + 0.75 * Band.z);
        trail = (1.0 - clamp(behind / trailLen, 0.0, 1.0)) * TRAIL_STRENGTH;
    }

    // Сетка привязана к цели, а не к миру, иначе она ползла бы по телу на
    // ходу. Ширина линии берётся в пикселях через производную: постоянная толщина в
    // блоках рябит на дистанции. Она же гасит сетку там, где ячейка стала мельче пикселя
    // или глубина рвётся, вместо того чтобы мерцать там сплошной заливкой.
    float grid = 0.0;
    if (Band.w > 0.0) {
        vec3 coord = local / GRID_CELL;
        vec3 deriv = fwidth(coord);
        vec3 edge = abs(fract(coord - 0.5) - 0.5) / max(deriv, vec3(1.0e-4));
        vec3 lines = (1.0 - min(edge, vec3(1.0))) * clamp(1.0 - deriv, 0.0, 1.0);
        grid = max(max(lines.x, lines.y), lines.z)
                * (1.0 - smoothstep(0.0, Band.w, dist)) * GRID_STRENGTH;
    }

    float band = core + glow + ambient + trail + grid;
    if (band + hot < MIN_CONTRIBUTION) {
        discard;
    }

    // Перелив между цветами темы идёт по окружности и вращается со временем.
    // Косинус разности угла и фазы раскрывается через уже готовые синус с косинусом
    // обоих, так что ни одной тригонометрической функции здесь не появляется.
    vec4 tint = mix(Tint, Tint2, 0.5 + 0.5 * (cosine * Phase.y + sine * Phase.x));

    vec3 hotColor = mix(tint.rgb, vec3(1.0), HOT_MIX);
    vec3 result = tint.rgb * band + hotColor * hot;
    fragColor = vec4(result, tint.a * Shape.y);
}
