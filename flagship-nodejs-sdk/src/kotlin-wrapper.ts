/**
 * TypeScript wrapper for Kotlin/JS compiled Flagship core.
 * 
 * This module provides type-safe access to Kotlin/JS compiled code,
 * allowing Node.js SDK to use shared logic for:
 * - Deterministic bucketing (MurmurHash3)
 * - Experiment assignment
 * - Backoff calculations
 * 
 * The Kotlin code is compiled to JavaScript and can be imported as an ES module.
 */

export interface VariantData {
  name: string;
  weight: number;
}

export interface ContextData {
  userId: string;
  deviceId?: string;
  appVersion?: string;
  osName?: string;
  osVersion?: string;
  locale?: string;
  region?: string;
  attributes?: Record<string, string>;
}

export interface ExperimentAssignmentResult {
  variant: string;
  hash?: string;
  payload?: Record<string, string>;
}

export interface FlagshipJsExports {
  assignExperiment(
    experimentKey: string,
    userId: string,
    variants: VariantData[]
  ): string | null;
  
  assignExperimentWithContext(
    experimentKey: string,
    contextData: ContextData,
    variants: VariantData[],
    targeting?: string | null
  ): ExperimentAssignmentResult | null;
  
  calculateBackoff(
    attempt: number,
    initialDelayMs: number,
    maxDelayMs: number,
    factor?: number
  ): number;
  
  nextBackoff(
    currentDelay: number,
    maxDelayMs: number,
    factor?: number
  ): number;
  
  isInBucket(userId: string, percent: number): boolean;
}

let kotlinExports: FlagshipJsExports | null = null;

/**
 * Load Kotlin/JS compiled code.
 * 
 * @returns FlagshipJsExports if available, null otherwise
 */
export async function loadKotlinCode(): Promise<FlagshipJsExports | null> {
  if (kotlinExports) {
    return kotlinExports;
  }
  
  try {
    // Try to load compiled Kotlin/JS code
    // The path depends on how the code is packaged
    const kotlinModule = await import('../kotlin/flagship-core.js');
    
    // Kotlin/JS exports are typically under a namespace
    // Adjust based on actual export structure
    if (kotlinModule.FlagshipJsExports) {
      kotlinExports = kotlinModule.FlagshipJsExports as FlagshipJsExports;
      return kotlinExports;
    }
    
    // Alternative: direct export
    if (kotlinModule.assignExperiment) {
      kotlinExports = kotlinModule as FlagshipJsExports;
      return kotlinExports;
    }
    
    return null;
  } catch (error) {
    console.warn('Kotlin/JS code not available, using fallback implementations', error);
    return null;
  }
}

/**
 * Check if Kotlin code is available.
 */
export function isKotlinCodeAvailable(): boolean {
  return kotlinExports !== null;
}

