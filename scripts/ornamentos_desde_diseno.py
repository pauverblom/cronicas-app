#!/usr/bin/env python3
"""
Genera ui/hud/OrnamentosDatos.kt a partir del bundle.js del sistema de diseño «Crónicas de la Marca».

El bundle dibuja los marcos como SVG en línea (ORN), la textura grabada (TILE) y los glifos (PATHS).
Este script los convierte en trazados SVG `d` ya transformados, listos para PathParser de Compose,
para que la app dibuje exactamente la misma geometría.

Uso:
    python3 scripts/ornamentos_desde_diseno.py ruta/a/components/bundle.js
"""
import json
import math
import re
import sys
from pathlib import Path

SALIDA = Path(__file__).resolve().parent.parent / "app/src/main/java/io/github/pauverblom/cronicas/ui/hud/OrnamentosDatos.kt"

IDENT = [1.0, 0.0, 0.0, 1.0, 0.0, 0.0]


def mul(m, n):
    """m · n para matrices afines SVG [a b c d e f]."""
    a, b, c, d, e, f = m
    a2, b2, c2, d2, e2, f2 = n
    return [a * a2 + c * b2, b * a2 + d * b2, a * c2 + c * d2, b * c2 + d * d2, a * e2 + c * f2 + e, b * e2 + d * f2 + f]


def aplicar(m, x, y):
    a, b, c, d, e, f = m
    return a * x + c * y + e, b * x + d * y + f


def transformacion(t):
    m = IDENT[:]
    for nombre, args in re.findall(r"(\w+)\(([^)]*)\)", t or ""):
        v = [float(x) for x in re.split(r"[ ,]+", args.strip()) if x]
        if nombre == "matrix":
            n = v
        elif nombre == "translate":
            n = [1, 0, 0, 1, v[0], v[1] if len(v) > 1 else 0]
        elif nombre == "scale":
            n = [v[0], 0, 0, v[1] if len(v) > 1 else v[0], 0, 0]
        else:
            raise ValueError(nombre)
        m = mul(m, n)
    return m


def num(x):
    s = f"{x:.3f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def pts(m, lista):
    return [aplicar(m, x, y) for x, y in lista]


def puntos(s):
    v = [float(x) for x in re.split(r"[ ,]+", s.strip()) if x]
    return list(zip(v[0::2], v[1::2]))


def trazado_d(m, d):
    """Transforma un `d` con comandos absolutos M L H V C Z."""
    fichas = re.findall(r"[MLHVCZ]|-?\d*\.?\d+", d)
    out, i, cmd, x, y = [], 0, None, 0.0, 0.0
    while i < len(fichas):
        f = fichas[i]
        if f.isalpha():
            cmd = f
            i += 1
            if cmd == "Z":
                out.append("Z")
            continue
        if cmd in ("M", "L"):
            x, y = float(fichas[i]), float(fichas[i + 1])
            i += 2
        elif cmd == "H":
            x = float(fichas[i])
            i += 1
        elif cmd == "V":
            y = float(fichas[i])
            i += 1
        elif cmd == "C":
            c = [float(v) for v in fichas[i:i + 6]]
            i += 6
            p = pts(m, [(c[0], c[1]), (c[2], c[3]), (c[4], c[5])])
            out.append("C" + " ".join(f"{num(a)} {num(b)}" for a, b in p))
            x, y = c[4], c[5]
            continue
        else:
            raise ValueError(cmd)
        px, py = aplicar(m, x, y)
        out.append(("M" if cmd == "M" else "L") + f"{num(px)} {num(py)}")
    return "".join(out)


def poligono(m, lista, cerrar):
    p = pts(m, lista)
    return "M" + "L".join(f"{num(a)} {num(b)}" for a, b in p) + ("Z" if cerrar else "")


def circulo(m, cx, cy, r):
    assert m == IDENT, "círculo transformado"
    return f"M{num(cx - r)} {num(cy)}A{num(r)} {num(r)} 0 1 0 {num(cx + r)} {num(cy)}A{num(r)} {num(r)} 0 1 0 {num(cx - r)} {num(cy)}Z"


def atributos(s):
    return dict(re.findall(r'([\w-]+)="([^"]*)"', s))


def primitivas(marcado, relleno_defecto=False, trazo_defecto=False, grosor_defecto=1.0):
    """Lista de (d, relleno, trazo, grosor, guiones) de un fragmento SVG."""
    pila = [IDENT]
    res = []
    for cierre, etiqueta, attrs in re.findall(r"<(/?)(\w+)([^>]*?)/?>", marcado):
        if etiqueta == "g":
            if cierre:
                pila.pop()
            else:
                pila.append(mul(pila[-1], transformacion(atributos(attrs).get("transform"))))
            continue
        if cierre:
            continue
        a = atributos(attrs)
        m = mul(pila[-1], transformacion(a.get("transform")))
        estilo = a.get("style", "")
        relleno = "fill:var(--p)" in estilo or (relleno_defecto and "fill:" not in estilo)
        trazo = "stroke:var(--p)" in estilo or (trazo_defecto and "stroke:" not in estilo)
        if etiqueta == "polygon":
            d = poligono(m, puntos(a["points"]), True)
        elif etiqueta == "polyline":
            d = poligono(m, puntos(a["points"]), False)
        elif etiqueta == "line":
            d = poligono(m, [(float(a["x1"]), float(a["y1"])), (float(a["x2"]), float(a["y2"]))], False)
        elif etiqueta == "rect":
            x, y, w, h = (float(a[k]) for k in ("x", "y", "width", "height"))
            d = poligono(m, [(x, y), (x + w, y), (x + w, y + h), (x, y + h)], True)
        elif etiqueta == "circle":
            d = circulo(m, float(a["cx"]), float(a["cy"]), float(a["r"]))
        elif etiqueta == "path":
            d = trazado_d(m, a["d"])
        else:
            continue
        guiones = a.get("stroke-dasharray")
        res.append((d, relleno, trazo, float(a.get("stroke-width", grosor_defecto)), guiones))
    return res


def kotlin_prims(prims, sangria="        "):
    lineas = []
    for d, relleno, trazo, grosor, guiones in prims:
        g = "null" if not guiones else "floatArrayOf(" + ", ".join(f"{float(v)}f" for v in guiones.split()) + ")"
        lineas.append(f'{sangria}Prim("{d}", relleno = {str(relleno).lower()}, trazo = {str(trazo).lower()}, grosor = {grosor}f, guiones = {g}),')
    return "\n".join(lineas)


def main(bundle):
    js = Path(bundle).read_text(encoding="utf-8")
    orn = json.loads(re.search(r"var ORN = (\{.*?\});\n", js, re.S).group(1))

    # La textura: TILE se construye en el bundle con rune(); se reproduce aquí
    runas = re.findall(r"'(M[^']+)'", re.search(r"var RUNES = \[(.*?)\];", js, re.S).group(1))

    def runa(i, x, y, sc):
        return f'<path transform="translate({x} {y}) scale({sc})" d="{runas[i % len(runas)]}"/>'

    tile = ('<path d="M0 60L60 0L120 60L60 120Z"/>'
            '<path d="M60 0V9M60 111V120M0 60H9M111 60H120" />'
            '<circle cx="60" cy="60" r="11"/><circle cx="60" cy="60" r="6.5" stroke-dasharray="1.5 2.5"/>'
            '<path d="M60 49L64 60L60 71L56 60Z"/>'
            + runa(0, 30, 30, 0.85) + runa(1, 90, 30, 0.85) + runa(2, 30, 90, 0.85) + runa(3, 90, 90, 0.85)
            + runa(4, 0, 0, 0.7) + runa(5, 120, 0, 0.7) + runa(5, 0, 120, 0.7) + runa(4, 120, 120, 0.7)
            + '<circle cx="60" cy="18" r="1.2"/><circle cx="60" cy="102" r="1.2"/><circle cx="18" cy="60" r="1.2"/><circle cx="102" cy="60" r="1.2"/>')
    assert "rune(0, 30, 30, 0.85)" in js, "La textura del bundle ha cambiado: revisa TILE"

    glifos = dict(re.findall(r"(\w+): '([^']+)'", re.search(r"var PATHS = \{(.*?)\};", js, re.S).group(1)))

    out = [
        "package io.github.pauverblom.cronicas.ui.hud",
        "",
        "// Generado por scripts/ornamentos_desde_diseno.py a partir del bundle.js del sistema de diseño.",
        "// No editar a mano: cambia los ornamentos en el sistema de diseño y vuelve a generar.",
        "",
        "internal val DATOS_ORNAMENTOS: Map<String, DatosOrnamento> = mapOf(",
    ]
    for nombre, o in orn.items():
        s = o["s"]
        cortes = "null" if s is None else "intArrayOf(" + ", ".join(str(v) for v in s) + ")"
        out.append(f'    "{nombre}" to DatosOrnamento(')
        out.append(f"        ancho = {o['w']}f, alto = {o['h']}f, cortes = {cortes},")
        out.append("        prims = listOf(")
        out.append(kotlin_prims(primitivas(o["b"]), "            "))
        out.append("        )")
        out.append("    ),")
    out.append(")")
    out.append("")
    out.append("/** Celosía de runas de 120 × 120 de la textura grabada (TILE). */")
    out.append("internal val TESELA_RUNAS: List<Prim> = listOf(")
    out.append(kotlin_prims(primitivas(tile, trazo_defecto=True), "    "))
    out.append(")")
    out.append("")
    out.append("/** Glifos de 24 × 24 (Material Symbols, Apache-2.0). */")
    out.append("internal val DATOS_GLIFOS: Map<String, String> = mapOf(")
    for k, d in glifos.items():
        out.append(f'    "{k}" to "{d}",')
    out.append(")")
    out.append("")
    SALIDA.parent.mkdir(parents=True, exist_ok=True)
    SALIDA.write_text("\n".join(out), encoding="utf-8")
    print(f"{SALIDA}: {len(orn)} ornamentos, {len(glifos)} glifos")


if __name__ == "__main__":
    main(sys.argv[1])
