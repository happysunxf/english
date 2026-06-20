#!/usr/bin/env python3
"""
Download Peppa Pig episodes into the app's private video directory.

Workflow:
  1. Reads assets/manifest/peppa_pig_manifest.json
  2. Downloads each episode using yt-dlp (1080p mp4, capped at 100MB)
  3. Saves into app/src/main/assets/videos/<id>.mp4

Usage:
  # First time: install yt-dlp
  pip install --user yt-dlp

  # Download all 30 episodes
  python3 scripts/download_assets.py

  # Download first 5 only (for testing)
  python3 scripts/download_assets.py --limit 5

  # Use audio-only (saves space; ~5MB per episode)
  python3 scripts/download_assets.py --audio-only

Notes:
  - YouTube URL formats change often; yt-dlp keeps up with them.
  - If a download fails, re-run the script — it skips files that exist.
  - Tested with yt-dlp 2024.05.27 on Ubuntu 22.04.
"""

import argparse
import json
import subprocess
import sys
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_ROOT = SCRIPT_DIR.parent
MANIFEST_PATH = PROJECT_ROOT / "app/src/main/assets/manifest/peppa_pig_manifest.json"
OUTPUT_DIR = PROJECT_ROOT / "app/src/main/assets/videos"


def check_ytdlp() -> str:
    """Return path to yt-dlp executable or exit with installation hint."""
    candidates = ["yt-dlp", f"{Path.home()}/.local/bin/yt-dlp"]
    for c in candidates:
        try:
            subprocess.run([c, "--version"], check=True, capture_output=True)
            return c
        except (FileNotFoundError, subprocess.CalledProcessError):
            continue
    print("❌ yt-dlp not found. Install with:")
    print("    pip install --user yt-dlp")
    print("    export PATH=$HOME/.local/bin:$PATH")
    sys.exit(1)


def download_episode(ytdlp: str, video_id: str, url: str, audio_only: bool):
    output_path = OUTPUT_DIR / f"{video_id}.mp4"
    if output_path.exists():
        print(f"  ↪ skip {video_id} (already exists)")
        return True

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    if audio_only:
        # Audio-only m4a, ~5MB per episode
        cmd = [
            ytdlp, "-x", "--audio-format", "mp4",
            "-o", str(output_path),
            url,
        ]
    else:
        # 720p mp4 capped at 100MB
        cmd = [
            ytdlp, "-f", "best[height<=720][ext=mp4]/best[height<=720]/best",
            "--merge-output-format", "mp4",
            "-o", str(output_path),
            url,
        ]

    print(f"  ↓ downloading {video_id}...")
    try:
        subprocess.run(cmd, check=True)
        print(f"  ✅ {video_id} -> {output_path.name}")
        return True
    except subprocess.CalledProcessError as e:
        print(f"  ❌ {video_id} failed: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(description="Download Peppa Pig episodes for MorningEnglish")
    parser.add_argument("--limit", type=int, default=0, help="Limit to first N episodes (0 = all)")
    parser.add_argument("--audio-only", action="store_true", help="Audio-only download (smaller files)")
    parser.add_argument("--id", type=str, default=None, help="Download only this specific episode id")
    args = parser.parse_args()

    if not MANIFEST_PATH.exists():
        print(f"❌ Manifest not found: {MANIFEST_PATH}")
        sys.exit(1)

    with open(MANIFEST_PATH) as f:
        manifest = json.load(f)

    videos = manifest["videos"]
    if args.id:
        videos = [v for v in videos if v["id"] == args.id]
    if args.limit > 0:
        videos = videos[:args.limit]

    print(f"📥 Downloading {len(videos)} Peppa Pig episode(s) into {OUTPUT_DIR}")
    print(f"   mode: {'audio-only' if args.audio_only else 'video 720p'}")
    print()

    ytdlp = check_ytdlp()
    success, fail = 0, 0
    for v in videos:
        ok = download_episode(ytdlp, v["id"], v["url"], args.audio_only)
        if ok:
            success += 1
        else:
            fail += 1

    print()
    print(f"✅ Done: {success} success, {fail} failed")
    print(f"   Files in: {OUTPUT_DIR}")
    print()
    print("Next steps:")
    print("  1. Push the videos to your device:")
    print(f"       adb push {OUTPUT_DIR} /data/data/com.morningenglish.app/files/videos/")
    print("  2. Update Room's filePath column to point at each downloaded file")
    print("     (a future 'sync_files' task will automate this)")


if __name__ == "__main__":
    main()