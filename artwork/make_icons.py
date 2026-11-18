"""Build the adaptive, monochrome and legacy launcher icons from the same paths."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
from PIL import Image, ImageDraw

RES = Path(__file__).resolve().parents[1] / 'app/src/main/res'
A = 'http://schemas.android.com/apk/res/android'
# A pair of soft message tiles; the M is a single closed, rounded outline.
BACK = 'M46.8,26.6 L60.5,26.6 C73.6,26.6 81.6,34.9 81.6,47.2 L81.6,52.2 C81.6,63.5 73.4,71.6 62.2,71.6 L46.1,71.6 C34.9,71.6 28.5,64.5 28.5,53.5 L28.5,46 C28.5,34.3 35.6,26.6 46.8,26.6 Z'
FRONT = 'M28,50.1 C28,38.5 35.7,31.4 47.3,31.4 L54.5,31.4 C67.9,31.4 76.4,40 76.4,53.4 L76.4,55.9 C76.4,67.8 68.8,75.1 57,75.1 L45.1,75.1 C42.8,75.1 41.6,75.8 39.9,76.7 L34.6,79.3 C32.3,80.4 30.7,79.1 31.1,76.8 L32.1,69.1 C29.2,65.9 28,61.6 28,56.1 Z'
M = 'M39.1,62 L39.1,46 C39.1,43.6 41.8,42.3 43.7,43.9 L52.2,51.4 L60.8,43.9 C62.6,42.3 65.3,43.6 65.3,46 L65.3,62 C65.3,65.5 59.9,65.5 59.9,62 L59.9,52.1 L54.1,57.2 C53,58.2 51.5,58.2 50.3,57.2 L44.5,52.1 L44.5,62 C44.5,65.5 39.1,65.5 39.1,62 Z'
BG, REAR, INK, GLYPH = '#ECE1FF', '#B79DD7', '#64498A', '#FFFAFF'


def save(rel, text):
    p = RES / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text + '\n', encoding='utf-8')


def path(d, color, hollow=False):
    rule = ' android:fillType="evenOdd"' if hollow else ''
    return f'<path android:fillColor="{color}"{rule} android:pathData="{d}" />'


def vector(elements, viewport=108):
    return (f'<vector xmlns:android="{A}" android:width="{viewport}dp" android:height="{viewport}dp" '
            f'android:viewportWidth="{viewport}" android:viewportHeight="{viewport}">\n{elements}\n</vector>')


save('drawable/launcher_color.xml', vector('\n'.join([path(BACK, REAR), path(FRONT, INK), path(M, GLYPH)])))
save('drawable/launcher_mono.xml', vector(path(FRONT + ' ' + M, '#FFFFFFFF', True)))
# The inline mark must also work on API 23 (evenOdd became public in API 24).
# Render its M as a separate Android theme role instead of relying on a fill rule.
save('drawable/inline_mark.xml', vector('<group android:translateX="-24" android:translateY="-27">\n' +
     path(FRONT, '?attr/colorPrimary') + '\n' + path(M, '?attr/colorOnPrimary') + '\n</group>', 56))
for version in (26, 33):
    mono = '    <monochrome android:drawable="@drawable/launcher_mono" />\n' if version >= 33 else ''
    save(f'mipmap-anydpi-v{version}/ic_launcher.xml', f'<adaptive-icon xmlns:android="{A}">\n'
         '    <background android:drawable="@color/launcher_surface" />\n'
         f'    <foreground android:drawable="@drawable/launcher_color" />\n{mono}</adaptive-icon>')


def polygon(data):
    tokens = iter(re.findall(r'[MLCZ]|-?(?:\d*\.)?\d+', data))
    def pair(): return (float(next(tokens)), float(next(tokens)))
    pts = []
    for op in tokens:
        if op in ('M', 'L'):
            p = pair(); pts.append(p)
        elif op == 'C':
            a, b, c = pair(), pair(), pair()
            for i in range(1, 41):
                t = i / 40; u = 1 - t
                pts.append(tuple(u*u*u*p[k] + 3*t*u*u*a[k] + 3*t*t*u*b[k] + t*t*t*c[k] for k in range(2)))
            p = c
        elif op != 'Z': raise ValueError(op)
    return pts


N = 1024
im = Image.new('RGBA', (N, N)); d = ImageDraw.Draw(im)
s = N / 76; margin = round(2 * s)
d.rounded_rectangle((margin, margin, N-margin-1, N-margin-1), radius=round(17*s), fill=BG)
for contour, fill in [(BACK, REAR), (FRONT, INK), (M, GLYPH)]:
    d.polygon([((x-16)*s, (y-16)*s) for x,y in polygon(contour)], fill=fill)
for dpi, size in [('mdpi',48), ('hdpi',72), ('xhdpi',96), ('xxhdpi',144), ('xxxhdpi',192)]:
    out = RES / f'mipmap-{dpi}' / 'ic_launcher.png'; out.parent.mkdir(exist_ok=True)
    im.resize((size, size), Image.Resampling.LANCZOS).save(out, optimize=True)
