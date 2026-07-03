# App launcher icons

Place the following source images here, then generate the native icons.

## Required file

| File | Size | Description |
| --- | --- | --- |
| `rippo_icon.png` | square (1024×1024 recommended) | The Rippo logo. Used for the legacy icon and the adaptive-icon foreground. |

The adaptive background is configured as `#FFFFFF` in `pubspec.yaml` to match the
logo's white background.

Note: Android adaptive icons crop to a circle/squircle, so the far-left fox ear
and far-right tail of the wide wordmark may be clipped on some launchers. A square
crop of the fox mark would render more cleanly.

## Generate

From the `frontend/` directory:

```bash
flutter pub get
dart run flutter_launcher_icons
```

This writes the generated icons into `android/app/src/main/res/mipmap-*`.
Rebuild the app afterwards.
