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
 * Retrato: recorte cuadrado centrado a 512 px como mucho, en JPEG, y su color dominante (ARGB) para el tema
 * «Desde retrato». Devuelve JSON {"base64", "color"} o null si la imagen no se puede leer.
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
            var datos = ctx.getImageData(0, 0, destino, destino).data, r = 0, g = 0, b = 0, peso = 0;
            for (var i = 0; i < datos.length; i += 16) {
                var R = datos[i], G = datos[i + 1], B = datos[i + 2];
                var max = Math.max(R, G, B), min = Math.min(R, G, B), w = (max - min) / 255 + 0.05;
                r += R * w; g += G * w; b += B * w; peso += w;
            }
            var color = peso > 0 ? ((0xff << 24) | (Math.round(r / peso) << 16) | (Math.round(g / peso) << 8) | Math.round(b / peso)) : 0;
            var jpeg = canvas.toDataURL("image/jpeg", 0.9);
            resolver(JSON.stringify({ base64: jpeg.substring(jpeg.indexOf(",") + 1), color: color | 0 }));
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
