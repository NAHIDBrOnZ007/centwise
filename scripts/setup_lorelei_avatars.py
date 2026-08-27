import os
import json
import urllib.request
from PIL import Image
import io

def setup_lorelei_avatars():
    # 10 vibrant, colorful character seeds (5 male, 5 female)
    avatars = [
        ("avatar_1", "Felix", "male"),
        ("avatar_2", "Aneka", "female"),
        ("avatar_3", "Alexander", "male"),
        ("avatar_4", "Sophia", "female"),
        ("avatar_5", "Oliver", "male"),
        ("avatar_6", "Maya", "female"),
        ("avatar_7", "Liam", "male"),
        ("avatar_8", "Elena", "female"),
        ("avatar_9", "Noah", "male"),
        ("avatar_10", "Aria", "female"),
    ]

    ios_root = "apps/ios/Centwise/Assets.xcassets"
    android_root = "apps/android/app/src/main/res/drawable"
    preview_root = "preview"

    os.makedirs(ios_root, exist_ok=True)
    os.makedirs(android_root, exist_ok=True)
    os.makedirs(preview_root, exist_ok=True)

    print("Downloading 10 Colorful Modern Avatars (DiceBear Lorelei)...")

    for name, seed, gender in avatars:
        # DiceBear Lorelei high-resolution PNG
        url = f"https://api.dicebear.com/9.x/lorelei/png?seed={seed}&size=512"
        req = urllib.request.Request(
            url,
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )

        try:
            with urllib.request.urlopen(req) as resp:
                data = resp.read()
                img = Image.open(io.BytesIO(data)).convert("RGBA")

                # 1. iOS Assets Imageset (.imageset)
                imageset_path = os.path.join(ios_root, f"{name}.imageset")
                os.makedirs(imageset_path, exist_ok=True)

                # Save 256x256 high-res PNG
                png_path = os.path.join(imageset_path, f"{name}.png")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(png_path, "PNG")

                # Write Contents.json for Xcode
                contents_dict = {
                    "images": [
                        {
                            "filename": f"{name}.png",
                            "idiom": "universal",
                            "scale": "1x"
                        }
                    ],
                    "info": {
                        "author": "xcode",
                        "version": 1
                    }
                }
                with open(os.path.join(imageset_path, "Contents.json"), "w") as f:
                    json.dump(contents_dict, f, indent=2)

                # 2. Android drawable
                android_png = os.path.join(android_root, f"{name}.png")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(android_png, "PNG")

                # 3. Web Preview
                preview_png = os.path.join(preview_root, f"{name}.png")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(preview_png, "PNG")

                print(f"[OK] {name} downloaded successfully ({gender} - {seed})")

        except Exception as e:
            print(f"[ERROR] Failed to download {name}: {e}")

    print("\nAll 10 Colorful Lorelei Avatars deployed to iOS and Android!")

if __name__ == "__main__":
    setup_lorelei_avatars()
