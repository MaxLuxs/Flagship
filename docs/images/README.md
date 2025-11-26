# Visual Materials for Flagship

This directory contains visual materials (screenshots, GIFs, videos) for marketing, documentation, and README.

## Directory Structure

```
images/
├── screenshots/     # Static screenshots of admin UI
├── gifs/           # Animated GIF demonstrations
└── flagship_icon.svg  # Logo (already exists)
```

## Screenshots Checklist

### Required Screenshots

#### Authentication
- [ ] `01-login-screen.png` - Login screen
- [ ] `02-register-screen.png` - Registration screen

#### Dashboard
- [ ] `03-dashboard-empty.png` - Empty dashboard (no projects)
- [ ] `04-dashboard-with-projects.png` - Dashboard with multiple projects
- [ ] `05-create-project-dialog.png` - Create project dialog

#### Flags Management
- [ ] `06-flags-screen-empty.png` - Empty flags list
- [ ] `07-flags-screen-with-flags.png` - Flags list with multiple flags
- [ ] `08-create-flag-dialog.png` - Create flag dialog (all types: bool, string, number, json)
- [ ] `09-edit-flag-dialog.png` - Edit flag dialog
- [ ] `10-flag-toggle-on.png` - Flag enabled (toggle switch ON)
- [ ] `11-flag-toggle-off.png` - Flag disabled (toggle switch OFF)

#### Experiments Management
- [ ] `12-experiments-screen-empty.png` - Empty experiments list
- [ ] `13-experiments-screen-with-experiments.png` - Experiments list
- [ ] `14-create-experiment-dialog.png` - Create experiment dialog
- [ ] `15-edit-experiment-dialog.png` - Edit experiment dialog
- [ ] `16-experiment-variants-visualization.png` - Experiment with variant distribution visualization

#### API Keys
- [ ] `17-api-keys-screen.png` - API keys list
- [ ] `18-create-api-key-dialog.png` - Create API key dialog
- [ ] `19-api-key-created-dialog.png` - Show API key after creation (with copy button)

#### Providers Analytics
- [ ] `20-providers-screen.png` - Providers health monitoring screen
- [ ] `21-provider-details.png` - Provider details card (expanded)
- [ ] `22-provider-healthy.png` - Healthy provider (🟢)
- [ ] `23-provider-degraded.png` - Degraded provider (🟡)
- [ ] `24-provider-unhealthy.png` - Unhealthy provider (🔴)

#### Settings
- [ ] `25-project-settings.png` - Project settings tab
- [ ] `26-project-members.png` - Project members management
- [ ] `27-profile-settings.png` - User profile settings

## GIF Demonstrations Checklist

### Required GIFs

- [ ] `01-create-flag.gif` - Complete flow: Click "Create Flag" → Fill form → Save → See in list
- [ ] `02-toggle-flag.gif` - Toggle flag on/off → See status change immediately
- [ ] `03-create-experiment.gif` - Create experiment with variants → See visualization
- [ ] `04-provider-analytics.gif` - View provider health → Expand details → See metrics
- [ ] `05-api-key-creation.gif` - Create API key → Copy to clipboard → Use in code

### Optional GIFs (Nice to have)

- [ ] `06-search-filter-flags.gif` - Search and filter flags
- [ ] `07-project-settings.gif` - Edit project settings → Add member → Change role
- [ ] `08-full-workflow.gif` - Complete workflow: Login → Create project → Create flag → Toggle → View analytics

## Video Tutorials Checklist

### Required Videos

- [ ] `01-quick-start.mp4` (5 minutes) - Quick start guide
  - Installation
  - Basic setup
  - First flag
  - First experiment

- [ ] `02-android-integration.mp4` (10 minutes) - Android integration
  - Add dependencies
  - Initialize Flagship
  - Use flags in code
  - A/B testing example

- [ ] `03-ios-integration.mp4` (10 minutes) - iOS integration
  - SPM installation
  - Swift setup
  - Use flags in SwiftUI
  - A/B testing example

- [ ] `04-providers-setup.mp4` (15 minutes) - Providers setup
  - Firebase Remote Config setup
  - REST API provider setup
  - Multiple providers with fallback
  - Provider analytics

### Optional Videos

- [ ] `05-advanced-features.mp4` (20 minutes) - Advanced features
  - Targeting rules
  - Gradual rollouts
  - Kill switches
  - Debug dashboard

## Screenshot Guidelines

### Technical Requirements

- **Resolution**: Minimum 1920x1080 (Full HD), preferably 2560x1440 (2K) or higher
- **Format**: PNG (lossless) for screenshots, optimized for web
- **Aspect Ratio**: 16:9 for desktop/web, or native aspect ratio for mobile
- **File Size**: Optimize PNGs (use tools like `pngquant` or `optipng`)
- **Naming**: Use descriptive names with numbers for ordering

### Content Guidelines

- **Clean UI**: Remove any sensitive data, use test/demo data
- **Consistent Theme**: Use the same theme (light or dark) across all screenshots
- **Highlight Features**: Use annotations/arrows if needed to highlight key features
- **Empty States**: Include both empty and populated states
- **Error States**: Optional - include error handling screenshots

### Tools for Screenshots

- **Desktop**: 
  - macOS: Cmd+Shift+4 (area), Cmd+Shift+3 (full screen)
  - Windows: Snipping Tool, ShareX
  - Linux: Flameshot, Spectacle
- **Mobile**: 
  - Android: Built-in screenshot
  - iOS: Built-in screenshot
- **Optimization**: 
  - `pngquant` - Lossy PNG compression
  - `optipng` - Lossless PNG optimization
  - `imagemagick` - Image manipulation

## GIF Creation Guidelines

### Technical Requirements

- **Resolution**: 1280x720 (720p) minimum, 1920x1080 (1080p) preferred
- **Frame Rate**: 10-15 FPS (lower file size, still smooth)
- **Duration**: 5-15 seconds per GIF
- **File Size**: Keep under 5MB for web (use compression)
- **Format**: GIF or WebP (WebP is smaller, but GIF has better compatibility)

### Content Guidelines

- **Smooth Actions**: Record at 30-60 FPS, then convert to 10-15 FPS GIF
- **Highlight Cursor**: Make cursor visible and smooth
- **Remove Delays**: Cut out waiting/loading times
- **Add Annotations**: Optional - add text labels or arrows
- **Loop**: Make GIFs loop seamlessly

### Tools for GIF Creation

- **Recording**:
  - macOS: QuickTime Player, ScreenFlow, OBS
  - Windows: OBS, ShareX
  - Linux: OBS, SimpleScreenRecorder
- **Conversion**:
  - `ffmpeg` - Convert video to GIF
  - `gifski` - High-quality GIF encoder
  - Online: EZGIF, CloudConvert
- **Optimization**:
  - `gifsicle` - Optimize GIF file size
  - `giflossy` - Lossy GIF compression

### FFmpeg Commands

```bash
# Convert video to GIF (high quality)
ffmpeg -i input.mp4 -vf "fps=15,scale=1280:-1:flags=lanczos" -c:v gif output.gif

# Optimize GIF
gifsicle -O3 --lossy=80 -o output-optimized.gif output.gif
```

## Video Tutorial Guidelines

### Technical Requirements

- **Resolution**: 1920x1080 (1080p) minimum
- **Frame Rate**: 30 FPS
- **Format**: MP4 (H.264 codec) for compatibility
- **Audio**: Clear narration, no background noise
- **Duration**: 
  - Quick start: 5 minutes
  - Integration guides: 10-15 minutes
  - Advanced: 20 minutes

### Content Guidelines

- **Script**: Write script before recording
- **Clear Audio**: Use good microphone, reduce background noise
- **Screen Recording**: Use high DPI, clear fonts
- **Annotations**: Add text overlays for key points
- **Transitions**: Smooth transitions between sections
- **Thumbnails**: Create attractive thumbnails for YouTube/GitHub

### Tools for Video Creation

- **Recording**:
  - OBS Studio (free, cross-platform)
  - ScreenFlow (macOS, paid)
  - Camtasia (Windows/Mac, paid)
- **Editing**:
  - DaVinci Resolve (free, professional)
  - Adobe Premiere Pro (paid)
  - Final Cut Pro (macOS, paid)
- **Audio**:
  - Audacity (free, audio editing)
  - Good microphone (Blue Yeti, Audio-Technica)

## Usage in Documentation

### README.md

Add screenshots/GIFs to README:

```markdown
## 🖼️ Screenshots

### Admin Panel
![Dashboard](docs/images/screenshots/04-dashboard-with-projects.png)
![Flags Management](docs/images/screenshots/07-flags-screen-with-flags.png)

### Quick Demo
![Create Flag](docs/images/gifs/01-create-flag.gif)
```

### Landing Page

Add visual materials to landing page HTML.

### Documentation

Reference screenshots in markdown documentation files.

## Status

**Current Status**: 📋 Planning phase - directories created, checklists prepared

**Next Steps**:
1. Record screenshots of admin UI
2. Create GIF demonstrations
3. Record video tutorials
4. Optimize all files
5. Add to README and documentation

## Notes

- All visual materials should use test/demo data (no real user data)
- Keep file sizes optimized for web
- Use consistent naming convention
- Update this README as materials are created

