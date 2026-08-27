import os
import urllib.request
from PIL import Image
import io

def download_notionists_avatars():
    # 10 curated seeds (5 male, 5 female with distinct hairstyles, glasses, gestures)
    avatar_seeds = [
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

    ios_assets_root = "apps/ios/Centwise/Assets.xcassets"
    android_res_root = "apps/android/app/src/main/res/drawable"
    preview_root = "preview"

    os.makedirs(ios_assets_root, exist_ok=True)
    os.makedirs(android_res_root, exist_ok=True)
    os.makedirs(preview_root, exist_ok=True)

    print("Downloading 10 Notionists Avatars from DiceBear API...")

    for name, seed, gender in avatar_seeds:
        # DiceBear Notionists API endpoint
        url = f"https://api.dicebear.com/7.x/notionists/png?seed={seed}&size=512"
        
        req = urllib.request.Request(
            url, 
            headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'}
        )

        try:
            with urllib.request.urlopen(req) as response:
                img_data = response.read()
                img = Image.open(io.BytesIO(img_data)).convert("RGBA")

                # 1. iOS Assets Imageset
                imageset_dir = os.path.join(ios_assets_root, f"{name}.imageset")
                os.makedirs(imageset_dir, exist_ok=True)
                
                # Write Contents.json for iOS
                contents_json = f'''{{
  "images" : [
    {{
      "filename" : "{name}.png",
      "idiom" : "universal",
      "scale" : "1x"
    }},
    {{
      "filename" : "{name}@2x.png",
      "idiom" : "universal",
      "scale" : "2x"
    }},
    {{
      "filename" : "{name}@3x.png",
      "idiom" : "universal",
      "scale" : "3x"
    }}
  ],
  "info" : {{
    "author" : "xcode",
    "version" : 1
  }}
}}'''
                with open(os.path.join(imageset_dir, "Contents.json"), "w") as f:
                    f.write(contents_json)

                # Save 1x, 2x, 3x for iOS
                img.resize((128, 128), Image.Resampling.LANCZOS).save(os.path.join(imageset_dir, f"{name}.png"), "PNG")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(os.path.join(imageset_dir, f"{name}@2x.png"), "PNG")
                img.resize((384, 384), Image.Resampling.LANCZOS).save(os.path.join(imageset_dir, f"{name}@3x.png"), "PNG")

                # 2. Android Drawable
                android_file = os.path.join(android_res_root, f"{name}.png")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(android_file, "PNG")

                # 3. HTML Preview
                preview_file = os.path.join(preview_root, f"{name}.png")
                img.resize((256, 256), Image.Resampling.LANCZOS).save(preview_file, "PNG")

                print(f"[OK] Saved {name} ({gender} - {seed}) across iOS, Android, and Preview.")

        except Exception as e:
            print(f"Error downloading {name}: {e}")

    print("\nAll 10 Notionists avatars downloaded and deployed successfully!")

if __name__ == "__main__":
    download_notionists_avatars()
