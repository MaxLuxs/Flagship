import { Router, Request, Response } from 'express';
import { isFlagEnabled, assignExperiment, getExperiment, getFlag } from '../mock-data';

const router = Router();

/**
 * GET /api/demo/checkout
 * Demo endpoint showing how to use checkout_flow experiment.
 * Returns different checkout flow configurations based on experiment assignment.
 */
router.get('/checkout', (req: Request, res: Response) => {
  try {
    const userId = (req.query.userId as string) || 'anonymous';
    const experiment = getExperiment('checkout_flow');

    if (!experiment) {
      return res.status(500).json({
        success: false,
        error: 'checkout_flow experiment not found'
      });
    }

    const assignment = assignExperiment('checkout_flow', userId, experiment);

    if (!assignment) {
      return res.status(500).json({
        success: false,
        error: 'Failed to assign checkout flow variant'
      });
    }

    // Different checkout flows based on variant
    const flows: Record<string, any> = {
      control: {
        name: 'Standard Checkout',
        steps: ['cart', 'shipping', 'payment', 'review', 'confirmation'],
        features: {
          expressCheckout: false,
          guestCheckout: true,
          saveForLater: true
        }
      },
      variant_a: {
        name: 'Streamlined Checkout',
        steps: ['cart', 'payment', 'confirmation'],
        features: {
          expressCheckout: true,
          guestCheckout: true,
          saveForLater: false
        }
      },
      variant_b: {
        name: 'Enhanced Checkout',
        steps: ['cart', 'shipping', 'payment', 'review', 'upsell', 'confirmation'],
        features: {
          expressCheckout: true,
          guestCheckout: true,
          saveForLater: true,
          upsell: true
        }
      }
    };

    const flow = flows[assignment.variant] || flows.control;

    res.json({
      success: true,
      userId,
      variant: assignment.variant,
      checkoutFlow: flow,
      assignment
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to get checkout flow',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * GET /api/demo/feature
 * Demo endpoint showing how to use new_feature flag.
 * Returns different feature configurations based on flag value.
 */
router.get('/feature', (req: Request, res: Response) => {
  try {
    const featureEnabled = isFlagEnabled('new_feature', false);
    const paymentEnabled = isFlagEnabled('payment_enabled', false);
    
    // Get flag values
    const maxRetriesFlag = getFlag('max_retries');
    const maxRetries = maxRetriesFlag && maxRetriesFlag.type === 'int' 
      ? maxRetriesFlag.value as number 
      : 1;
    
    const apiTimeoutFlag = getFlag('api_timeout');
    const apiTimeout = apiTimeoutFlag && apiTimeoutFlag.type === 'double'
      ? apiTimeoutFlag.value as number
      : 30.0;
    
    const welcomeMessageFlag = getFlag('welcome_message');
    const welcomeMessage = welcomeMessageFlag && welcomeMessageFlag.type === 'string'
      ? welcomeMessageFlag.value as string
      : 'Welcome to Flagship Demo!';

    const featureConfig = {
      newFeature: {
        enabled: featureEnabled,
        description: 'New experimental feature',
        endpoints: featureEnabled 
          ? ['/api/v2/new-endpoint', '/api/v2/advanced-feature']
          : ['/api/v1/legacy-endpoint']
      },
      payment: {
        enabled: paymentEnabled,
        maxRetries,
        timeout: apiTimeout
      },
      ui: {
        welcomeMessage,
        theme: isFlagEnabled('dark_mode', false) ? 'dark' : 'light'
      }
    };

    res.json({
      success: true,
      featureConfig,
      flags: {
        new_feature: featureEnabled,
        payment_enabled: paymentEnabled,
        dark_mode: isFlagEnabled('dark_mode', false),
        max_retries: maxRetries,
        api_timeout: apiTimeout,
        welcome_message: welcomeMessage
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to get feature configuration',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;

