import { describe, expect, it } from 'vitest';
import { runLimited } from '../utils/runLimited';

describe('runLimited', () => {
  it('never runs more than the requested number of workers', async () => {
    let active = 0;
    let maxActive = 0;
    const results = await runLimited([1, 2, 3, 4, 5], 2, async (value) => {
      active += 1;
      maxActive = Math.max(maxActive, active);
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      active -= 1;
      return value * 2;
    });

    expect(results).toHaveLength(5);
    expect(maxActive).toBe(2);
  });

  it('preserves input order and isolates a rejected item', async () => {
    const results = await runLimited([3, 1, 2], 2, async (value) => {
      if (value === 1) throw new Error('row failed');
      return value * 10;
    });

    expect(results[0]).toEqual({ status: 'fulfilled', value: 30 });
    expect(results[1]).toMatchObject({ status: 'rejected' });
    expect(results[2]).toEqual({ status: 'fulfilled', value: 20 });
  });

  it('stops after abort', async () => {
    const controller = new AbortController();
    controller.abort();

    await expect(runLimited([1], 1, async (value) => value, controller.signal))
      .rejects.toMatchObject({ name: 'AbortError' });
  });
});
