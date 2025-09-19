
import express from "express";
import { readFile } from "fs/promises";

let m;
let memory;
const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();
let J = 0;

function re(n, malloc, realloc) {
    if (realloc === undefined) {
        const s = textEncoder.encode(n);
        const ptr = malloc(s.length, 1) >>> 0;
        new Uint8Array(memory.buffer, ptr, s.length).set(s);
        J = s.length;
        return ptr;
    }
    let a = n.length;
    let ptr = malloc(a, 1) >>> 0;
    const mem = new Uint8Array(memory.buffer);
    let i = 0;
    for (; i < a; i++) {
        const code = n.charCodeAt(i);
        if (code > 0x7F) break;
        mem[ptr + i] = code;
    }
    if (i !== a) {
        if (i !== 0) n = n.slice(i);
        ptr = realloc(ptr, a, (a = i + n.length * 3), 1) >>> 0;
        const view = new Uint8Array(memory.buffer, ptr + i, a - i);
        const { written } = textEncoder.encodeInto(n, view);
        i += written;
        ptr = realloc(ptr, a, i, 1) >>> 0;
    }
    J = i;
    return ptr;
}

function P(ptr, len) {
    return textDecoder.decode(new Uint8Array(memory.buffer, ptr, len));
}

function fn(n) {
    let e, t;
    try {
        const a = re(n, m.__wbindgen_malloc, m.__wbindgen_realloc);
        const r = J;
        const o = m.get_pawtected_endpoint_payload(a, r);
        return (e = o[0]), (t = o[1]), P(o[0], o[1]);
    } finally {
        if (e !== undefined) {
            m.__wbindgen_free(e, t, 1);
        }
    }
}

async function loadWASM() {
    if (m) return;
    const wasmBuffer = await readFile("./pawtect_wasm_bg.wasm");
    const imports = hn();
    const { instance } = await WebAssembly.instantiate(wasmBuffer, imports);
    m = instance.exports;
    memory = m.memory;
    if (m.__wbindgen_start) m.__wbindgen_start();
}

function hn() {
    const n = {};
    n.wbg = {};
    n.wbg.__wbg_buffer_609cc3eee51ed158 = e => e.buffer;
    n.wbg.__wbg_call_672a4d21634d4a24 = (e, t) => e.call(t);
    n.wbg.__wbg_call_7cccdd69e0791ae2 = (e, t, a) => e.call(t, a);
    n.wbg.__wbg_crypto_574e78ad8b13b65f = e => e.crypto;
    n.wbg.__wbg_getRandomValues_b8f5dbd5f3995a9e = (e, t) => e.getRandomValues(t);
    n.wbg.__wbg_msCrypto_a61aeb35a24c1329 = e => e.msCrypto;
    n.wbg.__wbg_new_a12002a7f91c75be = e => new Uint8Array(e);
    n.wbg.__wbg_newnoargs_105ed471475aaf50 = (e, t) => new Function(P(e, t));
    n.wbg.__wbg_newwithbyteoffsetandlength_d97e637ebe145a9a = (e, t, a) =>
        new Uint8Array(e, t >>> 0, a >>> 0);
    n.wbg.__wbg_newwithlength_a381634e90c276d4 = e => new Uint8Array(e >>> 0);
    n.wbg.__wbg_node_905d3e251edff8a2 = e => e.node;
    n.wbg.__wbg_process_dc0fbacc7c1c06f7 = e => e.process;
    n.wbg.__wbg_randomFillSync_ac0988aba3254290 = (e, t) =>
        e.randomFillSync(t);
    n.wbg.__wbg_require_60cc747a6bc5215a = () => module.require;
    n.wbg.__wbg_set_65595bdd868b3009 = (e, t, a) => e.set(t, a >>> 0);
    n.wbg.__wbg_static_accessor_GLOBAL_88a902d13a557d07 = () =>
        typeof global === "undefined" ? null : global;
    n.wbg.__wbg_static_accessor_GLOBAL_THIS_56578be7e9f832b0 = () =>
        typeof globalThis === "undefined" ? null : globalThis;
    n.wbg.__wbg_static_accessor_SELF_37c5d418e4bf5819 = () =>
        typeof self === "undefined" ? null : self;
    n.wbg.__wbg_static_accessor_WINDOW_5de37043a91a9c40 = () =>
        typeof window === "undefined" ? null : window;
    n.wbg.__wbg_subarray_aa9065fa9dc5df96 = (e, t, a) =>
        e.subarray(t >>> 0, a >>> 0);
    n.wbg.__wbg_versions_c01dfd4722a88165 = e => e.versions;
    n.wbg.__wbindgen_init_externref_table = () => {
        const e = m.__wbindgen_export_2;
        const t = e.grow(4);
        e.set(0, void 0);
        e.set(t + 0, void 0);
        e.set(t + 1, null);
        e.set(t + 2, true);
        e.set(t + 3, false);
    };
    n.wbg.__wbindgen_is_function = e => typeof e === "function";
    n.wbg.__wbindgen_is_object = e => typeof e === "object" && e !== null;
    n.wbg.__wbindgen_is_string = e => typeof e === "string";
    n.wbg.__wbindgen_is_undefined = e => e === void 0;
    n.wbg.__wbindgen_memory = () => m.memory;
    n.wbg.__wbindgen_string_new = (e, t) => P(e, t);
    n.wbg.__wbindgen_throw = (e, t) => {
        throw new Error(P(e, t));
    };
    return n;
}

const app = express();
app.use(express.json());

app.post("/sign", async (req, res) => {
    try {
        const { url, body, userId } = req.body;
        if (!url || !body || !userId) {
            return res.status(400).json({ error: "url, body, userId are required" });
        }
        await loadWASM();
        m.set_user_id(userId);
        const urlPtr = re(url, m.__wbindgen_malloc, m.__wbindgen_realloc);
        m.request_url(urlPtr, J);
        m.get_load_payload();
        const sign = fn(body);
        res.json({ sign });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: String(err) });
    }
});

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`Sign server listening at http://localhost:${PORT}`);
});
