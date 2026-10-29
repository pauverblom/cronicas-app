// Ayudas del navegador para la versión web (Kotlin/Wasm las llama desde ui/plataforma y web/Js.kt).
// Los datos binarios viajan como base64 para cruzar la frontera con Kotlin de una vez.

// nube.js resuelve esta promesa con la API de Firebase, o con null si no hay configuración
window.cronicasNubeLista = new Promise(function (resolver) { window.__resolverNube = resolver; });

function base64DeBuffer(buffer) {
    var bytes = new Uint8Array(buffer), trozos = [], paso = 0x8000;
    for (var i = 0; i < bytes.length; i += paso) trozos.push(String.fromCharCode.apply(null, bytes.subarray(i, i + paso)));
    return btoa(trozos.join(""));
}

/** Pide un archivo. Devuelve JSON {"nombre", "mime", "base64"} o null si se cancela. */
window.cronicasElegirArchivo = function (accept) {
    return new Promise(function (resolver) {
        var input = document.createElement("input");
        input.type = "file";
        if (accept) input.accept = accept;
        input.style.display = "none";
        document.body.appendChild(input);
        input.addEventListener("change", function () {
            var f = input.files && input.files[0];
            document.body.removeChild(input);
            if (!f) { resolver(null); return; }
            f.arrayBuffer().then(function (buf) {
                resolver(JSON.stringify({ nombre: f.name, mime: f.type || "", base64: base64DeBuffer(buf) }));
            }, function () { resolver(null); });
        });
        input.addEventListener("cancel", function () { document.body.removeChild(input); resolver(null); });
        input.click();
    });
};

/** Descarga un archivo con [nombre] (en el iPhone, se abre la hoja de compartir/guardar). */
window.cronicasDescargar = function (nombre, mime, base64) {
    var bin = atob(base64), bytes = new Uint8Array(bin.length);
    for (var i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    var url = URL.createObjectURL(new Blob([bytes], { type: mime || "application/octet-stream" }));
    var a = document.createElement("a");
    a.href = url; a.download = nombre;
    document.body.appendChild(a); a.click(); document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 10000);
};

/**
 * Los tres colores del tema «Desde retrato» (ARGB), como en la app Android: el acento es el tono más vivo (y
 * presente); los marcos, el siguiente tono vivo bien distinto del acento (o el acento); el fondo, el tono de las zonas
 * oscuras y apagadas. Los píxeles se agrupan por tono en 24 cubos. Devuelve null si la imagen es gris.
 */
function coloresRetrato(datos) {
    var cubos = [], oscuro = { r: 0, g: 0, b: 0, n: 0 };
    for (var k = 0; k < 24; k++) cubos.push({ r: 0, g: 0, b: 0, n: 0, peso: 0, tono: k * 15 + 7.5 });
    for (var i = 0; i < datos.length; i += 16) {
        var R = datos[i], G = datos[i + 1], B = datos[i + 2];
        var max = Math.max(R, G, B), min = Math.min(R, G, B), l = (max + min) / 510, d = (max - min) / 255;
        var sat = d === 0 ? 0 : d / (1 - Math.abs(2 * l - 1));
        if (l < 0.45) { oscuro.r += R; oscuro.g += G; oscuro.b += B; oscuro.n++; }
        if (sat < 0.15 || l < 0.08 || l > 0.95) continue;
        var h = 0;
        if (max === R) h = ((G - B) / (max - min)) % 6; else if (max === G) h = (B - R) / (max - min) + 2; else h = (R - G) / (max - min) + 4;
        h = (h * 60 + 360) % 360;
        var c = cubos[Math.floor(h / 15) % 24];
        // Cuenta más lo vivo que lo extenso: un fondo grande y apagado no se lleva el acento
        c.r += R; c.g += G; c.b += B; c.n++; c.peso += d * d;
    }
    var argb = function (c) { return ((0xff << 24) | (Math.round(c.r / c.n) << 16) | (Math.round(c.g / c.n) << 8) | Math.round(c.b / c.n)) | 0; };
    var distancia = function (a, b) { var x = Math.abs(a - b) % 360; return x > 180 ? 360 - x : x; };
    var orden = cubos.filter(function (c) { return c.n > 0; }).sort(function (a, b) { return b.peso - a.peso; });
    if (orden.length === 0) return null;
    var acento = orden[0];
    var marco = orden.filter(function (c) { return distancia(c.tono, acento.tono) >= 30; })[0] || acento;
    var fondo = oscuro.n > 0 ? argb(oscuro) : argb(acento);
    return { acento: argb(acento), marco: argb(marco), fondo: fondo };
}

/**
 * Retrato: recorte cuadrado centrado a 512 px como mucho, en JPEG, y sus colores para el tema
 * «Desde retrato». Devuelve JSON {"base64", "colores": {"acento", "marco", "fondo"} o null} o null si la imagen no se puede leer.
 */
window.cronicasProcesarRetrato = function (base64) {
    return new Promise(function (resolver) {
        var img = new Image();
        img.onload = function () {
            var lado = Math.min(img.width, img.height), destino = Math.min(512, lado);
            var canvas = document.createElement("canvas");
            canvas.width = destino; canvas.height = destino;
            var ctx = canvas.getContext("2d");
            ctx.drawImage(img, (img.width - lado) / 2, (img.height - lado) / 2, lado, lado, 0, 0, destino, destino);
            var colores = coloresRetrato(ctx.getImageData(0, 0, destino, destino).data);
            var jpeg = canvas.toDataURL("image/jpeg", 0.9);
            resolver(JSON.stringify({ base64: jpeg.substring(jpeg.indexOf(",") + 1), colores: colores }));
        };
        img.onerror = function () { resolver(null); };
        img.src = "data:image/*;base64," + base64;
    });
};

/** «31/12/2026 18:05» en la hora local. */
window.cronicasFechaHora = function (ms) {
    var d = new Date(ms), p = function (n) { return String(n).padStart(2, "0"); };
    return p(d.getDate()) + "/" + p(d.getMonth() + 1) + "/" + d.getFullYear() + " " + p(d.getHours()) + ":" + p(d.getMinutes());
};

/** Quita el «Cargando…» cuando la app ya pinta. */
window.cronicasListo = function () {
    var c = document.getElementById("carga");
    if (c) c.remove();
    // Las fichas viven en localStorage: se pide que el navegador no lo borre por falta de espacio
    if (navigator.storage && navigator.storage.persist) navigator.storage.persist().catch(function () {});
};

/** Pinta la franja de arriba, el fondo de la página y el color del navegador con la noche del tema (ARGB). */
window.cronicasPintarBarra = function (argb) {
    var hex = "#" + ((argb >>> 0) & 0xffffff).toString(16).padStart(6, "0");
    document.documentElement.style.setProperty("--noche", hex);
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", hex);
};

/**
 * Texto nativo para escribir en pantallas táctiles (lo usa EditorNativo en ui/plataforma): un <input> o <textarea>
 * real, transparente, encima del campo de Compose mientras se escribe. Así el teclado, la selección, copiar y pegar
 * y el borrado por palabras son los del sistema. [opciones] es JSON; [caja] = [x, y, ancho, alto] en px CSS.
 */
window.cronicasEditor = {
    abrir: function (opciones, valor, x, y, ancho, alto, alCambiar, alCerrar) {
        var o = JSON.parse(opciones);
        var e = document.createElement(o.multilinea ? "textarea" : "input");
        if (!o.multilinea) e.type = "text";
        e.value = valor;
        e.setAttribute("inputmode", o.modo);
        e.setAttribute("enterkeyhint", o.multilinea ? "enter" : "done");
        e.setAttribute("autocapitalize", o.mayusculas ? "sentences" : "off");
        e.setAttribute("autocomplete", "off");
        e.setAttribute("autocorrect", o.multilinea ? "on" : "off");
        e.setAttribute("spellcheck", o.multilinea ? "true" : "false");
        if (o.etiqueta) e.setAttribute("aria-label", o.etiqueta);
        var s = e.style;
        s.position = "absolute"; s.margin = "0"; s.padding = "0"; s.border = "0"; s.outline = "none";
        s.background = "transparent"; s.resize = "none"; s.boxSizing = "border-box"; s.zIndex = "5";
        s.overflow = o.multilinea ? "auto" : "hidden";
        s.fontFamily = o.fuente; s.fontSize = o.tamano + "px"; s.fontWeight = String(o.peso);
        s.fontStyle = o.cursiva ? "italic" : "normal"; s.lineHeight = o.interlineado + "px";
        s.letterSpacing = o.espaciado + "px"; s.color = o.color; s.caretColor = o.cursor;
        // El resto de la página no deja seleccionar ni sacar el menú de iOS; aquí sí
        s.webkitUserSelect = "text"; s.userSelect = "text"; s.webkitTouchCallout = "default";
        window.cronicasEditor.colocar(e, x, y, ancho, alto);
        var cerrado = false;
        e.__cerrar = function () { cerrado = true; };
        e.addEventListener("input", function () { alCambiar(e.value); });
        e.addEventListener("blur", function () { if (!cerrado) { cerrado = true; alCerrar(); } });
        if (!o.multilinea) e.addEventListener("keydown", function (ev) { if (ev.key === "Enter") { ev.preventDefault(); e.blur(); } });
        document.getElementById("app").appendChild(e);
        // Dentro del toque: solo así Safari saca el teclado
        e.focus();
        var n = e.value.length;
        try { e.setSelectionRange(n, n); } catch (x) {}
        return e;
    },
    colocar: function (e, x, y, ancho, alto) {
        e.style.left = x + "px"; e.style.top = y + "px"; e.style.width = ancho + "px"; e.style.height = alto + "px";
    },
    poner: function (e, valor) { if (e.value !== valor) e.value = valor; },
    cerrar: function (e) { if (e.__cerrar) e.__cerrar(); if (e.parentNode) e.parentNode.removeChild(e); }
};

// Con el teclado en pantalla, la app se pinta solo en lo que queda visible (iOS no encoge la ventana), y así
// los diálogos se recolocan por encima del teclado
if (window.visualViewport) {
    (function () {
        var vv = window.visualViewport;
        var anterior = "0px";
        var ajustar = function () {
            var tapado = Math.max(0, window.innerHeight - vv.height);
            var nuevo = tapado > 80 ? tapado + "px" : "0px";
            if (tapado > 80 && (window.scrollY || vv.offsetTop)) window.scrollTo(0, 0);
            if (nuevo === anterior) return;
            anterior = nuevo;
            document.documentElement.style.setProperty("--teclado", nuevo);
            // Compose solo mide su lienzo de nuevo con el evento resize de la ventana
            window.dispatchEvent(new Event("resize"));
        };
        vv.addEventListener("resize", ajustar);
    })();
}
