import { Component, OnInit, inject, signal } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { RouterOutlet } from "@angular/router";

@Component({
  selector: "app-root",
  imports: [RouterOutlet],
  templateUrl: "./app.html",
  styleUrl: "./app.scss",
})
export class App implements OnInit {
  private http = inject(HttpClient);
  protected readonly backendStatus = signal("Checking...");

  ngOnInit() {
    this.http.get("/api/health", { responseType: "text" }).subscribe({
      next: (result) => this.backendStatus.set(result),
      error: () => this.backendStatus.set("Backend not reachable"),
    });
  }
}
