import { Router, Request, Response } from 'express';
import { getExperiment, assignExperiment, mockExperiments } from '../mock-data';

const router = Router();

/**
 * GET /api/experiments
 * List all available experiments.
 */
router.get('/', (req: Request, res: Response) => {
  try {
    const experiments = Object.entries(mockExperiments).map(([key, experiment]) => ({
      key,
      variants: experiment.variants,
      targeting: experiment.targeting,
      exposureType: experiment.exposureType
    }));

    res.json({
      success: true,
      experiments,
      count: experiments.length
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to retrieve experiments',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

/**
 * GET /api/experiments/:key
 * Get experiment assignment for a specific experiment key.
 * Uses userId from query parameter or defaults to 'anonymous'.
 */
router.get('/:key', (req: Request, res: Response) => {
  try {
    const { key } = req.params;
    const userId = (req.query.userId as string) || 'anonymous';
    
    const experiment = getExperiment(key);
    
    if (!experiment) {
      return res.status(404).json({
        success: false,
        error: 'Experiment not found',
        key
      });
    }

    const assignment = assignExperiment(key, userId, experiment);

    if (!assignment) {
      return res.status(500).json({
        success: false,
        error: 'Failed to assign experiment variant',
        key
      });
    }

    res.json({
      success: true,
      assignment,
      userId,
      experiment: {
        key: experiment.key,
        variants: experiment.variants,
        targeting: experiment.targeting
      }
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Failed to get experiment assignment',
      message: error instanceof Error ? error.message : 'Unknown error'
    });
  }
});

export default router;

