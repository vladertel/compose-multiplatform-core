import { instantiate } from './androidx-falling-balls-mpp-wasm.uninstantiated.mjs';

await wasmSetup;
instantiate({ skia: Module['asm'] });