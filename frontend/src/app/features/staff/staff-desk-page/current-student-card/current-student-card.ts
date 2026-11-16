import { Component, computed, input, output } from '@angular/core';

import { CalledStudent } from '../../../../core/models/staff-desk.model';
import { AppIcon } from '../../../../shared/icon/app-icon';

const NO_STUDENT_MESSAGE = 'No student at your desk';

@Component({
  selector: 'app-current-student-card',
  imports: [AppIcon],
  templateUrl: './current-student-card.html',
  styleUrl: './current-student-card.scss',
})
export class CurrentStudentCard {
  readonly student = input.required<CalledStudent | null>();
  readonly nobodyWaitingMessage = input<string | null>(null);
  readonly isSubmitting = input(false);
  readonly doneRequested = output<void>();
  readonly noShowRequested = output<void>();

  protected readonly idleMessage = computed(() => this.nobodyWaitingMessage() ?? NO_STUDENT_MESSAGE);

  protected readonly calledToText = computed(() => {
    const counterName = this.student()?.counterName;
    return counterName ? `Called to ${counterName}` : 'Called';
  });
}
