import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-landing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <!-- Navigation -->
    <nav class="nav">
      <div class="nav-inner">
        <a routerLink="/" class="logo">
          <span class="logo-icon">&#9654;</span>
          YTShare
        </a>
        <div class="nav-links">
          <a routerLink="/login" class="nav-link">Sign In</a>
          <a routerLink="/register" class="nav-link nav-link--cta">Get Started</a>
        </div>
      </div>
    </nav>

    <!-- Hero -->
    <section class="hero">
      <div class="container">
        <h1 class="hero-title">Share YouTube videos<br>from phone to PC instantly</h1>
        <p class="hero-subtitle">
          YTShare lets you send YouTube links from your Android device straight to your computer.
          Scan a QR code, tap share, and it opens on your PC — no cables, no hassle.
        </p>
        <div class="hero-actions">
          <a href="#download" class="btn btn-primary btn-lg">Download Now</a>
          <a href="#how-it-works" class="btn btn-outline btn-lg">How It Works</a>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features" id="features">
      <div class="container">
        <h2 class="section-title">Why YTShare?</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 12h-4l-3 9L9 3l-3 9H2"/>
              </svg>
            </div>
            <h3>Instant Sharing</h3>
            <p>Share a YouTube link from your phone and it opens on your PC within seconds.</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="2" y="2" width="20" height="20" rx="5" ry="5"/>
                <path d="M16 11.37A4 4 0 1112.63 8 4 4 0 0116 11.37z"/>
                <line x1="17.5" y1="6.5" x2="17.51" y2="6.5"/>
              </svg>
            </div>
            <h3>QR Code Pairing</h3>
            <p>Connect your phone to your PC by scanning a QR code — setup takes seconds.</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4-4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M23 21v-2a4 4 0 00-3-3.87"/>
                <path d="M16 3.13a4 4 0 010 7.75"/>
              </svg>
            </div>
            <h3>Bookmarks</h3>
            <p>Save your favorite shared videos and access them anytime from your bookmarks.</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                <line x1="12" y1="18" x2="12.01" y2="18"/>
              </svg>
            </div>
            <h3>Multiple Devices</h3>
            <p>Manage connections to multiple PCs and choose where to send your videos.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- How it works -->
    <section class="how-it-works" id="how-it-works">
      <div class="container">
        <h2 class="section-title">How It Works</h2>
        <div class="steps">
          <div class="step">
            <div class="step-number">1</div>
            <h3>Install Both Apps</h3>
            <p>Download the Android app on your phone and the Host app on your PC.</p>
          </div>
          <div class="step">
            <div class="step-number">2</div>
            <h3>Pair via QR Code</h3>
            <p>Open the Host on your PC, scan the QR code with the Android app to connect.</p>
          </div>
          <div class="step">
            <div class="step-number">3</div>
            <h3>Share & Watch</h3>
            <p>Share any YouTube video from your phone — it opens instantly on your PC browser.</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Download -->
    <section class="download" id="download">
      <div class="container">
        <h2 class="section-title section-title--light">Download YTShare</h2>
        <p class="download-subtitle">Get both apps to start sharing videos between your devices.</p>
        <div class="download-cards">
          <div class="download-card">
            <div class="download-card-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M17.523 2.047l-5.477 9.953 5.477 9.953c.15-.1.295-.206.433-.32a11.96 11.96 0 000-19.266 7.5 7.5 0 00-.433-.32zM1.99 6.39a12.03 12.03 0 000 11.22L7.467 12 1.99 6.39zM16.2 1.14A11.93 11.93 0 0012 .05c-1.8 0-3.5.4-5.03 1.1L12.046 12l4.153-10.86zM6.97 13.1L2.1 17.86A11.93 11.93 0 0012 23.95c1.45 0 2.85-.26 4.14-.73L6.97 13.1z"/>
              </svg>
            </div>
            <h3>Android App</h3>
            <p>Share YouTube links from your phone to any connected PC.</p>
            <a href="/downloads/YTShare.apk" class="btn btn-primary btn-download">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              Download APK
            </a>
          </div>
          <div class="download-card">
            <div class="download-card-icon">
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M0 3.449L9.75 2.1v9.451H0m10.949-9.602L24 0v11.4H10.949M0 12.6h9.75v9.451L0 20.699M10.949 12.6H24V24l-12.9-1.801"/>
              </svg>
            </div>
            <h3>Windows Host</h3>
            <p>Receive shared videos and open them directly in your browser.</p>
            <a href="/downloads/YTShare.Host.zip" class="btn btn-primary btn-download">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="btn-icon">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              Download for Windows
            </a>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="container footer-inner">
        <div class="footer-brand">
          <span class="logo-icon">&#9654;</span>
          <span>YTShare</span>
        </div>
        <div class="footer-links">
          <a routerLink="/login">Sign In</a>
          <a routerLink="/register">Register</a>
          <a href="#features">Features</a>
          <a href="#download">Download</a>
        </div>
        <p class="footer-copy">&copy; 2026 YTShare. All rights reserved.</p>
      </div>
    </footer>
  `,
  styles: `
    :host {
      display: block;
    }

    .container {
      max-width: 1120px;
      margin: 0 auto;
      padding: 0 1.5rem;
    }

    /* Nav */
    .nav {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      z-index: 100;
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(8px);
      border-bottom: 1px solid var(--color-border);
    }

    .nav-inner {
      max-width: 1120px;
      margin: 0 auto;
      padding: 0 1.5rem;
      height: 64px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--color-text);
      text-decoration: none;
    }

    .logo-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: var(--color-primary);
      color: white;
      border-radius: 8px;
      font-size: 0.875rem;
    }

    .nav-links {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .nav-link {
      padding: 0.5rem 1rem;
      font-size: 0.9rem;
      font-weight: 500;
      color: var(--color-text-muted);
      text-decoration: none;
      border-radius: var(--border-radius);
      transition: color 0.15s, background 0.15s;

      &:hover {
        color: var(--color-text);
        text-decoration: none;
      }
    }

    .nav-link--cta {
      background: var(--color-primary);
      color: white;

      &:hover {
        background: var(--color-primary-hover);
        color: white;
      }
    }

    /* Hero */
    .hero {
      padding: 10rem 0 6rem;
      text-align: center;
      background: linear-gradient(180deg, #fff 0%, #fef2f2 100%);
    }

    .hero-title {
      margin: 0 0 1.5rem;
      font-size: clamp(2rem, 5vw, 3.25rem);
      font-weight: 800;
      line-height: 1.15;
      color: var(--color-text);
    }

    .hero-subtitle {
      margin: 0 auto 2.5rem;
      max-width: 600px;
      font-size: 1.125rem;
      line-height: 1.7;
      color: var(--color-text-muted);
    }

    .hero-actions {
      display: flex;
      gap: 1rem;
      justify-content: center;
      flex-wrap: wrap;
    }

    /* Buttons */
    .btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: var(--border-radius);
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      text-decoration: none;
      transition: background 0.15s, transform 0.1s;

      &:hover {
        text-decoration: none;
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }
    }

    .btn-primary {
      background: var(--color-primary);
      color: white;

      &:hover {
        background: var(--color-primary-hover);
        color: white;
      }
    }

    .btn-outline {
      background: transparent;
      color: var(--color-primary);
      border: 2px solid var(--color-primary);

      &:hover {
        background: var(--color-primary);
        color: white;
      }
    }

    .btn-lg {
      padding: 0.875rem 2rem;
      font-size: 1.075rem;
    }

    .btn-download {
      width: 100%;
      padding: 0.875rem 1.5rem;
    }

    .btn-icon {
      width: 20px;
      height: 20px;
    }

    /* Features */
    .features {
      padding: 5rem 0;
      background: white;
    }

    .section-title {
      text-align: center;
      margin: 0 0 3rem;
      font-size: 2rem;
      font-weight: 700;
      color: var(--color-text);
    }

    .section-title--light {
      color: white;
    }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 2rem;
    }

    .feature-card {
      padding: 2rem;
      border-radius: 12px;
      border: 1px solid var(--color-border);
      transition: box-shadow 0.2s, transform 0.2s;

      &:hover {
        box-shadow: var(--shadow-lg);
        transform: translateY(-4px);
      }

      h3 {
        margin: 1rem 0 0.5rem;
        font-size: 1.125rem;
        font-weight: 600;
      }

      p {
        margin: 0;
        color: var(--color-text-muted);
        font-size: 0.95rem;
        line-height: 1.6;
      }
    }

    .feature-icon {
      width: 48px;
      height: 48px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-primary-light);
      color: var(--color-primary);
      border-radius: 12px;

      svg {
        width: 24px;
        height: 24px;
      }
    }

    /* How it works */
    .how-it-works {
      padding: 5rem 0;
      background: var(--color-background);
    }

    .steps {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 2rem;
      text-align: center;
    }

    .step {
      padding: 2rem;

      h3 {
        margin: 1rem 0 0.5rem;
        font-size: 1.125rem;
        font-weight: 600;
      }

      p {
        margin: 0;
        color: var(--color-text-muted);
        line-height: 1.6;
      }
    }

    .step-number {
      width: 56px;
      height: 56px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--color-primary);
      color: white;
      border-radius: 50%;
      font-size: 1.5rem;
      font-weight: 700;
    }

    /* Download */
    .download {
      padding: 5rem 0;
      background: linear-gradient(135deg, #FF0000 0%, #A80202 100%);
    }

    .download-subtitle {
      text-align: center;
      margin: -2rem 0 3rem;
      font-size: 1.1rem;
      color: rgba(255, 255, 255, 0.85);
    }

    .download-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 2rem;
      max-width: 700px;
      margin: 0 auto;
    }

    .download-card {
      background: rgba(255, 255, 255, 0.12);
      backdrop-filter: blur(8px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 16px;
      padding: 2rem;
      text-align: center;
      color: white;

      h3 {
        margin: 1rem 0 0.5rem;
        font-size: 1.25rem;
        font-weight: 600;
      }

      p {
        margin: 0 0 1.5rem;
        color: rgba(255, 255, 255, 0.8);
        font-size: 0.95rem;
        line-height: 1.6;
      }

      .btn-primary {
        background: white;
        color: var(--color-primary);

        &:hover {
          background: #f0f0f0;
          color: var(--color-primary-hover);
        }
      }
    }

    .download-card-icon {
      width: 56px;
      height: 56px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 14px;
      color: white;

      svg {
        width: 28px;
        height: 28px;
      }
    }

    /* Footer */
    .footer {
      padding: 3rem 0;
      background: #111;
      color: #aaa;
    }

    .footer-inner {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1.25rem;
    }

    .footer-brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.125rem;
      font-weight: 700;
      color: white;
    }

    .footer-links {
      display: flex;
      gap: 1.5rem;
      flex-wrap: wrap;
      justify-content: center;

      a {
        color: #aaa;
        text-decoration: none;
        font-size: 0.9rem;

        &:hover {
          color: white;
          text-decoration: none;
        }
      }
    }

    .footer-copy {
      margin: 0;
      font-size: 0.8rem;
      color: #666;
    }

    /* Responsive */
    @media (max-width: 640px) {
      .hero {
        padding: 7rem 0 4rem;
      }

      .features,
      .how-it-works,
      .download {
        padding: 3.5rem 0;
      }

      .download-subtitle {
        margin-bottom: 2rem;
      }
    }
  `
})
export class LandingComponent {}
