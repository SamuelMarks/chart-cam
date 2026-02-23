# Deployment & Site Generation Guide

ChartCam includes an automated site generation script that builds the Web (Wasm) App, API documentation (Dokka), and generates static HTML versions of our Markdown documentation.

This fully static website can be deployed to **GitHub Pages**, **AWS S3**, **Vercel**, or **Netlify** with zero backend configuration.

## Requirements

Ensure you have the following installed on your local machine or CI environment:
- **Java 17+** (to run `./gradlew`)
- **Python 3+** (used to render Markdown to HTML)
- **Bash** environment (Linux, macOS, or WSL for Windows)

## Generating the Site Locally

A helper script has been created to perform all required builds and package them cleanly into the `site_dist/` folder.

1. Open your terminal at the root of the project.
2. Run the site build script:
   ```bash
   ./scripts/build_site.sh
   ```

### What does the script do?

1. **Wasm Web App**: Compiles the Compose Multiplatform UI into WebAssembly using `./gradlew :chartCam:wasmJsBrowserDistribution`. The artifacts are copied to `site_dist/wasmJs/`.
2. **API Documentation**: Runs Dokka (`./gradlew :chartCam:dokkaHtml`) to generate the official KDoc reference for the codebase. The artifacts are copied to `site_dist/docs/`.
3. **HTML Theming**: Uses the `site/style.css` and `site/index.html` structure you created to form the baseline CSS theme.
4. **Markdown Rendering**: Iterates through `.md` files in the root folder (e.g., `README.md`, `USAGE.md`) and the `docs/` folder, converting them into HTML using Python's `markdown` module. The artifacts are placed in `site_dist/guides/`.

### Testing Locally

You can preview the generated site exactly as it will appear in production by running a simple HTTP server:

```bash
cd site_dist
python3 -m http.server
```

Open your browser to `http://localhost:8000`. 
*(Note: Wasm applications typically require proper HTTP hosting rather than `file:///` protocols to run successfully due to browser security restrictions).*

## Continuous Deployment (CI/CD)

To automatically deploy the site whenever you push to `master`, you can add a new job to your `.github/workflows/deploy.yml` file, or create a new `.github/workflows/pages.yml`.

### Example GitHub Pages Workflow

If using GitHub pages, ensure that you have configured your repository to point to GitHub actions for pages.

```yaml
name: Deploy Site to GitHub Pages

on:
  push:
    branches: ["master"]

permissions:
  contents: read
  pages: write
  id-token: write

jobs:
  build_site:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Setup Gradle Cache
        uses: gradle/actions/setup-gradle@v3

      - name: Generate Site
        run: |
          chmod +x scripts/build_site.sh
          ./scripts/build_site.sh
          
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: './site_dist'

  deploy_site:
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    runs-on: ubuntu-latest
    needs: build_site
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

## Structure of the Generated Output

After running the script, `site_dist/` will look like this:

```
site_dist/
├── index.html           # The main entrypoint mapping all assets
├── style.css            # Global unified CSS theme
├── wasmJs/              # Compose Multiplatform compiled Wasm assets
│   ├── index.html       # Web App wrapper
│   ├── chartCam.js      # JS bootstrap
│   └── *.wasm           # WebAssembly payloads
├── docs/                # Dokka API Reference
│   └── ...
└── guides/              # Rendered Markdown HTML files
    ├── README.html
    ├── USAGE.html
    ├── SECURITY.html
    ├── HOW_TO_RELEASE.html
    └── NAVIGATION.html
```