# 🔐 GitHub Actions — Required Secrets for Release Build

To build a signed release AAB, add these 4 secrets to your GitHub repository:

**Go to:** GitHub Repo → Settings → Secrets and variables → Actions → New repository secret

---

## Step 1 — Generate your keystore (run once, locally)

```bash
cd Asd-app
bash scripts/generate-keystore.sh
```

Follow the prompts. It will create `app/keystore/release.keystore` and print the exact values you need below.

---

## Step 2 — Add these 4 GitHub Secrets

| Secret Name        | Value                                                                 |
|--------------------|-----------------------------------------------------------------------|
| `KEYSTORE_BASE64`  | Output of: `base64 -w 0 app/keystore/release.keystore`              |
| `KEYSTORE_PASSWORD`| The keystore password you set during generation                       |
| `KEY_ALIAS`        | The key alias you chose (e.g. `pictoplay`)                           |
| `KEY_PASSWORD`     | The key password you set during generation                            |

---

## Step 3 — Trigger a release build

Push a version tag to trigger the signed AAB build:

```bash
git tag v1.0.0
git push origin v1.0.0
```

This triggers `.github/workflows/release-aab.yml`, which will:
1. Decode the keystore from the secret
2. Build a signed `app-release.aab`
3. Upload it as a workflow artifact

Download the `.aab` from GitHub Actions artifacts and upload it to the **Play Console**.

---

## ⚠️ Important Notes

- **Back up your keystore file securely** (cloud storage, password manager). If lost, you cannot publish updates.
- The `app/keystore/` folder is in `.gitignore` — the keystore file will never be committed.
- Each Play Store update must increment `versionCode` in `app/build.gradle`.
