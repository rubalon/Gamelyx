// src/app/shared/services/game-api.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// 🎮 Interfaces para tipado fuerte
export interface GameSearchItem {
  rawgId: number;
  slug: string;
  name: string;
  coverImage: string;
  description: string;
  rating: number;
  released: string;
  platforms: string[];
  genres: string[];
}

export interface GameSearchResponse {
  games: GameSearchItem[];
  currentPage: number;
  totalPages: number;
  totalResults: number;
  hasMore: boolean;
  searchQuery: string;
}

export interface GameDetails {
  rawgId: number;
  slug: string;
  name: string;
  description: string;
  descriptionRaw: string;
  backgroundImage: string;
  coverImage: string;
  screenshots: string[];
  rating: number;
  communityRating: number;
  totalCommunityReviews: number;
  released: string;
  website: string;
  metacriticScore: number;
  averagePlaytime: number;
  platforms: string[];
  genres: string[];
  developers: string[];
  publishers: string[];
  tags: string[];
  myStatus: GameUserStatus | null;
  recentReviews: GameReview[];
  lastUpdated: string;
}

export interface GameUserStatus {
  status: 'WISHLIST' | 'PLAYING' | 'COMPLETED' | 'ARCHIVED';
  rating: number;
  reviewText: string;
  reviewUpdatedAt: string;
}

export interface GameReview {
  username: string;
  rating: number;
  reviewText: string;
  status: string;
  reviewCreatedAt: string;
}

export interface MyReviewsResponse {
  reviews: MyGameReview[];
  currentPage: number | null;
  totalPages: number | null;
  totalReviews: number | null;
  hasMore: boolean | null;
}

export interface MyGameReview {
  gameRawgId: number;
  gameSlug: string;
  gameName: string;
  coverImage: string;
  rating: number;
  reviewText: string;
  status: string;
  reviewCreatedAt: string;
  reviewUpdatedAt: string;
}

export interface UpdateReviewRequest {
  status?: 'WISHLIST' | 'PLAYING' | 'COMPLETED' | 'ARCHIVED';
  rating?: number;
  reviewText?: string;
}

export interface UpdateReviewResponse {
  status: string;
  rating: number;
  reviewText: string;
  updatedAt: string;
  communityRating: number;
  totalReviews: number;
}

@Injectable({
  providedIn: 'root'
})
export class GameApiService {
  private http = inject(HttpClient);
  
  // URL base del backend desde environment centralizado
  private readonly API_URL = `${environment.apiUrl}/games`;

  /**
   * 🔍 Buscar juegos - Para el home search box
   * GET /api/games/search?q=minecraft&page=1&size=20
   */
  searchGames(query: string, page: number = 1, size: number = 20): Observable<GameSearchResponse> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<GameSearchResponse>(`${this.API_URL}/search`, { params });
  }

  /**
   * 🎮 Obtener detalles completos del juego
   * GET /api/games/game/{identifier}
   * Acepta rawgId o slug, preferimos slug
   */
  getGameDetails(identifier: string | number): Observable<GameDetails> {
    return this.http.get<GameDetails>(`${this.API_URL}/game/${identifier}`);
  }

  /**
   * 💭 Actualizar mi review/estado del juego
   * PUT /api/games/game/{identifier}/my-review
   * Requiere autenticación
   */
  updateMyReview(identifier: string | number, review: UpdateReviewRequest): Observable<UpdateReviewResponse> {
    return this.http.put<UpdateReviewResponse>(
      `${this.API_URL}/game/${identifier}/my-review`, 
      review
    );
  }

  /**
   * 📝 Obtener mis reviews
   * GET /api/games/my-reviews
   * Para home: sin paginación (limit=3)
   * Para página completa: con paginación
   */
  getMyReviews(params?: { 
    limit?: number; 
    page?: number; 
    size?: number; 
  }): Observable<MyReviewsResponse> {
    let httpParams = new HttpParams();
    
    if (params?.limit) {
      httpParams = httpParams.set('limit', params.limit.toString());
    }
    if (params?.page !== undefined) {
      httpParams = httpParams.set('page', params.page.toString());
    }
    if (params?.size) {
      httpParams = httpParams.set('size', params.size.toString());
    }

    return this.http.get<MyReviewsResponse>(`${this.API_URL}/my-reviews`, { params: httpParams });
  }

  /**
   * 🏥 Health check del servicio
   * GET /api/games/health
   */
  healthCheck(): Observable<any> {
    return this.http.get(`${this.API_URL}/health`);
  }
}