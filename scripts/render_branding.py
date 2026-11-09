#!/usr/bin/env python3
"""Render the supplied SVG into Android's existing PNG resource sizes.

Build-time only: pip install Pillow==13.0.0 CairoSVG==2.9.1
On Homebrew macOS: DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib python scripts/render_branding.py
Android/Gradle never invokes this script or downloads artwork.
"""
from io import BytesIO
from pathlib import Path
import cairosvg
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SVG = ROOT / 'images/yt_icon.svg'
ICON_NAMES = {'app_icon', 'app_icon_alt', 'app_logo', 'app_logo_semi_grey', 'app_logo_semi_red', 'app_icon_downscaled', 'icon'}
ASSETS = sorted(ROOT.glob('smarttubetv/src/*/res/mipmap*/app_*.png'))
ASSETS += [ROOT / 'images' / name for name in ('app_icon.png', 'app_icon_downscaled.png', 'app_banner.png', 'app_logo.png')]
ASSETS += [ROOT / 'fastlane/metadata/android/en-US/images/icon.png', ROOT / 'smarttubetv/src/main/res/drawable-nodpi/loading_brand_dark.png']

for target in ASSETS:
    if target.stem not in ICON_NAMES | {'app_banner', 'loading_brand_dark'}:
        continue
    with Image.open(target) as original:
        size = original.size
    wide = target.stem in {'app_banner', 'loading_brand_dark'}
    canvas = Image.new('RGBA', size, '#0b1016' if wide else (0, 0, 0, 0))
    side = round(min(size) * (0.80 if wide else 0.94))
    art = Image.open(BytesIO(cairosvg.svg2png(url=str(SVG), output_width=side, output_height=side))).convert('RGBA')
    canvas.alpha_composite(art, ((size[0] - side) // 2, (size[1] - side) // 2))
    canvas.save(target, optimize=True)
    print(target.relative_to(ROOT), f'{size[0]}x{size[1]}')
