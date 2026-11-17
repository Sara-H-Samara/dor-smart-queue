import { Component, computed, input, output } from '@angular/core';

import { CalledStudent } from '../../../../core/models/staff-desk.model';

@Component({
  selector: 'app-current-student-card',
  templateUrl: './current-student-card.html',
  styleUrl: './current-student-card.scss',
})
export class CurrentStudentCard {
  readonly student = input.required<CalledStudent | null>();
  readonly isSubmitting = input(false);
  readonly doneRequested = output<void>();
  readonly noShowRequested = output<void>();

  protected readonly calledToText = computed(() => {
    const counterName = this.student()?.counterName;
    return counterName ? `Called to ${counterName}` : 'Called';
  });
}
