import os
from datetime import date
import re

NOMBRE = "Pablo Daniel de Luca"
DNI = "31.649.936"
EMPRESA = "Ink 318 Software"
AÑO = 2026

AVISO = (
    f"© {AÑO} {NOMBRE} - {EMPRESA}. Todos los derechos reservados.\n"
    f"DNI: {DNI}\n"
    f"Este archivo es propiedad exclusiva de {NOMBRE} / {EMPRESA}.\n"
    "Queda prohibida su reproducción, distribución, modificación, venta o uso "
    "total o parcial sin autorización expresa y por escrito del titular.\n"
)

COMENTARIOS = {
    ".py": ("# ", ""),
    ".js": ("// ", ""),
    ".ts": ("// ", ""),
    ".jsx": ("// ", ""),
    ".tsx": ("// ", ""),
    ".java": ("// ", ""),
    ".kt": ("// ", ""),
    ".kts": ("// ", ""),
    ".c": ("// ", ""),
    ".cpp": ("// ", ""),
    ".h": ("// ", ""),
    ".cs": ("// ", ""),
    ".php": ("// ", ""),
    ".rb": ("# ", ""),
    ".sh": ("# ", ""),
    ".bash": ("# ", ""),
    ".zsh": ("# ", ""),
    ".ps1": ("# ", ""),
    ".html": ("<!-- ", " -->"),
    ".xml": ("<!-- ", " -->"),
    ".css": ("/* ", " */"),
    ".scss": ("// ", ""),
    ".sql": ("-- ", ""),
    ".yml": ("# ", ""),
    ".yaml": ("# ", ""),
    ".toml": ("# ", ""),
    ".ini": ("; ", ""),
    ".conf": ("# ", ""),
    ".md": ("<!-- ", " -->"),
    ".txt": ("", ""),
}

SIN_COMENTARIO = {".json", ".min.js", ".min.css"}

EXCLUIR = {'.git', 'node_modules', '__pycache__', 'venv', '.venv', 'dist', 'build', '.gradle'}

def formatear_aviso(ext):
    inicio, fin = COMENTARIOS.get(ext, ("# ", ""))
    lineas = AVISO.strip().splitlines()

    if not inicio and not fin:   
        return AVISO + "\n"

    comentado = "\n".join(f"{inicio}{linea}{fin}" for linea in lineas)
    return comentado + "\n\n"

old_comment_pattern = re.compile(r'/\*\s*\*\s*Copyright.*?Todos los derechos reservados\.\s*\*/\s*', re.DOTALL)

for root, dirs, files in os.walk('.'):
    dirs[:] = [d for d in dirs if d not in EXCLUIR and not d.startswith('.')]
    if 'build' in dirs:
        dirs.remove('build')

    for file in files:
        ext = os.path.splitext(file)[1].lower()
        ruta = os.path.join(root, file)
        
        if "/build/" in ruta.replace("\\", "/"):
            continue

        if ext in SIN_COMENTARIO:
            copyright_path = os.path.join(root, "COPYRIGHT.txt")
            if not os.path.exists(copyright_path):
                with open(copyright_path, 'w', encoding='utf-8') as f:
                    f.write(AVISO)
                print(f"Creado {copyright_path}")
            continue

        if ext not in COMENTARIOS:
            continue

        try:
            with open(ruta, 'r', encoding='utf-8') as f:
                contenido = f.read()
        except UnicodeDecodeError:
            print(f"Saltado (binario): {ruta}")
            continue

        if "Queda prohibida su reproducción" in contenido:
            print(f"Ya tiene aviso: {ruta}")
            continue
            
        contenido = old_comment_pattern.sub('', contenido)

        aviso_formateado = formatear_aviso(ext)

        with open(ruta, 'w', encoding='utf-8') as f:
            f.write(aviso_formateado + contenido)

        print(f"Protegido: {ruta}")
