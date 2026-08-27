import os
from PIL import Image, ImageDraw

def generate_icons():
    source_logo = "centwise logo.jpeg"
    if not os.path.exists(source_logo):
        source_logo = os.path.join("centwise", "centwise logo.jpeg")
    if not os.path.exists(source_logo):
        print(f"Error: {source_logo} not found")
        return

    img = Image.open(source_logo).convert("RGBA")

    # 1. iOS AppIcon sizes
    ios_icon_dir = "apps/ios/Centwise/Assets.xcassets/AppIcon.appiconset"
    if not os.path.exists(ios_icon_dir):
        ios_icon_dir = os.path.join("centwise", ios_icon_dir)
    os.makedirs(ios_icon_dir, exist_ok=True)

    ios_sizes = [
        ("icon_20x20.png", (20, 20)),
        ("icon_29x29.png", (29, 29)),
        ("icon_40x40.png", (40, 40)),
        ("icon_58x58.png", (58, 58)),
        ("icon_60x60.png", (60, 60)),
        ("icon_76x76.png", (76, 76)),
        ("icon_80x80.png", (80, 80)),
        ("icon_87x87.png", (87, 87)),
        ("icon_120x120.png", (120, 120)),
        ("icon_152x152.png", (152, 152)),
        ("icon_167x167.png", (167, 167)),
        ("icon_180x180.png", (180, 180)),
        ("icon_1024x1024.png", (1024, 1024)),
    ]

    for filename, size in ios_sizes:
        resized = img.resize(size, Image.Resampling.LANCZOS)
        out_path = os.path.join(ios_icon_dir, filename)
        resized.save(out_path, "PNG")

    # 2. Android Mipmap Icons & Adaptive Foreground
    android_res = "apps/android/app/src/main/res"
    if not os.path.exists(android_res):
        android_res = os.path.join("centwise", android_res)

    android_densities = [
        ("mipmap-mdpi", (48, 48), (108, 108), (72, 72)),
        ("mipmap-hdpi", (72, 72), (162, 162), (108, 108)),
        ("mipmap-xhdpi", (96, 96), (216, 216), (144, 144)),
        ("mipmap-xxhdpi", (144, 144), (324, 324), (216, 216)),
        ("mipmap-xxxhdpi", (192, 192), (432, 432), (288, 288)),
    ]

    for folder, legacy_size, fg_canvas_size, fg_logo_size in android_densities:
        target_dir = os.path.join(android_res, folder)
        os.makedirs(target_dir, exist_ok=True)

        # Standard legacy icon
        legacy_icon = img.resize(legacy_size, Image.Resampling.LANCZOS)
        legacy_icon.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")

        # Round legacy icon
        mask = Image.new("L", legacy_size, 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, legacy_size[0], legacy_size[1]), fill=255)
        round_icon = Image.new("RGBA", legacy_size, (0, 0, 0, 0))
        round_icon.paste(legacy_icon, (0, 0), mask=mask)
        round_icon.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

        # Adaptive icon foreground (108dp canvas with centered ~72dp mascot)
        fg_canvas = Image.new("RGBA", fg_canvas_size, (0, 0, 0, 0))
        fg_logo = img.resize(fg_logo_size, Image.Resampling.LANCZOS)
        offset_x = (fg_canvas_size[0] - fg_logo_size[0]) // 2
        offset_y = (fg_canvas_size[1] - fg_logo_size[1]) // 2
        fg_canvas.paste(fg_logo, (offset_x, offset_y))
        fg_canvas.save(os.path.join(target_dir, "ic_launcher_foreground.png"), "PNG")

        print(f"Generated Android icons in {folder}")

    print("\nAll icon sizes and adaptive foregrounds successfully generated!")

if __name__ == "__main__":
    generate_icons()
