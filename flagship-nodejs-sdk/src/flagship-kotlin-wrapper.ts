/**
 * TypeScript wrapper for Kotlin/JS compiled Flagship core.
 * 
 * This file provides a bridge between TypeScript and Kotlin/JS compiled code.
 * The Kotlin code is compiled to JavaScript and provides the core logic,
 * while this wrapper provides a TypeScript-friendly API.
 */

// Import compiled Kotlin/JS code
// @ts-ignore - Kotlin/JS generated code
import * as FlagshipCore from '../kotlin/flagship-core.js';

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

export interface FlagValue {
  type: 'bool' | 'int' | 'double' | 'string' | 'json';
  value: any;
}

export interface ExperimentAssignment {
  key: string;
  variant: string;
  payload?: Record<string, string>;
}

/**
 * TypeScript wrapper for Flagship using Kotlin/JS core.
 * 
 * This class wraps the Kotlin/JS compiled Flagship core,
 * providing a TypeScript-friendly API while using shared Kotlin logic.
 */
export class FlagshipKotlinWrapper {
  private kotlinFlagship: any; // Kotlin Flagship object
  private initialized: boolean = false;

  constructor(config: FlagshipConfig) {
    // Initialize Kotlin Flagship with config
    // This will use the shared Kotlin logic for evaluation, bucketing, etc.
    this.kotlinFlagship = FlagshipCore.Flagship.Companion.init(
      config.apiKey,
      this.createKotlinConfig(config)
    );
  }

  private createKotlinConfig(config: FlagshipConfig): any {
    // Convert TypeScript config to Kotlin FlagshipConfig
    // This uses the shared Kotlin configuration builder
    return FlagshipCore.FlagshipConfigBuilder.build(
      config.appKey || config.apiKey,
      config.environment || 'production',
      [], // providers - will be set up separately
      this.createKotlinCache(config.cache)
    );
  }

  private createKotlinCache(cache?: FlagsCache): any {
    if (cache) {
      // Wrap TypeScript cache in Kotlin-compatible interface
      return new (FlagshipCore as any).TypeScriptCacheAdapter(cache);
    }
    // Use default Kotlin InMemoryCache
    return new FlagshipCore.InMemoryCache();
  }

  async init(): Promise<void> {
    if (this.initialized) {
      return;
    }

    // Use Kotlin's bootstrap logic
    await this.kotlinFlagship.manager().ensureBootstrap(5000);
    this.initialized = true;
  }

  isEnabled(key: string, defaultValue: boolean = false): boolean {
    // Use Kotlin's evaluation logic
    return this.kotlinFlagship.isEnabled(key, defaultValue);
  }

  get<T>(key: string, defaultValue: T): T {
    // Use Kotlin's type-safe value retrieval
    return this.kotlinFlagship.get(key, defaultValue);
  }

  experiment(key: string): ExperimentAssignment | null {
    // Use Kotlin's BucketingEngine for deterministic assignment
    const assignment = this.kotlinFlagship.experiment(key);
    if (!assignment) {
      return null;
    }

    return {
      key: assignment.key,
      variant: assignment.variant,
      payload: assignment.payload
    };
  }

  async refresh(): Promise<void> {
    // Use Kotlin's refresh logic
    this.kotlinFlagship.manager().refresh();
  }
}

