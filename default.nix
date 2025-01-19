{
  lib,
  jdk,
  makeDesktopItem,
  maven,
  xorg,
}:

maven.buildMavenPackage rec {
  pname = "shimeji-desktop";
  version = "dev";

  src = ./.;

  mvnHash = "sha256-y71i4RzobDW+AlwwDhotj8SOQDHAcfM/e5oVJH+cwa4=";

  mvnParameters = "-DskipLaunch4j -Dmaven.antrun.skip=true";

  nativeBuildInputs = [ jdk ];

  desktopItem = makeDesktopItem {
    desktopName = "Shimeji Desktop";
    name = pname;
    exec = pname;
    icon = pname;
    comment = meta.description;
    startupWMClass = "com-group_finity-mascot-Main";
  };

  installPhase = ''
    export java=${lib.getExe jdk}
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
    mainProgram = pname;
  };
}