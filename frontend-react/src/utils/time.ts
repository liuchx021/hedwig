import dayjs from 'dayjs';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

dayjs.extend(utc);
dayjs.extend(timezone);

export function parseTimestampMs(ts: string): number | null {
  const num = Number(ts);
  if (!isNaN(num)) {
    return num < 1_000_000_000_000 ? num * 1000 : num;
  }
  const d = dayjs(ts);
  return d.isValid() ? d.valueOf() : null;
}

export function formatShanghaiDatetime(ts: string): string {
  const ms = parseTimestampMs(ts);
  if (ms === null) return ts;
  return dayjs(ms).tz('Asia/Shanghai').format('YYYY-MM-DD HH:mm');
}

export function formatRemainingTime(expiresAt: string): string {
  const expiresMs = parseTimestampMs(expiresAt);
  if (expiresMs === null) return '\u672a\u77e5';
  const remainingSec = Math.floor((expiresMs - Date.now()) / 1000);
  if (remainingSec <= 0) return '\u5df2\u8fc7\u671f';
  const days = Math.floor(remainingSec / 86400);
  const hours = Math.floor((remainingSec % 86400) / 3600);
  if (days > 0) return `${days}\u5929${hours}\u5c0f\u65f6`;
  return `${hours}\u5c0f\u65f6`;
}

export function trendArrow(trend?: string): string {
  switch (trend) {
    case 'DOUBLE_UP': return '\u2191';
    case 'SINGLE_UP': return '\u2197';
    case 'FORTY_FIVE_UP': return '\u2197';
    case 'FLAT': return '\u2192';
    case 'FORTY_FIVE_DOWN': return '\u2198';
    case 'SINGLE_DOWN': return '\u2193';
    case 'DOUBLE_DOWN': return '\u2193\u2193';
    default: return '\u2014';
  }
}

export function glucoseClass(mmol: number): 'low' | 'high' | 'normal' {
  if (mmol < 3.9) return 'low';
  if (mmol > 10.0) return 'high';
  return 'normal';
}

export function glucoseColor(mmol: number): string {
  if (mmol < 3.9) return '#e0a84b';
  if (mmol > 10.0) return '#e05c5c';
  return '#4ecdc4';
}
