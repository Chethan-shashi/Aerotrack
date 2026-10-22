import { Component, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  credentials = { username: '', password: '' };

  loading = signal(false);
  errorMessage = signal('');
  showPassword = signal(false);

  constructor(private http: HttpClient, private router: Router) {}

  login() {
    if (!this.credentials.username || !this.credentials.password) {
      this.errorMessage.set('Please enter your username and password.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.http.post<any>('/api/auth/login', this.credentials).subscribe({
      next: (res) => {
        localStorage.setItem('token', res.token);
        this.loading.set(false);
        this.router.navigate(['/map']);
      },
      error: (err) => {
        console.error(`Login failed (Status: ${err.status}):`, err.error);
        this.loading.set(false);
        if (err.status === 401) {
          this.errorMessage.set(err.error?.error || 'Invalid username or password.');
        } else if (err.status === 0 || err.status === 504) {
          this.errorMessage.set('Cannot reach the server. Is the backend running on port 8080?');
        } else {
          this.errorMessage.set(err.error?.error || err.error?.message || `Unexpected error (${err.status}).`);
        }
      }
    });
  }
}
