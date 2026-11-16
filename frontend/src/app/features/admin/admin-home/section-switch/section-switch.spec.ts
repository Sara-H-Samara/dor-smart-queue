import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SectionSwitch } from './section-switch';

describe('SectionSwitch', () => {
  let fixture: ComponentFixture<SectionSwitch>;
  let switchElement: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(SectionSwitch);
    fixture.componentRef.setInput('sections', [
      { value: 'services', label: 'Services' },
      { value: 'counters', label: 'Counters' },
    ]);
    fixture.componentRef.setInput('groupName', 'admin-section');
    fixture.componentRef.setInput('groupLabel', 'Section');
    fixture.componentRef.setInput('selectedSection', 'services');
    switchElement = fixture.nativeElement as HTMLElement;
  });

  function radioInputs(): HTMLInputElement[] {
    return Array.from(switchElement.querySelectorAll<HTMLInputElement>('input[type="radio"]'));
  }

  it('render_withTwoSections_showsBothLabelsWithFirstSelected', () => {
    fixture.detectChanges();

    const labels = Array.from(switchElement.querySelectorAll('label')).map((label) =>
      label.textContent?.trim(),
    );
    expect(labels).toEqual(['Services', 'Counters']);
    expect(radioInputs().map((radio) => radio.checked)).toEqual([true, false]);
  });

  it('clickSecondSection_withFirstSelected_updatesSelectedSection', () => {
    fixture.detectChanges();

    radioInputs()[1].click();

    expect(fixture.componentInstance.selectedSection()).toBe('counters');
  });

  it('render_withSelectedSection_checksOnlyThatRadio', () => {
    fixture.componentRef.setInput('selectedSection', 'counters');

    fixture.detectChanges();

    expect(radioInputs().map((radio) => radio.checked)).toEqual([false, true]);
  });
});
