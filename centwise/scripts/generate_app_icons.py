import os
from PIL import Image

def generate_icons():
    source_logo = "centwise/centwise logo.jpeg"
    if not os.path.exists(source_logo):
        print(f"Error: {source_logo} not found")
        return

    img = Image.open(source_logo).convert("RGBA")

    # 1. iOS AppIcon sizes
    ios_icon_dir = "centwise/apps/ios/Centwise/Assets.xcassets/AppIcon.appiconset"
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
        print(f"Generated iOS AppIcon: {out_path} ({size[0]}x{size[1]})")

    # 2. iOS AppLogo
    logo_dir = "centwise/apps/ios/Centwise/Assets.xcassets/AppLogo.imageset"
    os.makedirs(logo_dir, exist_ok=True)
    img.resize((180, 180), Image.Resampling.LANCZOS).save(os.path.join(logo_dir, "icon_180x180.png"), "PNG")
    print(f"Generated AppLogo in {logo_dir}")

    # 3. Avatar Mascot for GreetingCard
    avatar_dir = "centwise/apps/ios/Centwise/Assets.xcassets/avatar_1.imageset"
    os.makedirs(avatar_dir, exist_ok=True)
    img.resize((128, 128), Image.Resampling.LANCZOS).save(os.path.join(avatar_dir, "avatar_1.png"), "PNG")
    print(f"Generated Avatar in {avatar_dir}")

    # 4. Preview Logo
    preview_dir = "centwise/preview"
    os.makedirs(preview_dir, exist_ok=True)
    img.resize((128, 128), Image.Resampling.LANCZOS).save(os.path.join(preview_dir, "centwise_logo.png"), "PNG")
    img.resize((128, 128), Image.Resampling.LANCZOS).save(os.path.join(preview_dir, "avatar_1.png"), "PNG")
    print(f"Generated Preview Logos in {preview_dir}")

    # 5. Android Mipmap Icons
    android_res = "centwise/apps/android/app/src/main/res"
    android_sizes = [
        ("mipmap-mdpi", (48, 48)),
        ("mipmap-hdpi", (72, 72)),
        ("mipmap-xhdpi", (96, 96)),
        ("mipmap-xxhdpi", (144, 144)),
        ("mipmap-xxxhdpi", (192, 192)),
    ]

    for folder, size in android_sizes:
        target_dir = os.path.join(android_res, folder)
        os.makedirs(target_dir, exist_ok=True)
        out_file = os.path.join(target_dir, "ic_launcher.png")
        img.resize(size, Image.Resampling.LANCZOS).save(out_file, "PNG")
        # Round icon as well
        img.resize(size, Image.Resampling.LANCZOS).save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")
        print(f"Generated Android Icon: {out_file} ({size[0]}x{size[1]})")

    print("\nAll icon sizes successfully generated from centwise logo.jpeg!")

if __name__ == "__main__":
    generate_icons()
