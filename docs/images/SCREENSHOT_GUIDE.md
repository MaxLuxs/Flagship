# Screenshot Creation Guide

Step-by-step guide for creating screenshots of the Flagship Admin UI.

## Prerequisites

1. **Admin UI Running**: Start the admin UI application
2. **Test Data**: Create test projects, flags, experiments for screenshots
3. **Clean State**: Use fresh test data (no real user data)
4. **Theme**: Choose consistent theme (light or dark) for all screenshots

## Tools

### macOS
- Built-in screenshot: `Cmd+Shift+4` (area), `Cmd+Shift+3` (full screen)
- Or use: CleanShot X, Skitch

### Windows
- Snipping Tool (built-in)
- Or use: ShareX, Greenshot

### Linux
- Flameshot (recommended)
- Or use: Spectacle, Shutter

### Optimization
- `pngquant` - Lossy compression (smaller files)
- `optipng` - Lossless optimization
- `imagemagick` - Image manipulation

## Step-by-Step Process

### 1. Preparation

```bash
# Create test project with sample data
# - Create project: "Demo Project"
# - Create 5-10 flags (mix of types: bool, string, number, json)
# - Create 2-3 experiments
# - Create 2-3 API keys
# - Add 2-3 project members
```

### 2. Authentication Screenshots

#### Login Screen (`01-login-screen.png`)
1. Navigate to login screen
2. Ensure form is visible (email, password fields)
3. Take screenshot (1920x1080 or higher)
4. Save as `01-login-screen.png`

#### Register Screen (`02-register-screen.png`)
1. Click "Register" or navigate to register
2. Ensure form is visible (name, email, password fields)
3. Take screenshot
4. Save as `02-register-screen.png`

### 3. Dashboard Screenshots

#### Empty Dashboard (`03-dashboard-empty.png`)
1. Log in with new account (no projects)
2. Show empty state message
3. Take screenshot
4. Save as `03-dashboard-empty.png`

#### Dashboard with Projects (`04-dashboard-with-projects.png`)
1. Ensure 2-3 projects exist
2. Show project cards/list
3. Take screenshot
4. Save as `04-dashboard-with-projects.png`

#### Create Project Dialog (`05-create-project-dialog.png`)
1. Click "Create Project" button
2. Dialog should be open with form visible
3. Take screenshot
4. Save as `05-create-project-dialog.png`

### 4. Flags Management Screenshots

#### Empty Flags List (`06-flags-screen-empty.png`)
1. Navigate to Flags tab in project
2. Show empty state (no flags)
3. Take screenshot
4. Save as `06-flags-screen-empty.png`

#### Flags List with Flags (`07-flags-screen-with-flags.png`)
1. Ensure 5-10 flags exist (mix of types)
2. Show flags in table/list
3. Include toggle switches visible
4. Take screenshot
5. Save as `07-flags-screen-with-flags.png`

#### Create Flag Dialog (`08-create-flag-dialog-{type}.png`)
1. Click "Create Flag" button
2. For each type (bool, string, number, json):
   - Select type
   - Fill form with sample data
   - Take screenshot
   - Save as `08-create-flag-dialog-{type}.png`

#### Edit Flag Dialog (`09-edit-flag-dialog.png`)
1. Click on existing flag to edit
2. Show edit dialog with current values
3. Take screenshot
4. Save as `09-edit-flag-dialog.png`

#### Flag Toggle States
- `10-flag-toggle-on.png`: Flag with toggle ON (enabled)
- `11-flag-toggle-off.png`: Flag with toggle OFF (disabled)

### 5. Experiments Screenshots

#### Empty Experiments List (`13-experiments-screen-empty.png`)
1. Navigate to Experiments tab
2. Show empty state
3. Take screenshot
4. Save as `13-experiments-screen-empty.png`

#### Experiments List (`14-experiments-screen-with-experiments.png`)
1. Ensure 2-3 experiments exist
2. Show experiments with variant visualization
3. Take screenshot
4. Save as `14-experiments-screen-with-experiments.png`

#### Create Experiment Dialog (`15-create-experiment-dialog.png`)
1. Click "Create Experiment"
2. Fill form with variants
3. Show variant distribution
4. Take screenshot
5. Save as `15-create-experiment-dialog.png`

### 6. API Keys Screenshots

#### API Keys List (`19-api-keys-screen.png`)
1. Navigate to API Keys tab
2. Show 2-3 API keys in list
3. Take screenshot
4. Save as `19-api-keys-screen.png`

#### Create API Key Dialog (`20-create-api-key-dialog.png`)
1. Click "Create API Key"
2. Show dialog with type selection
3. Take screenshot
4. Save as `20-create-api-key-dialog.png`

#### API Key Created (`21-api-key-created-dialog.png`)
1. After creating key, show key value dialog
2. Include copy button visible
3. Take screenshot
4. Save as `21-api-key-created-dialog.png`

### 7. Providers Analytics Screenshots

#### Providers Screen (`23-providers-screen.png`)
1. Navigate to Providers tab
2. Show multiple providers with health status
3. Take screenshot
4. Save as `23-providers-screen.png`

#### Provider Health States
- `24-provider-healthy.png`: 🟢 Healthy provider
- `25-provider-degraded.png`: 🟡 Degraded provider
- `26-provider-unhealthy.png`: 🔴 Unhealthy provider

#### Provider Details (`27-provider-details-expanded.png`)
1. Click on provider to expand
2. Show metrics (success rate, response time, etc.)
3. Take screenshot
4. Save as `27-provider-details-expanded.png`

### 8. Settings Screenshots

#### Project Settings (`29-project-settings-tab.png`)
1. Navigate to Settings tab
2. Show project settings form
3. Take screenshot
4. Save as `29-project-settings-tab.png`

#### Project Members (`30-project-members-list.png`)
1. Show project members section
2. List 2-3 members with roles
3. Take screenshot
4. Save as `30-project-members-list.png`

#### Profile Settings (`32-profile-settings.png`)
1. Navigate to user profile settings
2. Show profile form
3. Take screenshot
4. Save as `32-profile-settings.png`

## Optimization

After taking screenshots, optimize them:

```bash
# Install tools (macOS)
brew install pngquant optipng imagemagick

# Lossy compression (smaller files, slight quality loss)
pngquant --quality=80-95 --ext .png --force *.png

# Lossless optimization (no quality loss)
optipng -o7 *.png

# Resize if needed (to 1920x1080)
convert input.png -resize 1920x1080 output.png
```

## Quality Checklist

Before finalizing screenshots:

- [ ] Resolution: Minimum 1920x1080
- [ ] Format: PNG (optimized)
- [ ] File size: Reasonable (< 1MB per screenshot)
- [ ] Theme: Consistent (all light or all dark)
- [ ] Data: Test/demo data only (no real user data)
- [ ] Clean UI: No sensitive information
- [ ] Naming: Follow naming convention (numbers + description)
- [ ] Content: Shows key features clearly

## Common Issues

### Blurry Screenshots
- Use high DPI display
- Take screenshots at native resolution
- Avoid scaling

### Large File Sizes
- Use pngquant for compression
- Remove unnecessary details
- Crop to relevant area

### Inconsistent Appearance
- Use same theme (light/dark)
- Use same browser/application state
- Consistent window size

## Next Steps

After creating screenshots:
1. Review all screenshots for quality
2. Optimize file sizes
3. Add to README.md
4. Update documentation with screenshots
5. Create GIF demonstrations (see `gifs/CHECKLIST.md`)

