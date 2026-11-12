export class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) { super(message); this.status = status; }
}
type Options = { token?: string; method?: string; body?: unknown; signal?: AbortSignal; headers?: Record<string, string> };
export async function api<T>(path: string, options: Options = {}): Promise<T> {
    const timeout = new AbortController();
    const timer = window.setTimeout(() => timeout.abort(), 20000);
    const signal = options.signal ? AbortSignal.any([options.signal, timeout.signal]) : timeout.signal;
    try {
        const response = await fetch('/api' + path, {
            method: options.method || (options.body ? 'POST' : 'GET'), signal,
            headers: {
                ...(options.token ? { Authorization: `Bearer ${options.token}` } : {}),
                ...(options.body ? { 'Content-Type': 'application/json' } : {}),
                ...options.headers,
            },
            ...(options.body ? { body: JSON.stringify(options.body) } : {}),
        });
        if (!response.ok) {
            const error = await response.json().catch(() => null);
            throw new ApiError(response.status, response.status === 401
                ? options.token ? 'Your session has expired. Sign in again to continue.' : 'Invalid email or password.'
                : error?.message || `Request failed (${response.status}). Please try again.`);
        }
        const text = await response.text();
        return text ? JSON.parse(text) as T : undefined as T;
    } catch (error) {
        if (error instanceof ApiError || options.signal?.aborted) throw error;
        throw new Error(timeout.signal.aborted
            ? 'The request timed out. Please try again.'
            : 'Cannot reach the store. Check your connection and try again.');
    } finally { window.clearTimeout(timer); }
}

