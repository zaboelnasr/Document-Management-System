import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// 👇 Document interface
export interface Document {
  id?: number;
  fileName: string;
  summary: string;
  content?: any;
  createdAt?: string;
  updatedAt?: string;
  editing?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentService {
  private apiUrl = 'http://localhost:8080/api/documents';  // backend REST API

  constructor(private http: HttpClient) {}

  // GET all documents
  getDocuments(): Observable<Document[]> {
    return this.http.get<Document[]>(this.apiUrl);
  }

  // GET one document by id
  getDocument(id: number): Observable<Document> {
    return this.http.get<Document>(`${this.apiUrl}/${id}`);
  }

  // POST: add a new document
  addDocument(doc: Document): Observable<Document> {
    return this.http.post<Document>(this.apiUrl, doc);
  }

  // PUT: update an existing document
  updateDocument(id: number, doc: Document): Observable<Document> {
    return this.http.put<Document>(`${this.apiUrl}/${id}`, doc);
  }


  // DELETE: remove a document
  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
