import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface DocumentDto {
  id: number;
  fileName: string;
  summary: string;
  content?: any;
  createdAt?: string;
  updatedAt?: string;
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

  getDocuments() {
    return this.http.get<Page<DocumentDto>>(this.apiUrl).pipe(
      map(p => p.content ?? [])
    );
  }

  getDocument(id: number): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`${this.apiUrl}/${id}`);
  }

  addDocument(doc: CreateDocumentRequest): Observable<DocumentDto> {
    return this.http.post<DocumentDto>(this.apiUrl, doc);
  }

  updateDocument(id: number, doc: UpdateDocumentRequest): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${this.apiUrl}/${id}`, doc);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
