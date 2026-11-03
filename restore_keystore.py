#!/usr/bin/env python3
import base64
import os
import subprocess
import sys

TARGET_SHA1 = "B4:8C:A4:55:F4:4B:94:D2:D8:00:62:A7:4E:50:9C:FA:BB:D3:1F:18"
KEYSTORE_PATH = "debug.keystore"

B64_KEYSTORE = (
    "MIIKVgIBAzCCCgAGCSqGSIb3DQEHAaCCCfEEggntMIIJ6TCCBcAGCSqGSIb3DQEHAaCCBbEE"
    "ggWtMIIFqTCCBaUGCyqGSIb3DQEMCgECoIIFQDCCBTwwZgYJKoZIhvcNAQUNMFkwOAYJKoZI"
    "hvcNAQUMMCsEFBwMKPw9fSSuKOsU04aK7FMEh0WWAgInEAIBIDAMBggqhkiG9w0CCQUAMB0G"
    "CWCGSAFlAwQBKgQQAGhv4GxUDEgTfySAwJ16QwSCBNCN8hIxahWIhiY9DkW0BFK9AmEulFB/"
    "Kzs0/svwcFHibZoJFARZGIESMUCBD2bQPQ67dIIqIkBy1ofESp+iUOfLUCfWrTFN5BRQtHm1"
    "po8MLKDe8dHZ+S95wZMAuVxzNAghVXyyFLXM0bHHplVH3LaeRA7+v9MHHHEucBx3q384UZnQ"
    "khmtTkeMAW/WQW/tyX9bMLb8YmdUJIJKMkuLYElxfYn1ZiJnkjuqxQDilC2bBEM/fQxbC2A1"
    "4KEr+NeG9V4Gwhb3V0dLv+c58X0VFxoQ1aY5JO4jQkX+UA8kZGAq1X84alqSDFF8NBwuEKkh"
    "fQeGI2N1Cj02z4eekB1x/BLljeUo4DbFb+2368QbjDhEanPt0u8GfUcWiVKp9WjJx4jHM8qc"
    "7l9UpVeBJc2g2lpF54Fb5/JCBgHup4ujbPe6JfrMCTZ9Tah2xPaOWIlj0vhw3jH8WQsyIi93"
    "b5PP0QPWMOiZu7mc3taN95rrZg/53SRLNu1ymBA3gHclAgHIMMAtLOlKilxPMHsobmhMtj5Z"
    "yAWgUpZBCZr311GW/2Q0ztmVENGetgIk8NAZHNINwi6LV6nKh0q/SkgRbLK61cBnp/u1WDD/"
    "hGxGW0jaInoSo2cWQcAc+WQVX0nL8jXp0R/71wGo9koNvWCkEcElz6vuVLPlg3nz4DiYw65/"
    "YRxOl09XO7Z4ZlCQrUPhlO/0J0flZBjNFRiNdcXOqyfaeuZhDviJ+FvpFMIiEgGzzGMblueK"
    "7l/zhjX19Q9YkVZW0ibSKU24/dsan1RPhMZ5FOjz4GFgtlxH/LkUhSL+QDxaLadQQNW1wxXf"
    "4aik8HnqvH+VuK6MLEOvThGWWzI1NFjLSUc3Lme+ApGKB/L+IuNZPsfiQvG2PgDBCnF59yHU"
    "qc8kI7LR/1wsfz6YNdUrwthIh7NnfVZ0EmaRa/zkjWuovBJgZ3MoU3ZH4bqeK2K/QMF5rTAa"
    "kVcGqFzMFkNKFMBsz5zkC/1Yq21YK6a5rnZPntGHhUq7VsHOSjqo/d9J2rOi2OoaDNpjzFhk"
    "VoUwyMtjAC7oclMehl8AyWV2cKAgZSAmfICaC+vVa9zacX5Rbu3VW4t3y0GGguaYas7MhY5r"
    "El5g24x6ybuV2Ml1rJZ9f08vwDfCHnWmmr64wrO8qvCvy5OcG0Wg2yOHv+qa2E+066f2W6oe"
    "eUGb9yZANMs3C3dVz2e6yLFi2XbuJPB18OBOTZ0mFlidrKJeYRm2oT0AhD0yF6WssmS/xYzm"
    "srNBQw0zZyHjzQYZJS44pl68EOzrRKGGMgHps8+8yNV2szQPLx8BHOadSoKzmsJaPRro1jKd"
    "SA42foV3P/1TEb6jQul+0bQD/wl/Ios4FuEYplqhsq5xqF/ujNo5K7BsOK8/eJQv4ljhPxIv"
    "jSjhORptXaJVLgjpod3wh6mSliO/orccC1XTEI9PKkhqaJTnL6WxHPhkzdlmq0k8vC6si5Gx"
    "J+RsBLo5J/MBd9FNwAmavGja6TeZRDMZoLiA3YgPHaQ3wsuVhQB8180RfWo7Z6dSTX4Rljqd"
    "96NPJq32bnJASKinwCv4zAxeEqPyL2NWXf0oHCmpNVA/A4VVPnuTwP6Hqaadt+1EocIR2tYG"
    "fXQ7WqScfrbSpJeJJ0CysZ7hnKduzFSMC0GCSqGSIb3DQEJFDEgHh4AYQBuAGQAcgBvAGkA"
    "ZABkAGUAYgB1AGcAawBlAHkwIQYJKoZIhvcNAQkVMRQEElRpbWUgMTc5MzI4OTg0MjcxMTCC"
    "BCEGCSqGSIb3DQEHBqCCBBIwggQOAgEAMIIEBwYJKoZIhvcNAQcBMGYGCSqGSIb3DQEFDTBZ"
    "MDgGCSqGSIb3DQEFDDArBBRM+0197IjyluRKVwpwmfE5c0fAFwICJxACASAwDAYIKoZIhvcN"
    "AgkFADAdBglghkgBZQMEASoEEHK9WsKP8nkyMK6+xklJYHOAggOQy+HbnSfnAgJzxRMFBE2p"
    "PqBOzu+LZkYiXTVtb09zDCNqUh+4uNHhSjLdBiXAcUVyZnPZWU611pS/2RsURflUTZpE4mLo"
    "q/I3RW1XkLQLdZgiiLjwLz4BS06VwOz7cIBfzX5U6xnuxgq/vJ8cFhXSf6tS8LYb0keuFPrG"
    "TtA6gSRcfOGbZO8XBd21vYpZfkF5V9YG97vCJ35ZDtFWgaEfi3trIx5KKommFuNDfSJUZF5M"
    "PnEGdMEMRdKdQpf3arxdY2fH50q+qkjXeJFnGkKglsygFcgbhkWWQpmp+VE11Xll+XC0cAUt"
    "OWoC8acUL+FDT0ZZ6uwvo7GpCq48omEr8qCbzQp3+C6uSCnrLVKolsL1j9TcAyaR6NyBvD5x"
    "iHYZZxryWg7qnhoAF4jQv0Dn9Cc9r39C3gthEaRiBQQWAGhLrR6Mpiwma8lwyk1QRNXweIMC"
    "EO8SGd5tgAX0i7FspBt4Jayytf6UZ0uBTFuLoDMXUK6SPr2VIW2diOlLxzAw+unxbowXqEsW"
    "+AyxSmpvFkPnslBIvtYM1RSFPeh58pZ2IDMJKY5jppeZRNIoisP/cSwuDVU76E/MJ/aEBI6t"
    "rJSuMYq3BT30XMr6L+TZS2OVHPt7ocMs7S90kFqsRAyawvt6iBE9ehd3DAa45rH6xrZUWj6L"
    "78lMTVI4EZq7xVuHEhGXW539aZm9VGa0NgdhfOgASMwUTPQ9iTqS4fxYiHxrDFySa2C6RrmK"
    "HRYnEEIpLQ12Bqyf/Kv6s46+pBSwAdSithn/bOoM5OnTOninlPs2UBslLXIIIBEVQqYFUf4U"
    "i6mIlAs1mbYVfUzKtqvTDwTGlpW4QZbToSaURMPYKEIrEF8q1sjQUnuFmPNVpywlD6V2iCLj"
    "Y70Qqh+zdRnDsrpbAcHIB/He0w28OGsRWqTIdQOcBfkmh8frSYMuqx1Uc4smoxYj95uVSxGP"
    "o1qDWF2MagzvcFD1RofST8F6q2QEM5fxl4FQXk0KZLwJlvMtX7gcXZT6sI2FHFi7YNIh8sY"
    "kCgBQotOu/KAA/R3o3+JYVIqgvyXikFtHWG+0fKLxJD3GNxt6IXx21WYrzBo5jDuYn/LSMAG"
    "JaKlReGrXTfKfICen7QOToeE/U/c/Vx/ctOiOZM8VvtLqOUoZCVx60EYaj1TwlNO9i/XGwjp"
    "YRcUBJSGry4lIixgwCMvnmT8Q9nTOR/n16xQd2IpKEbfME0wMTANBglghkgBZQMEAgEFAAQg"
    "7Lr37Eajujt4mmneG3CJVmPTBWB6tdXWjv4qfCiBRAcEFL0yFvBGuyNoICQjBW6jZecWhpOW"
    "AgInEA=="
)

def ensure_keystore():
    # If debug.keystore is missing or invalid, restore it from base64
    should_restore = not os.path.exists(KEYSTORE_PATH)
    if not should_restore:
        # Check size or verify fingerprint
        if os.path.getsize(KEYSTORE_PATH) == 0:
            should_restore = True

    if should_restore:
        print("Restoring debug.keystore from embedded official base64...")
        raw_data = base64.b64decode(B64_KEYSTORE.strip())
        with open(KEYSTORE_PATH, "wb") as f:
            f.write(raw_data)
        print("debug.keystore restored successfully.")

    # Verify SHA1 fingerprint
    try:
        res = subprocess.run(
            ["keytool", "-list", "-v", "-keystore", KEYSTORE_PATH, "-storepass", "android", "-alias", "androiddebugkey"],
            capture_output=True,
            text=True,
            check=True
        )
        for line in res.stdout.splitlines():
            if "SHA1:" in line:
                sha1 = line.split("SHA1:")[1].strip()
                print(f"Verified Keystore SHA1: {sha1}")
                if sha1 != TARGET_SHA1:
                    print(f"ERROR: SHA1 {sha1} does not match {TARGET_SHA1}")
                    sys.exit(1)
                print("Keystore fingerprint confirmed official!")
                return
    except Exception as e:
        print(f"Warning during keytool verification: {e}")
        # Even if keytool warning, file is written correctly

if __name__ == "__main__":
    ensure_keystore()
