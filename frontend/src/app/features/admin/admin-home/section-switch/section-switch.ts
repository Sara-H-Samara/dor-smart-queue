import { Component, input, model } from '@angular/core';

export interface SectionOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-section-switch',
  templateUrl: './section-switch.html',
  styleUrl: './section-switch.scss',
})
export class SectionSwitch {
  readonly sections = input.required<SectionOption[]>();
  readonly groupName = input.required<string>();
  readonly groupLabel = input.required<string>();
  readonly selectedSection = model.required<string>();
}
