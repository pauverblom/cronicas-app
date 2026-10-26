#!/usr/bin/env python3
"""Genera con Gemini las ilustraciones que ningún libro dibuja, a partir de dibujos parecidos de los libros.

Uso:
    python3 scripts/generar_ilustraciones.py LORE/Cronicasdelamarca.pdf app/src/main/assets/ilustraciones DIR_ORIGINALES \\
        [--solo id1,id2] [--modelo gemini-3-pro-image] [--paquete | --procesar]

Lee GEMINI_API_KEY del entorno o de ~/.gemini/.env. Ejecutar después de extract_ilustraciones.py, que genera las
referencias. Guarda el PNG original de Gemini en DIR_ORIGINALES y la versión procesada (tinta en escala de grises,
fondo transparente, recortada, WebP) en la carpeta de assets con el nombre que espera la app.
Las ilustraciones generadas así están listadas en GENERADAS para distinguirlas de las de los libros.

Sin API (a mano, en la app de Gemini):
    --paquete   crea DIR_ORIGINALES/<id>/ con las referencias (ref_1.png…) y el texto a pegar (prompt.txt).
    --procesar  toma la imagen descargada que se deje en cada DIR_ORIGINALES/<id>/ (cualquier PNG/JPG/WebP que
                no sea ref_*) y la procesa igual que la API a la carpeta de assets.
"""
import base64
import io
import json
import os
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

from PIL import Image, ImageChops

ESTILO = (
    "Draw it in exactly the same style as the reference illustration(s): black ink line art for a tabletop RPG "
    "rulebook, confident outlines, hatching and halftone dot shading, no colour, on a pure white background. "
    "A single subject, centred, fully visible, with generous white margin. "
    "No text or letters other than any badge asked for above, no signature, no frame, no border, no background scenery"
)

# id -> (referencias en assets, descripción, relación de aspecto)
# Las referencias "lamina:<página>:<caja>" se recortan de una lámina del reglamento (dibujos que no son artículos).
GENERADAS = {
    # ── Armas y munición: variantes de dibujos existentes ──
    "objeto_arco_corto_compuesto": (["objeto_arco_corto"], "a composite short bow: strongly recurved limbs made of laminated horn and sinew, wrapped grip, strung", "3:4"),
    "objeto_arco_largo_compuesto": (["objeto_arco_largo"], "a composite longbow: tall bow with recurved tips, layered horn and sinew limbs, leather-wrapped grip, strung", "9:16"),
    "objeto_ballesta_ligera": (["objeto_ballesta_mano", "objeto_ballesta_repeticion"], "a light crossbow for two hands: wooden stock, steel prod, simple stirrup at the front, no magazine, no crank", "4:3"),
    "objeto_ballesta_pesada": (["objeto_ballesta_mano", "objeto_ballesta_repeticion"], "a heavy crossbow: long thick wooden stock, very thick steel prod, large stirrup and a windlass crank mechanism at the back", "4:3"),
    "objeto_daga_plata": (["objeto_daga"], "a silver dagger: same shape as the reference dagger but with a bright polished silver blade (little shading on the blade) and finely engraved silver crossguard and pommel", "3:4"),
    "objeto_estoque": (["objeto_espada_larga"], "a rapier (estoque): long very thin straight blade, elaborate swept hilt with curved bars protecting the hand, small pommel", "9:16"),
    "objeto_lanza_caballeria": (["objeto_pica", "objeto_lanza"], "a knight's cavalry lance: very long thick wooden shaft, conical steel vamplate hand guard near the grip, small pointed steel tip", "9:16"),
    "objeto_dardos_cerbatana": (["objeto_cerbatana"], "a small bundle of blowgun darts tied with a thread: thin needle-like darts with tufts of fluff at the back, no blowgun tube", "1:1"),
    "objeto_virotes_ligeros": (["objeto_virotes"], "a leather quiver holding light crossbow bolts: thinner and shorter bolts than the reference, with small fletching, plus one loose bolt beside it", "3:4"),
    # ── Armaduras: conservan la insignia de CA de la lámina ──
    "objeto_camisote_mallas": (["objeto_cota_mallas", "objeto_anillas"], "a chain mail shirt (camisote de mallas): short-sleeved, waist-length mail shirt without hood, shown on its own like the references, with a round badge reading '+4' at the lower left like the references", "3:4"),
    "objeto_armadura_varillas": (["objeto_laminada", "objeto_anillas"], "splint armour (armadura de varillas): a torso armour made of narrow vertical metal strips riveted onto a leather backing, shown on its own like the references, with a round badge reading '+4' at the lower left like the references", "3:4"),
    # ── Escudos: misma composición que la lámina (silueta gris detrás e insignia de CA/atacantes) ──
    "objeto_escudo_pequeno_madera": (["objeto_broquel"], "a small round wooden shield made of planks with an iron rim and boss, held by the same grey silhouette, with the same round badge reading '+1/1' at the lower left", "3:4"),
    "objeto_escudo_pequeno_metal": (["objeto_broquel"], "a small round all-metal shield with a riveted rim and central boss, held by the same grey silhouette, with the same round badge reading '+1/1' at the lower left", "3:4"),
    "objeto_escudo_medio_madera": (["lamina:54:268,0,534,539"], "a medium wooden heater shield made of planks with iron bands, held by the same grey silhouette, with the same round badge reading '+1/2' at the lower left", "3:4"),
    "objeto_escudo_medio_metal": (["lamina:54:268,0,534,539"], "a medium steel heater shield with a riveted rim, held by the same grey silhouette, with the same round badge reading '+1/2' at the lower left", "3:4"),
    "objeto_escudo_pesado_madera": (["lamina:54:534,0,830,539"], "a large heavy round wooden shield made of thick planks with iron rim and boss, held by the same grey silhouette, with the same round badge reading '+1/3' at the lower left", "3:4"),
    "objeto_escudo_pesado_metal": (["lamina:54:534,0,830,539"], "a large heavy round steel shield with a thick riveted rim and boss, held by the same grey silhouette, with the same round badge reading '+1/3' at the lower left", "3:4"),
    # ── Equipo ──
    "objeto_cuerda_seda": (["objeto_cuerda_canamo"], "a coil of fine silk rope: thinner, smoother and more tightly coiled than the hemp rope in the reference", "4:3"),
    "objeto_arnica": (["objeto_ajo", "objeto_aceite"], "a small bundle of dried arnica flowers (daisy-like medicinal flowers) tied with string", "1:1"),
    "objeto_escala": (["objeto_pertiga", "objeto_cuerda_canamo"], "a 3-metre wooden ladder leaning slightly, simple rungs", "3:4"),
    "objeto_racion": (["objeto_odre", "objeto_saco_pequeno"], "a one-day travel ration: a small loaf of bread, a wedge of cheese and strips of dried meat on an open cloth wrap", "4:3"),
    "objeto_tiza": (["objeto_velas", "objeto_estacas"], "three sticks of chalk lying together, one with a worn tip", "4:3"),
    "objeto_muda_comun": (["objeto_manta", "objeto_alforjas"], "a set of plain peasant clothes neatly folded and stacked: rough wool tunic, trousers, simple hood, with worn shoes beside them", "4:3"),
    "objeto_muda_buena": (["objeto_manta", "objeto_alforjas"], "a set of good-quality traveller's clothes: a buttoned doublet, a cloak with a metal clasp and leather boots, laid out together", "4:3"),
    "objeto_muda_noble": (["objeto_manta", "objeto_alforjas"], "a set of noble's fine clothes: embroidered doublet with puffed sleeves, fur-trimmed cloak, feathered hat and fine gloves, laid out together", "4:3"),
    "objeto_barda_cuero": (["objeto_cuero_tachonado", "objeto_alforjas"], "leather horse barding displayed on a wooden saddle stand: padded and studded leather covering for a horse's neck, chest and flanks", "4:3"),
    "objeto_barda_metal": (["objeto_armadura_completa", "objeto_alforjas"], "metal horse barding displayed on a wooden saddle stand: articulated steel plates for a horse's head (chanfron), neck and chest", "4:3"),
    "objeto_silla_montar": (["objeto_alforjas", "objeto_mochila"], "a leather riding saddle with stirrups hanging down and a saddle blanket underneath", "4:3"),
    "objeto_brida": (["objeto_alforjas", "objeto_cadena"], "a horse bridle: leather headstall with metal bit and long reins, hanging from a peg", "3:4"),
    "objeto_pienso": (["objeto_saco_grande"], "an open sack of horse feed with oats and grain spilling out onto the ground", "1:1"),
    # ── Especie y clase ──
    "especie_semielfo": (["especie_elfo", "especie_humano"], "a half-elf adventurer, full figure standing like the reference character illustrations: features halfway between the elf and the human references, slightly pointed ears, travelling clothes with light leather armour and a sword at the hip", "9:16"),
    "clase_armonico_hueso": (["clase_bardo", "clase_druida"], "a Bone Harmonist from a swamp town: a gaunt bard-like musician, full figure standing like the reference character illustrations, playing a lute-like instrument built from bones and a skull, tattered layered clothes, a third arm (swamp mutation), small glowing mushrooms growing on the shoulder and a few fireflies around", "9:16"),
}


def clave_api():
    if os.environ.get("GEMINI_API_KEY"):
        return os.environ["GEMINI_API_KEY"]
    with open(os.path.expanduser("~/.gemini/.env")) as f:
        for linea in f:
            if linea.startswith("GEMINI_API_KEY="):
                return linea.split("=", 1)[1].strip().strip('"')
    sys.exit("No hay GEMINI_API_KEY")


def png_referencia(ref, assets, pdf, tmp):
    """PNG sobre blanco de una referencia (asset o recorte de lámina)."""
    if ref.startswith("lamina:"):
        _, pagina, caja = ref.split(":")
        prefijo = os.path.join(tmp, f"p{pagina}")
        subprocess.run(["pdfimages", "-f", pagina, "-l", pagina, "-png", pdf, prefijo], check=True)
        im = next(Image.open(os.path.join(tmp, f)) for f in sorted(os.listdir(tmp))
                  if f.startswith(f"p{pagina}-") and Image.open(os.path.join(tmp, f)).size == (1090, 539))
        im = im.convert("RGB").crop(tuple(int(v) for v in caja.split(",")))
    else:
        im = Image.open(os.path.join(assets, ref + ".webp")).convert("RGBA")
        fondo = Image.new("RGBA", im.size, (255, 255, 255, 255))
        fondo.alpha_composite(im)
        im = fondo.convert("RGB")
    buf = io.BytesIO()
    im.save(buf, "PNG")
    return buf.getvalue()


def instruccion(descripcion):
    return f"Create a new illustration of {descripcion}. {ESTILO}."


def generar(clave, modelo, refs, descripcion, aspecto):
    partes = [{"inline_data": {"mime_type": "image/png", "data": base64.b64encode(r).decode()}} for r in refs]
    partes.append({"text": instruccion(descripcion)})
    cuerpo = {
        "contents": [{"parts": partes}],
        "generationConfig": {"responseModalities": ["IMAGE"], "imageConfig": {"aspectRatio": aspecto}},
    }
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{modelo}:generateContent"
    req = urllib.request.Request(url, data=json.dumps(cuerpo).encode(),
                                 headers={"x-goog-api-key": clave, "Content-Type": "application/json"})
    for intento in range(4):
        try:
            resp = json.load(urllib.request.urlopen(req, timeout=180))
            for parte in resp["candidates"][0]["content"]["parts"]:
                datos = parte.get("inlineData") or parte.get("inline_data")
                if datos:
                    return base64.b64decode(datos["data"])
            raise RuntimeError(f"sin imagen en la respuesta: {json.dumps(resp)[:300]}")
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 503) and intento < 3:
                time.sleep(20 * (intento + 1))
                continue
            raise RuntimeError(f"HTTP {e.code}: {e.read()[:300]!r}")


def procesar(png, destino):
    """Igual que las ilustraciones de los libros: tinta gris, papel transparente, recortado, máx. 720 px de alto."""
    gris = Image.open(io.BytesIO(png)).convert("L")
    rgba = Image.merge("RGBA", (gris, gris, gris, gris.point(lambda v: 0 if v > 235 else 255)))
    caja = rgba.getchannel("A").getbbox()
    if caja:
        m = 12
        rgba = rgba.crop((max(0, caja[0] - m), max(0, caja[1] - m), min(rgba.width, caja[2] + m), min(rgba.height, caja[3] + m)))
    if rgba.height > 720:
        rgba = rgba.resize((round(rgba.width * 720 / rgba.height), 720), Image.LANCZOS)
    rgba.save(destino, "WEBP", quality=80, method=6)


def paquete(nombre, refs, descripcion, aspecto, carpeta):
    """Referencias y texto para pedir la ilustración a mano en la app de Gemini (que no deja fijar el aspecto)."""
    os.makedirs(carpeta, exist_ok=True)
    for i, r in enumerate(refs, 1):
        with open(os.path.join(carpeta, f"ref_{i}.png"), "wb") as f:
            f.write(r)
    adjuntas = "the attached reference illustration" if len(refs) == 1 else f"the {len(refs)} attached reference illustrations"
    with open(os.path.join(carpeta, "prompt.txt"), "w") as f:
        f.write(f"Using {adjuntas} as the style guide: {instruccion(descripcion)} "
                f"Aspect ratio {aspecto}.\n")


def descargada(carpeta):
    """La imagen más reciente de la carpeta que no sea una referencia, o None."""
    candidatas = [os.path.join(carpeta, f) for f in os.listdir(carpeta)
                  if f.lower().endswith((".png", ".jpg", ".jpeg", ".webp")) and not f.startswith("ref_")]
    return max(candidatas, key=os.path.getmtime) if candidatas else None


def main():
    args = sys.argv[1:]
    modo = "api"
    for m in ("--paquete", "--procesar"):
        if m in args:
            modo = m[2:]
            args.remove(m)
    solo = None
    modelo = "gemini-3-pro-image"
    if "--solo" in args:
        i = args.index("--solo")
        solo = set(args[i + 1].split(","))
        del args[i:i + 2]
    if "--modelo" in args:
        i = args.index("--modelo")
        modelo = args[i + 1]
        del args[i:i + 2]
    pdf, assets, originales = args
    os.makedirs(originales, exist_ok=True)
    clave = clave_api() if modo == "api" else None
    fallos = []
    with tempfile.TemporaryDirectory() as tmp:
        for nombre, (refs, descripcion, aspecto) in GENERADAS.items():
            if solo and nombre not in solo and nombre.split("_", 1)[1] not in solo:
                continue
            try:
                if modo == "paquete":
                    paquete(nombre, [png_referencia(r, assets, pdf, tmp) for r in refs], descripcion, aspecto,
                            os.path.join(originales, nombre))
                    print("ok   ", nombre, file=sys.stderr)
                    continue
                if modo == "procesar":
                    carpeta = os.path.join(originales, nombre)
                    imagen = descargada(carpeta) if os.path.isdir(carpeta) else None
                    if imagen is None:
                        print("falta", nombre, file=sys.stderr)
                        continue
                    with open(imagen, "rb") as f:
                        procesar(f.read(), os.path.join(assets, nombre + ".webp"))
                    print("ok   ", nombre, file=sys.stderr)
                    continue
                png = generar(clave, modelo, [png_referencia(r, assets, pdf, tmp) for r in refs], descripcion, aspecto)
                with open(os.path.join(originales, nombre + ".png"), "wb") as f:
                    f.write(png)
                procesar(png, os.path.join(assets, nombre + ".webp"))
                print("ok   ", nombre, file=sys.stderr)
            except Exception as e:  # sigue con las demás
                fallos.append(nombre)
                print("FALLO", nombre, e, file=sys.stderr)
    if fallos:
        sys.exit(f"{len(fallos)} fallos: {', '.join(fallos)}")


if __name__ == "__main__":
    main()
