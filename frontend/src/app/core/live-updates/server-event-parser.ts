export interface ServerEvent {
  name: string;
  data: string;
}

export const COMMENT_EVENT_NAME = 'comment';
const DEFAULT_EVENT_NAME = 'message';
const LINE_ENDING = /\r\n|\n|\r/g;

export class ServerEventParser {
  private pendingText = '';
  private eventName = DEFAULT_EVENT_NAME;
  private dataLines: string[] = [];

  readEventsFrom(textChunk: string): ServerEvent[] {
    this.pendingText += textChunk;
    const events: ServerEvent[] = [];
    for (const line of this.takeCompleteLines()) {
      const event = this.readLine(line);
      if (event !== null) {
        events.push(event);
      }
    }
    return events;
  }

  private takeCompleteLines(): string[] {
    const lines: string[] = [];
    const lineEnding = new RegExp(LINE_ENDING);
    let lineStart = 0;
    let lineEndingMatch = lineEnding.exec(this.pendingText);
    while (lineEndingMatch !== null) {
      const matchEnd = lineEndingMatch.index + lineEndingMatch[0].length;
      const mightBeFirstHalfOfCarriageReturnLineFeed =
        lineEndingMatch[0] === '\r' && matchEnd === this.pendingText.length;
      if (mightBeFirstHalfOfCarriageReturnLineFeed) {
        break;
      }
      lines.push(this.pendingText.slice(lineStart, lineEndingMatch.index));
      lineStart = matchEnd;
      lineEndingMatch = lineEnding.exec(this.pendingText);
    }
    this.pendingText = this.pendingText.slice(lineStart);
    return lines;
  }

  private readLine(line: string): ServerEvent | null {
    if (line === '') {
      return this.finishEvent();
    }
    if (line.startsWith(':')) {
      return { name: COMMENT_EVENT_NAME, data: '' };
    }
    const separatorIndex = line.indexOf(':');
    const fieldName = separatorIndex === -1 ? line : line.slice(0, separatorIndex);
    const fieldValue = separatorIndex === -1 ? '' : this.valueAfterSeparator(line, separatorIndex);
    this.storeField(fieldName, fieldValue);
    return null;
  }

  private valueAfterSeparator(line: string, separatorIndex: number): string {
    const value = line.slice(separatorIndex + 1);
    return value.startsWith(' ') ? value.slice(1) : value;
  }

  private storeField(fieldName: string, fieldValue: string): void {
    if (fieldName === 'event') {
      this.eventName = fieldValue;
    } else if (fieldName === 'data') {
      this.dataLines.push(fieldValue);
    }
  }

  private finishEvent(): ServerEvent | null {
    const event =
      this.dataLines.length === 0
        ? null
        : { name: this.eventName, data: this.dataLines.join('\n') };
    this.eventName = DEFAULT_EVENT_NAME;
    this.dataLines = [];
    return event;
  }
}
