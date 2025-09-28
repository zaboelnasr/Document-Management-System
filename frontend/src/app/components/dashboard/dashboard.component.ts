import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { Document, DocumentService } from '../../services/document.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [FormsModule, CommonModule, HttpClientModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  documents: Document[] = [];
  newDocument: Document = { fileName: '', summary: '' };

  constructor(private docService: DocumentService) {}

  ngOnInit(): void {
    this.docService.getDocuments().subscribe(data => {
      this.documents = data;
    });
  }

  addDocument(): void {
    this.docService.addDocument(this.newDocument).subscribe(saved => {
      this.documents.push(saved);
      this.newDocument = { fileName: '', summary: '' };
    });
  }
}
