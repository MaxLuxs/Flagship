Pod::Spec.new do |spec|
    spec.name                     = 'flagship_provider_firebase'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'https://github.com/maxluxs/Flagship'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Flagship Firebase Provider'
    spec.vendored_frameworks      = 'build/cocoapods/framework/FlagshipProviderFirebase.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target    = '14.0'
    spec.dependency 'FirebaseRemoteConfig', '11.8.0'
                
    if !Dir.exist?('build/cocoapods/framework/FlagshipProviderFirebase.framework') || Dir.empty?('build/cocoapods/framework/FlagshipProviderFirebase.framework')
        raise "

        Kotlin framework 'FlagshipProviderFirebase' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :flagship-provider-firebase:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.xcconfig = {
        'ENABLE_USER_SCRIPT_SANDBOXING' => 'NO',
    }
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':flagship-provider-firebase',
        'PRODUCT_MODULE_NAME' => 'FlagshipProviderFirebase',
    }
                
    spec.script_phases = [
        {
            :name => 'Build flagship_provider_firebase',
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