<div align="center">
  <img src="src/main/resources/assets/better_weather/icon.png" width="120" alt="Better Weather icon"/>

  <h1>Better Weather (1.12.2 Forge)</h1>

  <p>Volumetric fluffy clouds and local, moving weather for Minecraft 1.12.2.</p>
</div>

A Minecraft Forge 1.12.2 port of [paulevs' Better Weather](https://github.com/paulevsGitch/BetterWeather),
originally written for Minecraft Beta 1.7.3. All of the original look and behavior is preserved, with
the rendering and simulation rebuilt on the 1.12.2 Forge rendering pipeline.

## Features

- **Volumetric clouds.** Clouds are 32 to 64 blocks tall, fluffy, and softly shaded instead of the flat vanilla sheet.
- **Local weather.** Rain and snow are tied to cloud fronts. Weather moves across the world with the clouds rather than toggling globally.
- **Better rain and snow rendering.** Precipitation uses level of detail and renders over larger distances.
- **Better weather sounds.** Improved rain ambience.
- **Clouds beyond render distance.** Clouds can extend past the terrain view distance without clipping at the far plane.
- **In game config.** Cloud height presets (Beta 108 / vanilla 128 / custom), fluffy toggle, rain color, cloud speed, and cloud view distance are editable live under Mods, Better Weather, Config.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge 14.23.5.2860 or newer
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter) (the mod loads its mixins through MixinBooter)

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Drop MixinBooter and `better_weather-1.0.jar` into your `mods` folder.
3. Launch the game.

## Building from source

```
./gradlew build
```

The built jar is written to `build/libs/`.

## Credits

- Original mod: [paulevs](https://github.com/paulevsGitch/BetterWeather)
- 1.12.2 Forge port: hunterhaunter

## License

[MIT](LICENSE). The original mod is MIT licensed by paulevs; this port keeps the same license.
