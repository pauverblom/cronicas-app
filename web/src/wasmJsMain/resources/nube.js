// Sincronización con Firestore para la versión web: los mismos documentos que la app Android
// (personajes/{codigo} y personajes/{codigo}/retrato/actual). Sin window.FIREBASE_CONFIG no hace nada.
const VERSION = "10.14.1";
const base = "https://www.gstatic.com/firebasejs/" + VERSION + "/";

async function iniciar() {
    const cfg = window.FIREBASE_CONFIG;
    if (!cfg) return null;
    const { initializeApp } = await import(base + "firebase-app.js");
    const { getAuth, signInAnonymously } = await import(base + "firebase-auth.js");
    const fs = await import(base + "firebase-firestore.js");
    const app = initializeApp(cfg);
    const auth = getAuth(app);
    const db = fs.initializeFirestore(app, { localCache: fs.persistentLocalCache() });
    const ref = (codigo) => fs.doc(db, "personajes", codigo);
    const refRetrato = (codigo) => fs.doc(db, "personajes", codigo, "retrato", "actual");
    const ficha = (s) => JSON.stringify({ formato: s.get("formato") ?? 0, datos: s.get("datos") ?? "", retratoHash: s.get("retratoHash") ?? "" });
    const escuchas = new Map();
    let siguiente = 0;

    return {
        // Sesión anónima (se recuerda entre visitas)
        sesion: async () => {
            await auth.authStateReady();
            if (!auth.currentUser) await signInAnonymously(auth);
            return true;
        },
        leer: async (codigo) => {
            const s = await fs.getDocFromServer(ref(codigo));
            return s.exists() ? ficha(s) : null;
        },
        escribir: (codigo, formato, datos, retratoHash) => {
            fs.setDoc(ref(codigo), { formato, datos, retratoHash, actualizado: fs.serverTimestamp() })
                .catch((e) => console.warn("No se pudo subir la ficha", codigo, e));
        },
        // Solo cambios confirmados de otros dispositivos: los propios aún pendientes ya están aplicados
        escuchar: (codigo, cb) => {
            const id = ++siguiente;
            escuchas.set(id, fs.onSnapshot(ref(codigo), (s) => {
                if (s.exists() && !s.metadata.hasPendingWrites) cb(ficha(s));
            }, (e) => console.warn("Error escuchando", codigo, e)));
            return id;
        },
        dejar: (id) => {
            const f = escuchas.get(id);
            if (f) { f(); escuchas.delete(id); }
        },
        leerRetrato: async (codigo) => {
            const s = await fs.getDoc(refRetrato(codigo));
            return s.exists() ? JSON.stringify({ hash: s.get("hash") ?? "", imagen: s.get("imagen") ?? null }) : null;
        },
        escribirRetrato: (codigo, hash, imagen) => {
            fs.setDoc(refRetrato(codigo), { hash, imagen: imagen || null })
                .catch((e) => console.warn("No se pudo subir el retrato", codigo, e));
        }
    };
}

iniciar().then(
    (api) => { window.cronicasNube = api; window.__resolverNube(api); },
    (e) => { console.warn("Firebase no disponible", e); window.__resolverNube(null); }
);
