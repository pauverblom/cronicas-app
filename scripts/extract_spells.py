#!/usr/bin/env python3
"""Extrae los conjuros del reglamento a app/src/main/assets/conjuros.json.

Uso: python3 scripts/extract_spells.py LORE/Cronicasdelamarca.pdf app/src/main/assets/conjuros.json

Usa `pdftotext -bbox-layout` (poppler) y reordena los bloques por columna, porque el
modo de texto normal mezcla las dos columnas de la página.
"""
import html
import json
import re
import subprocess
import sys

FIRST_PAGE, LAST_PAGE = 88, 145  # páginas del PDF con "Conjuros por orden alfabético"
CLASES = {"clérigo": "clerigo", "druida": "druida", "ilusionista": "ilusionista", "mago": "mago"}
CLASE_RE = r"(clérigo|druida|ilusionista|mago)\s+nivel\s+(\d+)"
HEADER = re.compile(r"\(\s*((?:" + CLASE_RE + r"\s*,?\s*)+)\)\s*$")
FIELD = re.compile(r"^(TL|Alcance|Componentes|Duración|TS|RC)\s*:\s*(.*)$", re.S)
FIELD_ANY = re.compile(r"(TL|Alcance|Componentes|Duración|TS|RC)\s*:\s*(.*?)(?=\s*(?:TL|Alcance|Componentes|Duración|TS|RC)\s*:|$)")
FIELDS = ["tl", "alcance", "componentes", "duracion", "ts", "rc"]


def words_to_line(words):
    """Une palabras; pdftotext separa letras capitulares ("A lcance"), así que se juntan si no hay hueco."""
    out, prev_x = "", None
    for x0, x1, text in words:
        if prev_x is not None and x0 - prev_x > 1.2:
            out += " "
        out += text
        prev_x = x1
    return out


def read_blocks(pdf):
    xml = subprocess.run(
        ["pdftotext", "-bbox-layout", "-f", str(FIRST_PAGE), "-l", str(LAST_PAGE), pdf, "-"],
        check=True, capture_output=True, text=True,
    ).stdout
    blocks = []
    for width, body in re.findall(r'<page width="([\d.]+)"[^>]*>(.*?)</page>', xml, re.S):
        mid = float(width) / 2
        page = []
        for bx, by, inner in re.findall(r'<block xMin="([\d.]+)" yMin="([\d.]+)"[^>]*>(.*?)</block>', body, re.S):
            lines = []
            for line in re.findall(r"<line[^>]*>(.*?)</line>", inner, re.S):
                words = [
                    (float(a), float(b), html.unescape(t))
                    for a, b, t in re.findall(r'<word xMin="([\d.]+)" yMin="[\d.]+" xMax="([\d.]+)"[^>]*>([^<]*)</word>', line)
                ]
                lines.append(words_to_line(words))
            text = "\n".join(lines).strip()
            if not text or re.fullmatch(r"\d+|[A-ZÁÉÍÓÚ]", text):
                continue  # número de página o letra de sección
            page.append((0 if float(bx) < mid else 1, float(by), text))
        page.sort()
        blocks.extend(t for _, _, t in page)
    return blocks


def join_text(text):
    """Une líneas de párrafo deshaciendo los guiones de corte de línea."""
    out = ""
    for line in text.split("\n"):
        line = line.strip()
        if not line:
            continue
        if out.endswith(" -"):
            out = out[:-2] + line
        elif out.endswith("-"):
            out = out[:-1] + line
        else:
            out = f"{out} {line}" if out else line
    return out


def find_header(blocks, i):
    """La cabecera "Nombre (clase nivel N, ...)" ocupa los 1-3 bloques anteriores al bloque i.

    Devuelve (nombre, clases, bloques que ocupa, texto de descripción sobrante del primero) o None.
    """
    for k in range(1, 4):
        if i - k < 0:
            return None
        lines = "\n".join(blocks[i - k:i]).split("\n")
        if not HEADER.search(" ".join(lines)):
            continue
        # Las líneas hasta la que abre "(clase nivel" forman (descripción sobrante +) nombre
        head = []
        for line in lines:
            pos = re.search(r"\(\s*(clérigo|druida|ilusionista|mago)\b", line)
            if pos:
                before = line[:pos.start()].strip()
                if before:
                    head.append(before)
                break
            head.append(line.strip())
        name = []
        while head and head[-1] and not head[-1].endswith((".", ":", ";")) and len(head[-1]) <= 60:
            name.insert(0, head.pop())
        if not name:
            continue
        m = HEADER.search(" ".join(lines))
        clases = {CLASES[c]: int(n) for c, n in re.findall(CLASE_RE, m.group(1))}
        leftover = "\n".join(head).strip() or None
        return " ".join(name), clases, k, leftover
    return None


def main(pdf, out):
    blocks = read_blocks(pdf)
    spells, desc = [], []

    def close_last():
        if spells:
            spells[-1]["descripcion"] = join_text("\n".join(desc))

    i = 0
    while i < len(blocks):
        if FIELD.match(blocks[i]) and not FIELD.match(blocks[i - 1]):
            hdr = find_header(blocks, i)
            if hdr:
                name, clases, k, leftover = hdr
                del desc[max(0, len(desc) - k):]
                if leftover:
                    desc.append(leftover)
                close_last()
                desc = []
                spell = {"nombre": re.sub(r"\s+", " ", name), "clases": clases}
                # Los campos pueden venir en bloques sueltos o varios en un mismo bloque
                fields = []
                while i < len(blocks) and FIELD.match(blocks[i]):
                    fields.append(blocks[i])
                    i += 1
                flat = re.sub(r"\s+", " ", " ".join(fields))
                for key, val in FIELD_ANY.findall(flat):
                    spell[key.lower().replace("ó", "o")] = val.strip()
                for f in FIELDS:
                    spell.setdefault(f, "")
                spells.append(spell)
                continue
        if spells:
            desc.append(blocks[i])
        i += 1
    close_last()

    by_name = {s["nombre"]: s for s in spells}
    for s in spells:
        s["descripcion"] = re.sub(r"\s*Capítulo 9:.*", "", s["descripcion"], flags=re.S)
    # La descripción de Rociada prismática salta de columna por debajo de la cabecera de Salto
    salto, rociada = by_name.get("Salto"), by_name.get("Rociada prismática")
    if salto and rociada and not rociada["descripcion"] and "Siete rayos" in salto["descripcion"]:
        cut = salto["descripcion"].index("Siete rayos")
        salto["descripcion"], rociada["descripcion"] = salto["descripcion"][:cut].strip(), salto["descripcion"][cut:]
    spells.sort(key=lambda s: s["nombre"].lower())
    with open(out, "w", encoding="utf-8") as fh:
        json.dump(spells, fh, ensure_ascii=False, indent=1)
    print(f"{len(spells)} conjuros -> {out}", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
