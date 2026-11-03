#!/usr/bin/env python3
import base64
import os
import subprocess
import sys

TARGET_SHA1 = "B4:8C:A4:55:F4:4B:94:D2:D8:00:62:A7:4E:50:9C:FA:BB:D3:1F:18"
KEYSTORE_PATH = "debug.keystore"

B64_KEYSTORE = """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"""

def restore():
    print("Unconditionally writing canonical debug.keystore...")
    raw = base64.b64decode(B64_KEYSTORE.strip())
    with open(KEYSTORE_PATH, "wb") as f:
        f.write(raw)
    
    file_size = os.path.getsize(KEYSTORE_PATH)
    print(f"Wrote {file_size} bytes to {KEYSTORE_PATH}.")
    if file_size != 2650:
        print(f"ERROR: Keystore size is {file_size}, expected 2650!")
        sys.exit(1)

    # Verify with keytool
    proc = subprocess.run(
        ["keytool", "-list", "-v", "-keystore", KEYSTORE_PATH, "-storepass", "android", "-alias", "androiddebugkey"],
        capture_output=True,
        text=True
    )
    if proc.returncode != 0:
        print("ERROR: keytool failed with code", proc.returncode)
        print(proc.stderr)
        sys.exit(proc.returncode)

    found_sha1 = None
    for line in proc.stdout.splitlines():
        if "SHA1:" in line:
            found_sha1 = line.split("SHA1:")[1].strip()
            print(f"Verified Keystore SHA1: {found_sha1}")
            break

    if found_sha1 != TARGET_SHA1:
        print(f"ERROR: Keystore SHA1 {found_sha1} does not match official {TARGET_SHA1}")
        sys.exit(1)

    print("Keystore integrity and fingerprint verified successfully!")

if __name__ == "__main__":
    restore()
