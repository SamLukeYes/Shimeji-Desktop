{
  lib,
  jdk11,
  makeDesktopItem,
  maven,
  xorg,
}:

maven.buildMavenPackage rec {
  pname = "shimeji-desktop";
  version = "dev";

  src = ./.;

  # manualMvnArtifacts = [
  #   "com.akathist.maven.plugins.launch4j:launch4j-maven-plugin:2.5.1"
  #   "org.apache.maven.plugins:maven-surefire-plugin:3.2.5"
  #   "net.sf.launch4j:launch4j:jar:workdir-linux64:3.50"
  # ];

  mvnHash = "sha256-y71i4RzobDW+AlwwDhotj8SOQDHAcfM/e5oVJH+cwa4=";

  # buildOffline = true;

  # https://stackoverflow.com/questions/32299902/parallel-downloads-of-maven-artifacts
  # mvnParameters = "-Daether.dependencyCollector.impl=bf -Dmaven.artifact.threads=10 -Dmaven.mirror.url=${mavenMirrorUrl}";
  mvnParameters = "-DskipLaunch4j -Dmaven.antrun.skip=true";

  nativeBuildInputs = [ jdk11 ];

  desktopItem = makeDesktopItem {
    desktopName = "Shimeji Desktop";
    name = pname;
    exec = pname;
    icon = pname;
    comment = meta.description;
    startupWMClass = "com-group_finity-mascot-Main";
  };

  installPhase = ''
    export java=${lib.getExe jdk11}
    export runtimeLibs=${lib.makeLibraryPath [ xorg.libX11 xorg.libXrender ]}
    mkdir -p $out/bin $out/share/{pixmaps,$pname}
    cp -r conf/ img/ target/{lib,Shimeji-ee.jar} $out/share/$pname
    mv $out/share/$pname/img/profile.png $out/share/pixmaps/$pname.png
    ln -s $desktopItem/share/applications $out/share/
    substituteAll ${./run.sh.in} $out/bin/shimeji-desktop
    chmod +x $out/bin/shimeji-desktop
  '';

  meta = {
    description = "A fork of Shimeji-ee with newer Java version and better cross-platform support";
    homepage = "https://github.com/SamLukeYes/Shimeji-Desktop";
    license = with lib.licenses; [ bsd3 mit lgpl21Only ];
  };
}