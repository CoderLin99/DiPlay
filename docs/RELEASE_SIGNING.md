# Release APK signing

The GitHub Actions workflow can build and sign a release APK when all four repository Actions secrets are configured:

| Secret | Local value file |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `.private/ANDROID_KEYSTORE_BASE64.txt` |
| `ANDROID_KEYSTORE_PASSWORD` | `.private/ANDROID_KEYSTORE_PASSWORD.txt` |
| `ANDROID_KEY_ALIAS` | `.private/ANDROID_KEY_ALIAS.txt` |
| `ANDROID_KEY_PASSWORD` | `.private/ANDROID_KEY_PASSWORD.txt` |

In GitHub, open **Settings → Secrets and variables → Actions**, then add each file's value under its matching secret name. The `.private` folder and keystore are ignored by Git; never commit or publish them.

After the secrets are configured, pushes to `main` and manual workflow runs build `:mobile:assembleRelease` and upload the signed APK as the `diplay-release-apk` artifact. The release signing key must be backed up securely. A different key cannot update an APK signed with this one.
