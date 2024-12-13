{ 
  pkgs? import <nixpkgs> {}
}:

with pkgs;

mkShell {
  LD_LIBRARY_PATH = lib.makeLibraryPath [
    xorg.libX11
    xorg.libXrender
  ];

  packages = [
    jdk11
    maven
  ];
}
