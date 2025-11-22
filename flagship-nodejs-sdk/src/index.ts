import axios, { AxiosInstance } from 'axios';

export interface FlagshipConfig {
  apiKey: string;
  baseUrl?: string;
  environment?: string;
  cache?: FlagsCache;
}

export interface FlagsCache {
  get(key: string): Promise<string | null>;
  set(key: string, value: string): Promise<void>;
  clear(): Promise<void>;
}

class InMemoryCache implements FlagsCache {
  private cache: Map<string, string> = new Map();

  async get(key: string): Promise<string | null> {
    return this.cache.get(key) || null;
  }

  async set(key: string, value: string): Promise<void> {
    this.cache.set(key, value);
  }

  async clear(): Promise<void> {
    this.cache.clear();
  }
}

export interface FlagValue {
  type: 'bool' | 'int' | 'double' | 'string' | 'json';
  value: any;
}

export interface ExperimentAssignment {
  key: string;
  variant: string;
  payload?: Record<string, string>;
}

export interface ConfigResponse {
  revision: string;
  fetchedAt: number;
  ttlMs: number;
  flags: Record<string, FlagValue>;
  experiments: Record<string, any>;
}

/**
 * Flagship SDK for Node.js
 * 
 * Usage:
 * ```typescript
 * import { Flagship } from '@flagship/nodejs-sdk';
 * 
 * const flagship = new Flagship({
 *   apiKey: 'your-api-key',
 *   baseUrl: 'https://api.example.com/flags'
 * });
 * 
 * await flagship.init();
 * 
 * if (flagship.isEnabled('new_ui')) {
 *   // Use new UI
 * }
 * 
 * const variant = flagship.experiment('checkout_flow')?.variant;
 * ```
 */
export class Flagship {
  private config: FlagshipConfig;
  private httpClient: AxiosInstance;
  private cache: FlagsCache;
  private flags: Map<string, FlagValue> = new Map();
  private experiments: Map<string, any> = new Map();
  private revision: string | null = null;
  private initialized: boolean = false;

  constructor(config: FlagshipConfig) {
    this.config = {
      baseUrl: 'https://api.example.com/flags',
      environment: 'production',
      cache: new InMemoryCache(),
      ...config
    };
    
    this.httpClient = axios.create({
      baseURL: this.config.baseUrl,
      headers: {
        'Authorization': `Bearer ${this.config.apiKey}`
      }
    });
    
    this.cache = this.config.cache || new InMemoryCache();
  }

  /**
   * Initialize Flagship and load flags.
   */
  async init(): Promise<void> {
    if (this.initialized) {
      return;
    }

    try {
      // Try to load from cache first
      const cached = await this.cache.get('flagship_config');
      if (cached) {
        const config: ConfigResponse = JSON.parse(cached);
        this.updateFromConfig(config);
      }
    } catch (e) {
      // Cache miss or invalid, continue to fetch
    }

    await this.refresh();
    this.initialized = true;
  }

  /**
   * Refresh flags from server.
   */
  async refresh(): Promise<void> {
    try {
      const url = this.revision 
        ? `/config?rev=${this.revision}`
        : '/config';
      
      const response = await this.httpClient.get<ConfigResponse>(url);
      const config = response.data;
      
      this.updateFromConfig(config);
      
      // Cache the config
      await this.cache.set('flagship_config', JSON.stringify(config));
    } catch (error) {
      console.error('Failed to refresh flags:', error);
      // Continue with cached values
    }
  }

  private updateFromConfig(config: ConfigResponse): void {
    this.revision = config.revision;
    this.flags.clear();
    this.experiments.clear();
    
    Object.entries(config.flags).forEach(([key, value]) => {
      this.flags.set(key, value);
    });
    
    Object.entries(config.experiments).forEach(([key, value]) => {
      this.experiments.set(key, value);
    });
  }

  /**
   * Check if a flag is enabled.
   */
  isEnabled(key: string, defaultValue: boolean = false): boolean {
    const flag = this.flags.get(key);
    if (!flag || flag.type !== 'bool') {
      return defaultValue;
    }
    return flag.value === true;
  }

  /**
   * Get a typed flag value.
   */
  get<T>(key: string, defaultValue: T): T {
    const flag = this.flags.get(key);
    if (!flag) {
      return defaultValue;
    }
    
    return flag.value as T;
  }

  /**
   * Get experiment assignment.
   * 
   * Uses Kotlin/JS compiled BucketingEngine for deterministic assignment
   * with MurmurHash3, ensuring consistency across platforms.
   */
  async experiment(key: string): Promise<ExperimentAssignment | null> {
    const experiment = this.experiments.get(key);
    if (!experiment) {
      return null;
    }
    
    const variants = experiment.variants || [];
    if (variants.length === 0) {
      return null;
    }
    
    // Try to use Kotlin/JS compiled code
    const { loadKotlinCode } = await import('./kotlin-wrapper');
    const kotlinCode = await loadKotlinCode();
    
    if (kotlinCode) {
      try {
        const variantData = variants.map((v: any) => ({
          name: v.name || v,
          weight: v.weight || (1.0 / variants.length)
        }));
        
        const userId = this.config.apiKey; // Use API key as user identifier for now
        const variantName = kotlinCode.assignExperiment(
          key,
          userId,
          variantData
        );
        
        if (!variantName) {
          return null;
        }
        
        const variant = variants.find((v: any) => (v.name || v) === variantName) || variants[0];
        
        return {
          key,
          variant: variantName,
          payload: variant.payload || {}
        };
      } catch (e) {
        console.warn('Error using Kotlin code, falling back', e);
        return this.experimentFallback(key, variants);
      }
    }
    
    // Fallback to simple hash if Kotlin code not available
    return this.experimentFallback(key, variants);
  }

  private experimentFallback(key: string, variants: any[]): ExperimentAssignment | null {
    const hash = this.hashCode(key + this.config.apiKey);
    const index = Math.abs(hash) % variants.length;
    const variant = variants[index];
    
    return {
      key,
      variant: variant.name || variant,
      payload: variant.payload || {}
    };
  }

  private hashCode(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
  }
}

// Export singleton instance helper
let defaultInstance: Flagship | null = null;

export function init(config: FlagshipConfig): Promise<Flagship> {
  defaultInstance = new Flagship(config);
  return defaultInstance.init().then(() => defaultInstance!);
}

export function getInstance(): Flagship {
  if (!defaultInstance) {
    throw new Error('Flagship not initialized. Call init() first.');
  }
  return defaultInstance;
}

// Convenience functions
export async function isEnabled(key: string, defaultValue: boolean = false): Promise<boolean> {
  const instance = getInstance();
  await instance.init();
  return instance.isEnabled(key, defaultValue);
}

export async function get<T>(key: string, defaultValue: T): Promise<T> {
  const instance = getInstance();
  await instance.init();
  return instance.get(key, defaultValue);
}

export async function experiment(key: string): Promise<ExperimentAssignment | null> {
  const instance = getInstance();
  await instance.init();
  return await instance.experiment(key);
}

