#!/usr/bin/env python3
"""Extrae las ilustraciones del reglamento a app/src/main/assets/ilustraciones/.

Uso: python3 scripts/extract_ilustraciones.py LORE/Cronicasdelamarca.pdf app/src/main/assets/ilustraciones

Necesita `pdfimages` (poppler) y Pillow. Las páginas son las del PDF (página impresa + 2).
"""
import glob
import os
import re
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops

# Ilustración principal de cada clase y especie: página del PDF
CLASES = {
    "asesino": 15, "barbaro": 17, "bardo": 18, "caballero": 20, "clerigo": 23, "druida": 24, "explorador": 26,
    "guerrero": 27, "ilusionista": 29, "ladron": 31, "mago": 32, "mistico": 34, "paladin": 36,
}
ESPECIES = {
    "elfo": 39, "elfo_oscuro": 40, "enano": 41, "gnomo": 42, "mediano": 43, "humano": 44, "semiorco": 45,
    # El libro no ilustra al semielfo: se usa la del elfo
    "semielfo": 39,
}

# Láminas de equipo: (página, tamaño de la imagen) para distinguirlas en la página
LAMINAS = {
    "armas_cuerpo": (50, (1093, 417)),
    "armas_astas": (52, (1130, 581)),
    "armas_distancia": (52, (1090, 531)),
    "armaduras": (53, (1091, 982)),
    "escudos": (54, (1090, 539)),
    "cascos": (54, (1090, 272)),
    "equipo": (57, (1090, 537)),
}

# Recortes de piezas sueltas (x0, y0, x1, y1) sobre la lámina, en píxeles de la imagen original
RECORTES = {
    "armaduras": {
        "cuero_acolchado": (35, 25, 245, 320), "cuero_endurecido": (245, 25, 405, 320),
        "cuero_tachonado": (410, 15, 620, 320), "cota_escamas": (620, 15, 800, 320),
        "anillas": (820, 15, 1040, 320), "cota_mallas": (35, 330, 250, 700),
        "laminada": (250, 375, 480, 700), "peto_coraza": (35, 710, 245, 930),
        "cota_bandas": (245, 710, 485, 940), "placas_mallas": (505, 330, 790, 940),
        "armadura_completa": (790, 315, 1075, 945),
    },
    "escudos": {
        "broquel": (0, 0, 268, 539), "escudo_medio": (268, 0, 534, 539),
        "escudo_pesado": (534, 0, 830, 539), "paves": (812, 0, 1090, 539),
    },
    "cascos": {
        "cofia_cuero": (0, 0, 175, 272), "bacinete": (175, 0, 327, 272), "celada": (327, 0, 535, 272),
        "cofia_mallas": (535, 0, 733, 272), "yelmo": (733, 0, 916, 272), "yelmo_completo": (916, 0, 1090, 272),
    },
}

ALTO_MAX = 720


def extraer_pagina(pdf, pagina, tmp):
    prefijo = os.path.join(tmp, f"p{pagina}")
    if not glob.glob(prefijo + "-*.png"):
        subprocess.run(["pdfimages", "-f", str(pagina), "-l", str(pagina), "-png", pdf, prefijo], check=True)
    imgs = []
    for f in sorted(glob.glob(prefijo + "-*.png")):
        im = Image.open(f)
        # Descarta el fondo de página (1272x1813) y las máscaras
        if im.mode in ("RGB", "RGBA") and im.size != (1272, 1813) and im.size[0] >= 300:
            imgs.append(im.convert("RGB"))
    return imgs


def recortar_blanco(im, margen=12):
    fondo = Image.new("RGB", im.size, (255, 255, 255))
    diff = ImageChops.difference(im, fondo).convert("L").point(lambda v: 255 if v > 24 else 0)
    caja = diff.getbbox()
    if not caja:
        return im
    x0, y0, x1, y1 = caja
    return im.crop((max(0, x0 - margen), max(0, y0 - margen), min(im.width, x1 + margen), min(im.height, y1 + margen)))


def blanco_transparente(im):
    """El papel blanco pasa a transparente para que la app ponga su propio fondo."""
    rgba = im.convert("RGBA")
    alfa = im.convert("L").point(lambda v: 0 if v > 242 else 255)
    rgba.putalpha(alfa)
    return rgba


def guardar(im, destino):
    im = recortar_blanco(im)
    if im.height > ALTO_MAX:
        im = im.resize((round(im.width * ALTO_MAX / im.height), ALTO_MAX), Image.LANCZOS)
    blanco_transparente(im).save(destino, "WEBP", quality=80, method=6)


def main(pdf, salida):
    os.makedirs(salida, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for prefijo, tabla in (("clase", CLASES), ("especie", ESPECIES)):
            for nombre, pagina in tabla.items():
                im = max(extraer_pagina(pdf, pagina, tmp), key=lambda i: i.width * i.height)
                guardar(im, os.path.join(salida, f"{prefijo}_{nombre}.webp"))
        for nombre, (pagina, tamano) in LAMINAS.items():
            im = next(i for i in extraer_pagina(pdf, pagina, tmp) if i.size == tamano)
            guardar(im, os.path.join(salida, f"lamina_{nombre}.webp"))
            for pieza, caja in RECORTES.get(nombre, {}).items():
                guardar(im.crop(caja), os.path.join(salida, f"objeto_{pieza}.webp"))
    total = len(os.listdir(salida))
    print(f"{total} ilustraciones -> {salida}", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
