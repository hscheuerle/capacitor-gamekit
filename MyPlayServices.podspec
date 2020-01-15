
  Pod::Spec.new do |s|
    s.name = 'MyPlayServices'
    s.version = '0.0.1'
    s.summary = 'my play services'
    s.license = 'MIT'
    s.homepage = 'http://github/my-play-services'
    s.author = 'harry'
    s.source = { :git => 'http://github/my-play-services', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end