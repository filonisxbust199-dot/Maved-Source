// WorldTweaks weather: one uniform block for the sky, the wet gloss, its blur and the combine pass (ported from
// Zenith, where every pass had its own block). WeatherFx writes it in this order; the names the shader bodies use are
// mapped onto its vec4 lanes below, so the bodies stay as they were.
layout(std140) uniform WeatherFrame {
    vec4 Frame0;        // Intensity, time, SkyPitch, SkyYaw
    vec4 Frame1;        // TanHalfFov, Aspect, DepthA, DepthB (view depth = DepthB / (depth + DepthA), reverse Z)
    vec4 CameraPosition4;
    vec4 SkyZenith4;
    vec4 SkyHorizon4;
    vec4 SkyNadir4;
    vec4 SunDir4;
    vec4 SunColor4;
    vec4 Sun0;          // SunSize, SunGlare, CloudCover, CloudDark
    vec4 Cloud0;        // CloudSpeed, CloudScale, GlowFocus, Flash
    vec4 CloudLit4;
    vec4 CloudShadow4;
    vec4 Night0;        // StarAmount, BandAmount, AuroraAmount, MeteorAmount
    vec4 Haze0;         // HazeAmount, FrostAmount, Opacity, FarPlane
    vec4 HazeColor4;
    vec4 GlowDir4;
    vec4 FlashColor4;
    vec4 Wet0;          // WetAmount, DarkAmount, ReflectAmount, PuddleAmount
    vec4 Wet1;          // GlintAmount, Frost, TerrainValid, SnowGeometry
    vec4 Wet2;          // FadeEnd, CoverRadius, RainAmount, MistAmount
    vec4 WetTint4;
    vec4 MistTint4;
    vec4 Mist0;         // MistBase, MistHeight, MistWind
    vec4 Grade0;        // ZenithSaturation, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart
    vec4 Grade1;        // FogRenderDistanceEnd, BlurDir, NearPlane
    vec4 ZenithGrade;
    vec4 FogColor;
    vec4 Screen0;       // OutSize, InSize
};
#define Intensity Frame0.x
#define time Frame0.y
#define SkyPitch Frame0.z
#define SkyYaw Frame0.w
#define TanHalfFov Frame1.x
#define Aspect Frame1.y
#define DepthA Frame1.z
#define DepthB Frame1.w
#define CameraPosition CameraPosition4.xyz
#define SkyZenith SkyZenith4.xyz
#define SkyHorizon SkyHorizon4.xyz
#define SkyNadir SkyNadir4.xyz
#define SunDir SunDir4.xyz
#define SunColor SunColor4.xyz
#define SunSize Sun0.x
#define SunGlare Sun0.y
#define CloudCover Sun0.z
#define CloudDark Sun0.w
#define CloudSpeed Cloud0.x
#define CloudScale Cloud0.y
#define GlowFocus Cloud0.z
#define Flash Cloud0.w
#define CloudLit CloudLit4.xyz
#define CloudShadow CloudShadow4.xyz
#define StarAmount Night0.x
#define BandAmount Night0.y
#define AuroraAmount Night0.z
#define MeteorAmount Night0.w
#define HazeAmount Haze0.x
#define FrostAmount Haze0.y
#define Opacity Haze0.z
#define FarPlane Haze0.w
#define HazeColor HazeColor4.xyz
#define GlowDir GlowDir4.xyz
#define FlashColor FlashColor4.xyz
#define WetAmount Wet0.x
#define DarkAmount Wet0.y
#define ReflectAmount Wet0.z
#define PuddleAmount Wet0.w
#define GlintAmount Wet1.x
#define Frost Wet1.y
#define TerrainValid Wet1.z
#define SnowGeometry Wet1.w
#define FadeEnd Wet2.x
#define CoverRadius Wet2.y
#define RainAmount Wet2.z
#define MistAmount Wet2.w
#define WetTint WetTint4.xyz
#define MistTint MistTint4.xyz
#define MistBase Mist0.x
#define MistHeight Mist0.y
#define MistWind Mist0.zw
#define ZenithSaturation Grade0.x
#define FogEnvironmentalStart Grade0.y
#define FogEnvironmentalEnd Grade0.z
#define FogRenderDistanceStart Grade0.w
#define FogRenderDistanceEnd Grade1.x
#define BlurDir Grade1.yz
#define NearPlane Grade1.w
#define OutSize Screen0.xy
#define InSize Screen0.zw
#define oneTexel (1.0 / OutSize)

// Distance along the view axis for a depth sample. 26.3 renders with reversed depth (the sky is 0), so the
// 1.21 formula from near and far planes is replaced by the two projection terms.
float weatherLinearDepth(float depth) {
    return DepthB / max(depth + DepthA, 1.0e-6);
}
bool weatherIsSky(float depth) {
    return depth <= 0.000001;
}
