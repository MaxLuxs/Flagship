# GIF Demonstrations Checklist

This checklist tracks which animated GIF demonstrations have been created.

## Core Workflows

- [ ] `01-create-flag.gif` - Complete flag creation flow
  - Click "Create Flag" button
  - Fill form (key, type, value, description)
  - Toggle isEnabled
  - Click Save
  - See flag appear in list
  - Duration: ~10 seconds

- [ ] `02-toggle-flag.gif` - Toggle flag on/off
  - Show flags list
  - Click toggle switch to enable flag
  - See status change immediately
  - Click toggle switch to disable flag
  - See status change
  - Duration: ~8 seconds

- [ ] `03-create-experiment.gif` - Create experiment flow
  - Click "Create Experiment"
  - Fill experiment key
  - Add variants (control, variant A, variant B)
  - Set weights (auto-normalize)
  - Configure targeting (optional)
  - Save and see in list
  - Duration: ~15 seconds

- [ ] `04-edit-experiment.gif` - Edit experiment
  - Click on experiment
  - Edit variant weights
  - See automatic normalization
  - Update targeting rules
  - Save changes
  - Duration: ~12 seconds

## API Keys Workflow

- [ ] `05-create-api-key.gif` - Create and copy API key
  - Navigate to API Keys tab
  - Click "Create API Key"
  - Select type (READ_ONLY or ADMIN)
  - Set expiration (optional)
  - Create key
  - Show key in dialog
  - Click copy button
  - Show toast "Copied to clipboard"
  - Duration: ~12 seconds

## Provider Analytics

- [ ] `06-provider-analytics.gif` - View provider health
  - Navigate to Providers tab
  - Show list of providers with health status
  - Click on provider to expand details
  - Show metrics (success rate, response time, etc.)
  - Show metrics history
  - Duration: ~15 seconds

## Search and Filter

- [ ] `07-search-filter-flags.gif` - Search and filter flags
  - Show flags list
  - Type in search box
  - See filtered results
  - Change filter dropdown (by type)
  - See filtered results
  - Clear search/filter
  - Duration: ~10 seconds

## Project Management

- [ ] `08-project-settings.gif` - Project settings workflow
  - Navigate to Settings tab
  - Edit project name
  - Add new member (enter email, select role)
  - See member added to list
  - Change member role
  - Remove member (with confirmation)
  - Duration: ~20 seconds

## Complete Workflow

- [ ] `09-full-workflow.gif` - Complete user journey
  - Login screen
  - Dashboard (create project if empty)
  - Create project
  - Navigate to project
  - Create flag
  - Toggle flag
  - Create experiment
  - View providers
  - Duration: ~30 seconds (can be sped up)

## Technical Requirements

- **Resolution**: 1280x720 (720p) minimum, 1920x1080 (1080p) preferred
- **Frame Rate**: 10-15 FPS (balance between smoothness and file size)
- **Duration**: 5-30 seconds per GIF
- **File Size**: Keep under 5MB for web
- **Format**: GIF (for compatibility) or WebP (for smaller size)

## Recording Tips

1. **Record at high FPS**: Record screen at 30-60 FPS, then convert to 10-15 FPS GIF
2. **Smooth cursor**: Make cursor movements smooth and deliberate
3. **Remove delays**: Cut out loading/waiting times
4. **Highlight actions**: Slow down important actions slightly
5. **Add annotations**: Optional - add text labels or arrows for clarity
6. **Loop seamlessly**: Make GIFs loop without jarring transitions

## FFmpeg Commands

```bash
# Convert video to GIF (high quality)
ffmpeg -i input.mp4 -vf "fps=15,scale=1280:-1:flags=lanczos,palettegen" -y palette.png
ffmpeg -i input.mp4 -i palette.png -lavfi "fps=15,scale=1280:-1:flags=lanczos[x];[x][1:v]paletteuse" output.gif

# Optimize GIF
gifsicle -O3 --lossy=80 -o output-optimized.gif output.gif
```

## Tools

- **Recording**: OBS Studio, ScreenFlow, QuickTime (macOS)
- **Conversion**: FFmpeg, gifski, EZGIF (online)
- **Optimization**: gifsicle, giflossy

