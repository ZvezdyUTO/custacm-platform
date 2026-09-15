import { describe, expect, it } from 'vitest';
import { readableTextColor } from '../utils/colorContrast';

describe('category text contrast', () => {
  it.each([
    ['#deef18', '#000000'],
    ['#ffffff', '#000000'],
    ['#8B1E3F', '#ffffff'],
    ['#245A73', '#ffffff'],
    ['#777777', '#000000'],
    ['#000', '#ffffff'],
    [' #FFF ', '#000000'],
  ])('chooses readable text for %s', (background, foreground) => {
    expect(readableTextColor(background)).toBe(foreground);
  });

  it('retains white text for the existing dark HSL tag fallback', () => {
    expect(readableTextColor('hsl(220 80% 24%)')).toBe('#ffffff');
  });
});
