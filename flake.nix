{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
  let
    system = "x86_64-linux";
    pkgs = import nixpkgs {
      inherit system;
      overlays = [
        (final: prev: {
          jdk = final.jdk11;
        })
      ];
    };
    defaultPackage = pkgs.callPackage ./. { };
  in {
    packages.${system} = {
      default = defaultPackage;

      # https://github.com/NixOS/nixpkgs/issues/356340
      fhs4gnome = pkgs.buildFHSEnv {
        inherit (defaultPackage) pname version;
        runScript = pkgs.lib.getExe defaultPackage;
        targetPkgs = ps: with ps; [ gnome-shell ];
        extraInstallCommands = ''
          ln -s ${defaultPackage}/share $out/
        '';
      };
    };

    devShells.${system}.default = import ./shell.nix {
      inherit pkgs;
    };
  };
}