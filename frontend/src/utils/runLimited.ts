export type LimitedResult<T> =
  | { status: 'fulfilled'; value: T }
  | { status: 'rejected'; reason: unknown };

function abortError() {
  return new DOMException('Aborted', 'AbortError');
}

export async function runLimited<T, R>(
  items: readonly T[],
  limit: number,
  worker: (item: T, index: number) => Promise<R>,
  signal?: AbortSignal,
): Promise<Array<LimitedResult<R>>> {
  if (signal?.aborted) {
    throw abortError();
  }

  const results = new Array<LimitedResult<R>>(items.length);
  let nextIndex = 0;
  const runWorker = async () => {
    while (nextIndex < items.length) {
      if (signal?.aborted) {
        throw abortError();
      }
      const index = nextIndex;
      nextIndex += 1;
      try {
        results[index] = {
          status: 'fulfilled',
          value: await worker(items[index]!, index),
        };
      } catch (reason) {
        if (signal?.aborted) {
          throw abortError();
        }
        results[index] = { status: 'rejected', reason };
      }
    }
  };

  const workerCount = Math.min(Math.max(1, limit), items.length);
  await Promise.all(Array.from({ length: workerCount }, runWorker));
  return results;
}
