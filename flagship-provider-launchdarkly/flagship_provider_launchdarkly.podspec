Pod::Spec.new do |spec|
    spec.name                     = 'flagship_provider_launchdarkly'
    spec.version                  = '0.1.0'
    spec.homepage                 = 'https://github.com/maxluxs/Flagship'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Flagship LaunchDarkly Provider'
    spec.vendored_frameworks      = 'build/cocoapods/framework/FlagshipProviderLaunchDarkly.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '14.0'
    spec.dependency 'LaunchDarkly', '8.0.0'
                
    if !Dir.exist?('build/cocoapods/framework/FlagshipProviderLaunchDarkly.framework') || Dir.empty?('build/cocoapods/framework/FlagshipProviderLaunchDarkly.framework')
        raise "

        Kotlin framework 'FlagshipProviderLaunchDarkly' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :flagship-provider-launchdarkly:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':flagship-provider-launchdarkly',
        'PRODUCT_MODULE_NAME' => 'FlagshipProviderLaunchDarkly',
    }
                
    spec.script_phases = [
        {
            :name => 'Build flagship_provider_launchdarkly',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end