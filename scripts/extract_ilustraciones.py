#!/usr/bin/env python3
"""Extrae las ilustraciones de los reglamentos a app/src/main/assets/ilustraciones/.

Uso:
    python3 scripts/extract_ilustraciones.py LORE/Cronicasdelamarca.pdf LORE/caja_roja.pdf \\
        app/src/main/assets/ilustraciones

Necesita `pdfimages` (poppler) y Pillow. Las páginas son las del PDF, no las impresas.

- Clases y especies: la ilustración principal de su página en Crónicas de la Marca.
- Objetos: un archivo objeto_<id de catálogo>.webp por artículo. Se sacan de las láminas de equipo de
  Crónicas (recortando cada dibujo por sus propios trazos, no por un rectángulo) y, donde Crónicas no lo
  dibuja suelto, de las ilustraciones rotuladas de la caja roja (Aventuras en la Marca del Este).
- Láminas: lamina_<categoría>.webp, para los artículos sin dibujo propio.
"""
import glob
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops, ImageFilter

CLASES = {
    "asesino": 15, "barbaro": 17, "bardo": 18, "caballero": 20, "clerigo": 23, "druida": 24, "explorador": 26,
    "guerrero": 27, "ilusionista": 29, "ladron": 31, "mago": 32, "mistico": 34, "paladin": 36,
}
ESPECIES = {
    "elfo": 39, "elfo_oscuro": 40, "enano": 41, "gnomo": 42, "mediano": 43, "humano": 44, "semiorco": 45,
    # El libro no ilustra al semielfo: se usa la del elfo
    "semielfo": 39,
}

# Láminas de Crónicas: (página, tamaño de la imagen) para distinguirlas dentro de la página
LAMINAS = {
    "armas_cuerpo": (50, (1093, 417)),
    "armas_varias": (50, (489, 529)),
    "armas_astas": (52, (1130, 581)),
    "armas_distancia": (52, (1090, 531)),
    "armaduras": (53, (1091, 982)),
    "escudos": (54, (1090, 539)),
    "cascos": (54, (1090, 272)),
    "equipo": (57, (1090, 537)),
}

# Piezas rotuladas que se recortan por rectángulo (llevan su valor de CA dibujado)
RECTANGULOS = {
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

# Dibujos sueltos dentro de las láminas: se toman los trazos cuya caja cae dentro de estas zonas
TRAZOS = {
    "armas_cuerpo": {
        "daga": [(16, 72, 76, 206)], "espada_corta": [(82, 58, 122, 252)], "espada_larga": [(136, 46, 200, 326)],
        "espada_bastarda": [(202, 28, 270, 350)], "espada_dos_manos": [(276, 22, 362, 396)],
        "cimitarra": [(366, 56, 424, 304)], "hacha_mano": [(542, 124, 616, 326)], "gran_hacha": [(600, 12, 710, 332)],
        "maza": [(704, 104, 750, 324)], "lucero_alba": [(762, 92, 818, 326)], "martillo_guerra": [(818, 58, 900, 324)],
        "martillo_ligero": [(876, 106, 970, 330)], "garrote": [(1008, 90, 1062, 332)], "cachiporra": [(968, 186, 996, 324)],
    },
    "armas_varias": {"hoz": [(158, 48, 256, 184)]},
    "armas_astas": {
        "latigo": [(18, 46, 116, 308)], "pica": [(222, 4, 278, 580)], "baston": [(182, 238, 204, 576)],
        "alabarda": [(340, 126, 432, 578)], "jabalina": [(564, 230, 584, 574)], "tridente": [(720, 168, 770, 580)],
        "guadana": [(800, 104, 856, 580)],
    },
    "armas_distancia": {
        "arco_largo": [(54, 50, 132, 474)], "arco_corto": [(156, 188, 222, 468)],
        "ballesta_repeticion": [(566, 66, 734, 308)], "ballesta_mano": [(560, 304, 662, 464)],
        "virotes": [(650, 258, 740, 456), (732, 282, 760, 470)],
        "honda": [(792, 282, 888, 368), (796, 394, 878, 466)], "dardo": [(908, 312, 944, 446)],
        "cerbatana": [(976, 292, 1054, 468)],
    },
    # La lámina de equipo sigue el orden de la lista de equipo del libro
    "equipo": {
        "aceite": [(46, 28, 90, 112)], "ajo": [(94, 60, 176, 118)], "agua_bendita": [(190, 32, 238, 114)],
        "alforjas": [(246, 34, 408, 118)], "antorchas": [(428, 10, 506, 148)], "barra": [(502, 12, 558, 146)],
        "botella": [(562, 28, 618, 134)], "cadena": [(626, 34, 750, 128)], "catalejo": [(732, 34, 820, 130)],
        "cerrojo": [(830, 36, 926, 130)], "cuerda_canamo": [(942, 42, 1044, 120)], "escarpias": [(16, 154, 92, 228)],
        "espejo": [(114, 136, 176, 254)], "estacas": [(198, 140, 238, 252)], "estuche": [(248, 144, 362, 248)],
        "farol": [(384, 156, 452, 260)], "pertiga": [(456, 216, 590, 436)], "frasco": [(484, 166, 526, 234)],
        "grilletes": [(532, 166, 670, 258)], "herramientas_ladron": [(662, 176, 818, 248)],
        "libro_conjuros": [(832, 156, 944, 260)], "manta": [(954, 160, 1072, 244)], "martillo": [(30, 254, 90, 354)],
        "mochila": [(114, 264, 210, 374)], "odre": [(220, 272, 288, 364)], "pala": [(300, 248, 370, 422)],
        "pergamino": [(370, 282, 470, 370)], "petate": [(568, 240, 668, 378)], "pica_escalada": [(676, 262, 758, 366)],
        "pluma": [(776, 254, 814, 376)], "polea": [(842, 286, 918, 358)], "puas": [(928, 306, 1000, 354)],
        "saco_grande": [(16, 370, 136, 512)], "saco_pequeno": [(148, 402, 200, 492)],
        "simbolo_madera": [(206, 404, 306, 524)], "simbolo_plata": [(244, 366, 340, 478)],
        "tinta": [(358, 450, 408, 508)], "velas": [(436, 410, 530, 524)], "vial": [(620, 422, 652, 490)],
        "yesca": [(676, 400, 822, 486)],
    },
}

# Ilustraciones rotuladas de la caja roja: (página, tamaño, giro en grados en sentido antihorario)
CAJA_ROJA = {
    "hacha_batalla": (41, (278, 704), 0),     # "Hacha de batalla"
    "daga_plata": (41, (300, 222), 0),        # "Puñal"
    "lanza": (40, (765, 108), 270),           # "Lanza corta", impresa en vertical
    "lanza_caballeria": (40, (108, 997), 0),  # "Lanza larga"
    "ballesta_ligera": (40, (541, 619), 0),   # ballestero
    "flechas": (40, (194, 315), 0),           # "Flechas para arco corto"
    "camisote_mallas": (42, (186, 244), 0),   # "Armadura de cota de mallas" (camisote, sin capucha)
}

# Artículos sin dibujo propio que comparten el de una variante muy parecida
VARIANTES = {
    "arco_corto_compuesto": "arco_corto", "arco_largo_compuesto": "arco_largo",
    "ballesta_pesada": "ballesta_ligera", "virotes_ligeros": "virotes", "dardos_cerbatana": "cerbatana",
    "cuerda_seda": "cuerda_canamo", "escudo_pequeno_madera": "broquel", "escudo_pequeno_metal": "broquel",
    "escudo_medio_madera": "escudo_medio", "escudo_medio_metal": "escudo_medio",
    "escudo_pesado_madera": "escudo_pesado", "escudo_pesado_metal": "escudo_pesado",
}

ALTO_MAX = 720


def imagenes_pagina(pdf, pagina, tmp):
    prefijo = os.path.join(tmp, f"{os.path.basename(pdf)}-{pagina}")
    if not glob.glob(prefijo + "-*.png"):
        subprocess.run(["pdfimages", "-f", str(pagina), "-l", str(pagina), "-png", pdf, prefijo], check=True)
    out = []
    for f in sorted(glob.glob(prefijo + "-*.png")):
        im = Image.open(f)
        if im.size not in ((1272, 1813), (1164, 1700)):  # fondos de página
            out.append(im)
    return out


def imagen(pdf, pagina, tamano, tmp):
    return next(i for i in imagenes_pagina(pdf, pagina, tmp) if i.size == tamano).convert("RGB")


def recortar_blanco(im, margen=12):
    """Recorta el papel sobrante alrededor del dibujo (con o sin canal alfa)."""
    if im.mode == "RGBA":
        caja = im.getchannel("A").getbbox()
    else:
        fondo = Image.new("RGB", im.size, (255, 255, 255))
        caja = ImageChops.difference(im, fondo).convert("L").point(lambda v: 255 if v > 24 else 0).getbbox()
    if not caja:
        return im
    x0, y0, x1, y1 = caja
    return im.crop((max(0, x0 - margen), max(0, y0 - margen), min(im.width, x1 + margen), min(im.height, y1 + margen)))


def blanco_transparente(im):
    """El papel blanco pasa a transparente para que la app ponga su propio fondo."""
    rgba = im.convert("RGBA")
    alfa = im.convert("L").point(lambda v: 0 if v > 242 else 255)
    if im.mode == "RGBA":
        alfa = ImageChops.multiply(alfa, im.getchannel("A"))
    rgba.putalpha(alfa)
    return rgba


def guardar(im, destino):
    im = recortar_blanco(blanco_transparente(im))
    if im.height > ALTO_MAX:
        im = im.resize((round(im.width * ALTO_MAX / im.height), ALTO_MAX), Image.LANCZOS)
    im.save(destino, "WEBP", quality=80, method=6)


def componentes(im):
    """Trazos conectados (a mitad de resolución, engordados para unir sombreados). Devuelve (caja, píxeles)."""
    k = 2
    sm = im.convert("L").resize((im.width // k, im.height // k))
    mask = sm.point(lambda v: 255 if v < 170 else 0).filter(ImageFilter.MaxFilter(3))
    w, h = mask.size
    px = mask.load()
    visto = bytearray(w * h)
    out = []
    for y in range(h):
        for x in range(w):
            if not px[x, y] or visto[y * w + x]:
                continue
            pila, puntos = [(x, y)], []
            visto[y * w + x] = 1
            while pila:
                cx, cy = pila.pop()
                puntos.append((cx, cy))
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if 0 <= nx < w and 0 <= ny < h and px[nx, ny] and not visto[ny * w + nx]:
                        visto[ny * w + nx] = 1
                        pila.append((nx, ny))
            if len(puntos) > 60:
                xs = [p[0] for p in puntos]
                ys = [p[1] for p in puntos]
                caja = (min(xs) * k, min(ys) * k, (max(xs) + 1) * k, (max(ys) + 1) * k)
                out.append((caja, puntos))
    return out


def recortar_trazos(im, comps, zonas, tolerancia=6):
    """Solo los trazos cuya caja cae dentro de alguna zona; el resto de la lámina queda transparente."""
    k = 2
    mascara = Image.new("L", (im.width // k, im.height // k), 0)
    mp = mascara.load()
    for caja, puntos in comps:
        if any(caja[0] >= z[0] - tolerancia and caja[1] >= z[1] - tolerancia and
               caja[2] <= z[2] + tolerancia and caja[3] <= z[3] + tolerancia for z in zonas):
            for x, y in puntos:
                mp[x, y] = 255
    mascara = mascara.resize(im.size, Image.NEAREST).filter(ImageFilter.MaxFilter(3))
    rgba = im.convert("RGBA")
    rgba.putalpha(mascara)
    return rgba


def main(cronicas, caja_roja, salida):
    os.makedirs(salida, exist_ok=True)
    escritos = set()
    with tempfile.TemporaryDirectory() as tmp:
        for prefijo, tabla in (("clase", CLASES), ("especie", ESPECIES)):
            for nombre, pagina in tabla.items():
                ims = [i.convert("RGB") for i in imagenes_pagina(cronicas, pagina, tmp) if i.mode in ("RGB", "RGBA")]
                guardar(max(ims, key=lambda i: i.width * i.height), os.path.join(salida, f"{prefijo}_{nombre}.webp"))

        for nombre, (pagina, tamano) in LAMINAS.items():
            im = imagen(cronicas, pagina, tamano, tmp)
            if nombre != "armas_varias":
                guardar(im, os.path.join(salida, f"lamina_{nombre}.webp"))
            for pieza, caja in RECTANGULOS.get(nombre, {}).items():
                guardar(im.crop(caja), os.path.join(salida, f"objeto_{pieza}.webp"))
                escritos.add(pieza)
            if nombre in TRAZOS:
                comps = componentes(im)
                for pieza, zonas in TRAZOS[nombre].items():
                    guardar(recortar_trazos(im, comps, zonas), os.path.join(salida, f"objeto_{pieza}.webp"))
                    escritos.add(pieza)

        for pieza, (pagina, tamano, giro) in CAJA_ROJA.items():
            im = imagen(caja_roja, pagina, tamano, tmp)
            if giro:
                im = im.rotate(giro, expand=True, fillcolor=(255, 255, 255))
            guardar(im, os.path.join(salida, f"objeto_{pieza}.webp"))
            escritos.add(pieza)

    for pieza, origen in VARIANTES.items():
        with open(os.path.join(salida, f"objeto_{origen}.webp"), "rb") as f:
            datos = f.read()
        with open(os.path.join(salida, f"objeto_{pieza}.webp"), "wb") as f:
            f.write(datos)
        escritos.add(pieza)

    print(f"{len(os.listdir(salida))} ilustraciones, {len(escritos)} objetos -> {salida}", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])
