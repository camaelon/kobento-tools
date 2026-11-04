# kobento-tools

Kobento tooling for [RemoteCompose](https://github.com/androidx/androidx/tree/androidx-main/compose/remote)
documents: converters, headless renderers, conformance harnesses and the inspector.

```
kobento-tools/
├── json2rc/      JSON → .rc converter (TypeScript). src/ is the source, build/ the compiled
│                 CommonJS converter (committed; refresh with json2rc/build.sh).
├── cpp/          the C++ command-line tools, on the engine from kobento-libs:
│                 rc2image (render → PNG), rc2json (binary → JSON), rc2layout (layout tree),
│                 rc2image-native (Core Graphics backend, needs kobento-ios), d3scene, rc3d, vecrpn
├── lottie/       Lottie → RemoteCompose converter (lottie2rc.py) and its validation
├── avd/          Android AnimatedVectorDrawable → RemoteCompose converter (avd2rc.py), its
│                 validation, and convert_icons.py (the Material Symbols kobento-mcp uses)
├── assets/avds/  the AVD corpus avd/ converts and validates against
├── conformance/  cross-engine comparison: Java reference (java-json.sh, java-layout.sh),
│                 layout specs, 3D / vector / determinism parity scripts
├── 3d-oracle/    the Java oracle for the 3D renderer's bit-exact parity
├── inspector/    the RemoteCompose inspector (web), built on the TypeScript player
└── creation/     a TypeScript document-creation experiment (flappy)
```

## Sibling checkouts

Tools that need an engine use the other Kobento repositories, checked out next to this one:

| needs | for |
| :--- | :--- |
| `../kobento-libs` (`-DKOBENTO_LIBS_DIR`) | the C++ tools (`cpp/`), the parity scripts |
| `../kobento-ts` (`KOBENTO_TS`) | the inspector, the TypeScript renders in the converters' validation, conformance |
| `../kobento-ios` (`-DKOBENTO_IOS_DIR`, optional) | `rc2image-native` |
| `../kobento-mcp` (`KOBENTO_MCP`) | `avd/convert_icons.py` reads and writes its `assets/icons` |

```sh
cmake -S cpp -B cpp/build && cmake --build cpp/build -j
cpp/build/rc2image/rc2image ../kobento-libs/samples/canvas.rc out.png
node json2rc/build/cli.js doc.json doc.rc
```

This repository is not endorsed or supported by Google.
