import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface DocumentDto {
  id: number;
  fileName: string;
  summary: string;
  content?: any;
  createdAt?: string;
  updatedAt?: string;
  ocrStatus?: 'PENDING' | 'COMPLETED' | 'FAILED';
  reviewStatus?: 'OPEN' | 'IN_REVIEW' | 'REVIEWED';
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface CreateDocumentRequest {
  fileName: string;
  summary: string;
}

export interface UpdateDocumentRequest {
  fileName: string;
  summary: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {
  private readonly apiUrl = '/api/documents';

  constructor(private http: HttpClient) {}

  // Get all documents (paged)
  getDocuments(status?: string) {
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<Page<DocumentDto>>(this.apiUrl, { params }).pipe(
      map((p) => p.content ?? [])
    );
  }

  getDocument(id: number): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`${this.apiUrl}/${id}`);
  }

  addDocument(doc: CreateDocumentRequest): Observable<DocumentDto> {
    return this.http.post<DocumentDto>(this.apiUrl, doc);
  }

  uploadDocument(formData: FormData): Observable<DocumentDto> {
    return this.http.post<DocumentDto>(`${this.apiUrl}/upload`, formData);
  }

  updateDocument(id: number, doc: UpdateDocumentRequest): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${this.apiUrl}/${id}`, doc);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  searchDocuments(term: string): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>(
      `${this.apiUrl}/search?term=${encodeURIComponent(term)}`
    );
  }

  updateReviewStatus(id: number, status: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/review-status`, { status });
  }
}
