import { Injectable, signal, computed } from '@angular/core';
import { initializeApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  Auth,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
  User,
  UserCredential,
  updateProfile
} from 'firebase/auth';
import { Observable, from } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuthUser {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
}

export interface RegisterData {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly app: FirebaseApp;
  private readonly auth: Auth;

  private readonly firebaseUser = signal<User | null>(null);

  readonly currentUser = computed<AuthUser | null>(() => {
    const u = this.firebaseUser();
    if (!u) return null;
    return {
      uid: u.uid,
      email: u.email,
      displayName: u.displayName,
      photoURL: u.photoURL
    };
  });

  readonly isAuthenticated = computed(() => this.firebaseUser() !== null);
  readonly isLoading = signal(true);

  constructor() {
    this.app = initializeApp(environment.firebase);
    this.auth = getAuth(this.app);

    onAuthStateChanged(this.auth, (user) => {
      this.firebaseUser.set(user);
      this.isLoading.set(false);
    });
  }

  register(data: RegisterData): Observable<UserCredential> {
    const displayName = `${data.firstName} ${data.lastName}`;
    return from(
      createUserWithEmailAndPassword(this.auth, data.email, data.password).then(async (credential) => {
        if (credential.user) {
          await updateProfile(credential.user, { displayName });
        }
        return credential;
      })
    );
  }

  login(email: string, password: string): Observable<UserCredential> {
    return from(signInWithEmailAndPassword(this.auth, email, password));
  }

  logout(): Observable<void> {
    return from(signOut(this.auth));
  }

  async getIdToken(): Promise<string | null> {
    const currentUser = this.auth.currentUser;
    if (!currentUser) return null;
    return currentUser.getIdToken();
  }

  getFirebaseUid(): string | null {
    return this.auth.currentUser?.uid ?? null;
  }
}
