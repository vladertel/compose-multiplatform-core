import { instantiate } from './androidx-demo-wasm.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });