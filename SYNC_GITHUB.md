# Procedimiento para sincronizar con GitHub desde el ZIP descargado

Sigue estos pasos en tu computadora (Windows) después de descargar el archivo `.zip` desde Google AI Studio para subir los cambios y disparar **GitHub Actions** (`Build & Release Android APK`).

---

## 1. Preparar tu repositorio local (solo la primera vez)

Si aún no tienes el repositorio clonado en tu PC:

```powershell
git clone https://github.com/TU_USUARIO/TU_REPOSITORIO.git
cd TU_REPOSITORIO
git checkout main
```

*(Si tu rama principal se llama `master`, reemplaza `main` por `master` en los comandos.)*

---

## 2. Copiar los archivos del ZIP al repositorio local

1. Descomprime el `.zip` descargado de Google AI Studio en una carpeta temporal.
2. Abre la carpeta de tu repositorio clonado (`TU_REPOSITORIO`).
   - **Importante:** Asegúrate de **NO borrar ni sobrescribir** la carpeta oculta `.git`.
3. Copia todo el contenido de la carpeta descomprimida (incluyendo `.github`, `app`, `gradle`, `.gitignore`, etc.) dentro de `TU_REPOSITORIO` y acepta reemplazar los archivos existentes.
   - *Nota:* Si trabajas en varias iteraciones, conviene limpiar primero los archivos viejos dentro de `TU_REPOSITORIO` (dejando intacta únicamente la carpeta `.git`) antes de pegar los del nuevo ZIP, para que también se reflejen archivos eliminados o renombrados.

---

## 3. Hacer commit y subir a GitHub

Abre PowerShell o Terminal dentro de la carpeta `TU_REPOSITORIO` y ejecuta:

```bash
git add -A
git commit -m "Actualizacion desde AI Studio"
git push origin main
```

---

## 4. Verificar que se dispare GitHub Actions

- El workflow `.github/workflows/android_release.yml` se ejecuta automáticamente al hacer `push` a **`main`** o **`master`** (o con etiquetas `v*`).
- Puedes ver el progreso en tu repositorio de GitHub en la pestaña **Actions**.
- Al finalizar, el APK compilado queda disponible en los **Artifacts** de la ejecución y en la sección **Releases**.

### Notas útiles
- **Permiso `workflow`:** Si cambiaste algún archivo dentro de `.github/workflows/` y haces `push` usando un Personal Access Token (PAT), asegúrate de que el token tenga los permisos `repo` y `workflow` (o usa **GitHub Desktop** / `gh auth login`).
- **Firebase (`google-services.json`):** El workflow utiliza `app/google-services.json` si está incluido o el secreto `GOOGLE_SERVICES_JSON` configurado en **Settings → Secrets and variables → Actions** en GitHub.
