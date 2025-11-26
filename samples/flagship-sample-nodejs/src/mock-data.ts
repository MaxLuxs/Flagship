import { FlagValue, ExperimentAssignment } from '@flagship/nodejs-sdk';

/**
 * Mock flags provider for demo purposes.
 * Returns predefined flags and experiments without requiring a real backend.
 */

export interface MockExperiment {
  key: string;
  variants: Array<{
    name: string;
    weight: number;
    payload?: Record<string, string>;
  }>;
  targeting?: any;
  exposureType?: string;
}

/**
 * Mock flags data matching the Kotlin MockFlagsProvider.
 */
export const mockFlags: Record<string, FlagValue> = {
  new_feature: { type: 'bool', value: true },
  dark_mode: { type: 'bool', value: false },
  max_retries: { type: 'int', value: 3 },
  api_timeout: { type: 'double', value: 30.0 },
  welcome_message: { type: 'string', value: 'Welcome to Flagship Demo!' },
  payment_enabled: { type: 'bool', value: true }
};

/**
 * Mock experiments data matching the Kotlin MockFlagsProvider.
 */
export const mockExperiments: Record<string, MockExperiment> = {
  test_experiment: {
    key: 'test_experiment',
    variants: [
      { name: 'control', weight: 0.5 },
      { name: 'treatment', weight: 0.5 }
    ],
    exposureType: 'onAssign'
  },
  checkout_flow: {
    key: 'checkout_flow',
    variants: [
      { name: 'control', weight: 0.33 },
      { name: 'variant_a', weight: 0.33 },
      { name: 'variant_b', weight: 0.34 }
    ],
    targeting: {
      type: 'composite',
      all: [
        { type: 'app_version_gte', value: '1.0.0' }
      ]
    },
    exposureType: 'onAssign'
  }
};

/**
 * Simple hash function for deterministic experiment assignment.
 * Matches the logic from Kotlin MockFlagsProvider.
 */
export function hashCode(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  return hash;
}

/**
 * Assign a variant for an experiment based on user ID.
 * Uses deterministic bucketing similar to Kotlin MockFlagsProvider.
 */
export function assignExperiment(
  experimentKey: string,
  userId: string,
  experiment: MockExperiment
): ExperimentAssignment | null {
  if (!experiment || !experiment.variants || experiment.variants.length === 0) {
    return null;
  }

  // Simple bucketing based on userId hash
  const hash = hashCode(userId + experimentKey);
  const bucket = Math.abs(hash) % 100;

  let cumulative = 0.0;
  for (const variant of experiment.variants) {
    cumulative += variant.weight * 100;
    if (bucket < cumulative) {
      return {
        key: experimentKey,
        variant: variant.name,
        payload: variant.payload || {}
      };
    }
  }

  // Fallback to first variant
  const firstVariant = experiment.variants[0];
  return {
    key: experimentKey,
    variant: firstVariant.name,
    payload: firstVariant.payload || {}
  };
}

/**
 * Get flag value by key.
 */
export function getFlag(key: string): FlagValue | null {
  return mockFlags[key] || null;
}

/**
 * Get experiment by key.
 */
export function getExperiment(key: string): MockExperiment | null {
  return mockExperiments[key] || null;
}

/**
 * Check if a flag is enabled (boolean flag).
 */
export function isFlagEnabled(key: string, defaultValue: boolean = false): boolean {
  const flag = mockFlags[key];
  if (!flag || flag.type !== 'bool') {
    return defaultValue;
  }
  return flag.value === true;
}

