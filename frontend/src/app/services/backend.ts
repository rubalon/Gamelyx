import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class Backend {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  // Método para obtener el saludo del backend
  getHello(): Observable<string> {
    return this.http.get(`${this.baseUrl}/hello`, { responseType: 'text' });
  }

  // Método para obtener el estado del backend
  getStatus(): Observable<string> {
    return this.http.get(`${this.baseUrl}/status`, { responseType: 'text' });
  }
}