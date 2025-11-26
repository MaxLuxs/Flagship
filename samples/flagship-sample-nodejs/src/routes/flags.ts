import { Router, Request, Response } from 'express';
import { getFlag, isFlagEnabled, mockFlags } from '../mock-data';

const router = Router();

/**
 * GET /api/flags
 * List all available flags.
 */
router.get('/', (req: Request, res: Response) => {
  try {
    const flags = Object.entries(mockFlags).map(([key, value]) => ({
      key,
      type: value.type,
      value: value.value
    }));

    res.json({
      success: true,
      flags,
      count: flags.length
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to retrieve flags',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * GET /api/flags/:key
 * Get a specific flag value by key.
 */
router.get('/:key', (req: Request, res: Response) => {
  try {
    const { key } = req.params;
    const flag = getFlag(key);

    if (!flag) {
      return res.status(404).json({
        success: false,
        error: 'Flag not found',
        key
      });
    }

    res.json({
      success: true,
      key,
      type: flag.type,
      value: flag.value
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to retrieve flag',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * GET /api/flags/:key/enabled
 * Check if a boolean flag is enabled.
 */
router.get('/:key/enabled', (req: Request, res: Response) => {
  try {
    const { key } = req.params;
    const defaultValue = req.query.default === 'true';
    const enabled = isFlagEnabled(key, defaultValue);

    res.json({
      success: true,
      key,
      enabled,
      defaultValue
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to check flag status',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;

