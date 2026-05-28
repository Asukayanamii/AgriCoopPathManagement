# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- `npm run dev` – Start the Vite development server with hot reload
- `npm run build` – Build the project for production
- `npm run preview` – Preview the production build locally

## Architecture Overview

This is a Vue 3 single‑page application for an agricultural drone path‑planning system. It uses Vite as the build tool and Element Plus as the UI component library.

### Build & Development
- **Vite** is configured with a proxy that forwards `/api` requests to `http://localhost:8080` (see `vite.config.js`). The proxy rewrites the path by removing the `/api` prefix.
- The project uses Vue 3 Composition API (`<script setup>` syntax) throughout.
- All Element Plus icons are globally registered in `src/main.js`.

### Routing & Layout
- **Vue Router** is set up with `createWebHistory`. The main layout (`src/views/layout/index.vue`) contains a collapsible sidebar, header, and a main content area where nested routes are rendered.
- The root path (`/`) uses the layout component and redirects to `/index`.
- The login page (`/login`) is a separate route without the layout.
- Additional sidebar menu items correspond to planned sub‑routes (e.g., `/land`, `/algorithm`, `/drone`, etc.) that are not yet implemented.

### State & Authentication
- Login credentials are sent to `/user/login` via `src/api/login.js`.
- On successful login, the returned token is stored in `localStorage` under the key `loginUser`.
- **Axios interceptors** (`src/utils/request.js`) automatically attach the token from `localStorage` to every outgoing request header as `token`.
- Responses are intercepted; a 401 status redirects to the login page and shows an error message.
- The layout component checks login status on mount by calling `/user/loginornot` (see `src/api/userloginornot.js`).

### Project Structure
- `src/views/` – Page‑level components (layout, login, index).
- `src/components/` – Reusable Vue components (currently only `HelloWorld.vue`).
- `src/api/` – API endpoint wrappers that use the configured axios instance.
- `src/utils/` – Shared utilities (currently only the axios request module).
- `src/router/` – Router configuration.
- `src/assets/` – Static images and icons.
- `src/style.css` – Global CSS (if any; currently minimal).
- `public/` – Public assets served at the root.

### Styling
- Component‑scoped styles are written with `<style scoped>` in each `.vue` file.
- Element Plus themes are imported globally from `element-plus/dist/index.css`.
- The layout and login pages include extensive custom CSS to achieve a full‑viewport, scroll‑bar‑free design with gradient backgrounds.

### Important Notes
- The application expects a backend running at `http://localhost:8080` that handles `/user/login` and `/user/loginornot` endpoints.
- Many sidebar menu items link to routes that do not yet have corresponding view components; clicking them will render an empty router‑view.
- The index page (`src/views/index/index.vue`) currently contains only placeholder static HTML.