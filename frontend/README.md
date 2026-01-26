# YTShare Frontend

Angular 20+ frontend for YTShare with Firebase Authentication.

## Prerequisites

- Node.js 20+
- npm 10+

## Setup

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Configure Firebase:**
   
   Update `src/environments/environment.ts` with your Firebase project configuration:
   ```typescript
   firebase: {
     apiKey: 'YOUR_API_KEY',
     authDomain: 'YOUR_PROJECT_ID.firebaseapp.com',
     projectId: 'YOUR_PROJECT_ID',
     storageBucket: 'YOUR_PROJECT_ID.appspot.com',
     messagingSenderId: 'YOUR_SENDER_ID',
     appId: 'YOUR_APP_ID'
   }
   ```

   You can find these values in your Firebase Console → Project Settings → General → Your apps.

3. **Start the development server:**
   ```bash
   npm start
   ```

   Navigate to `http://localhost:4200/`.

## Build

```bash
npm run build
```

Build artifacts are stored in the `dist/` directory.

## Project Structure

```
src/
├── app/
│   ├── core/
│   │   ├── guards/         # Route guards
│   │   ├── interceptors/   # HTTP interceptors
│   │   └── services/       # Core services (AuthService)
│   ├── features/
│   │   ├── auth/           # Login & Register components
│   │   └── home/           # Home/Dashboard component
│   ├── app.component.ts
│   ├── app.config.ts
│   └── app.routes.ts
├── environments/           # Environment configs
└── styles.scss             # Global styles
```

## Features

- **Firebase Authentication** - Email/password login and registration
- **Standalone Components** - No NgModules, Angular 20+ best practices
- **Signals** - Reactive state management with Angular signals
- **Lazy Loading** - Feature routes are lazy loaded
- **Reactive Forms** - Form validation with proper error handling
- **Accessibility** - WCAG AA compliant with proper ARIA attributes
- **OnPush Change Detection** - Optimized rendering performance
