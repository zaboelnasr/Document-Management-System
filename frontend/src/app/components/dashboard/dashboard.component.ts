import { Component, OnInit } from '@angular/core';
import { Document, DocumentService } from '../../services/document.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, CommonModule],
  styleUrls: ['./dashboard.component.css'],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  documents: Document[] = [];
  newDocument: Document = { fileName: '', summary: '' };


  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.docService.getDocuments().subscribe(data => {
      this.documents = data.map(d => ({ ...d, editing: false })); // add editing flag
    });
  }

  addDocument(): void {
    this.docService.addDocument(this.newDocument).subscribe(saved => {
      this.documents.push({ ...saved, editing: false });
      this.newDocument = { fileName: '', summary: '' };
    });
  }

  editDocument(doc: any): void {
    doc.editing = true;
  }

  saveDocument(doc: Document): void {
    this.docService.updateDocument(doc.id!, doc).subscribe(updated => {
      const index = this.documents.findIndex(d => d.id === updated.id);
      if (index !== -1) {
        this.documents[index] = updated;  // replace instead of push
      }
      doc.editing = false;  // exit edit mode
    });
  }


  cancelEdit(doc: any): void {
    doc.editing = false;
    this.loadDocuments(); // reload to discard changes
  }

  deleteDocument(id?: number): void {
    if (!id) return;
    this.docService.deleteDocument(id).subscribe(() => {
      this.documents = this.documents.filter(d => d.id !== id);
    });
  }
}
