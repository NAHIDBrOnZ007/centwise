import os
import json
import urllib.request
from PIL import Image
import io

def setup_personas_avatars():
    # 10 diverse, smiling, modern colorful avatars
    avatars = [
        ("avatar_1", "Oliver", "male"),
        ("avatar_2", "Sophia", "female"),
        ("avatar_3", "Lucas", "male"),
        ("avatar_4", "Emma", "female"),
        ("avatar_5", "Ethan", "male"),
        ("avatar_6", "Ava", "female"),
        ("avatar_7", "Mason", "male"),
        ("avatar_8", "Isabella", "female"),
        ("avatar_9", "Aiden", "male"),
        ("avatar_10", "Mia", "female"),
    ]

    ios_root = "apps/ios/Centwise/Assets.xcassets"
    android_root = "apps/android/app/src/main/res/drawable"
    preview_root = "preview"

    os.makedirs(ios_root, exist_ok=True)
    os.makedirs(android_root, exist_ok=True)
    os.makedirs(preview_root, exist_ok=True)

    print("Deploying 10 Modern Colorful Smiling Avatars (Personas style)...")

    for name, seed, gender in avatars:
        url = f"https://api.dicebear.com/9.x/personas/png?seed={seed}&eyes=open&mouth=smile&size=512"
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

                print(f"[OK] {name} deployed ({gender} - {seed})")

        except Exception as e:
            print(f"[ERROR] Failed to download {name}: {e}")

    # Clean up test temp folder
    test_dir = "preview/personas_test"
    if os.path.exists(test_dir):
        import shutil
        shutil.rmtree(test_dir, ignore_errors=True)

    print("\nAll 10 Modern Colorful Avatars successfully deployed to Centwise!")

if __name__ == "__main__":
    setup_personas_avatars()
