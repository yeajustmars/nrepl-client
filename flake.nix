{
  description = "nREPL Client - Clojure + GraalVM Native Image Environment (Local Stable)";

  inputs = {
    # This tells the flake to use your system's existing nixpkgs registry
    # instead of fetching a new snapshot from GitHub.
    nixpkgs.url = "nixpkgs";
  };

  outputs = { self, nixpkgs }:
    let
      supportedSystems = [ "aarch64-darwin" "x86_64-darwin" "x86_64-linux" "aarch64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs supportedSystems;
    in
    {
      devShells = forAllSystems (system:
        let
          pkgs = import nixpkgs { inherit system; };

          # Safely pulling in the frameworks required by older Nixpkgs on macOS
          darwinFrameworks = pkgs.lib.optionals pkgs.stdenv.isDarwin (with pkgs.darwin.apple_sdk.frameworks; [
            CoreServices
            Foundation
            pkgs.libiconv
          ]);
        in
        {
          default = pkgs.mkShell {
            # Everything required to compile and link the application
            buildInputs = [
              pkgs.clojure
              pkgs.graalvmPackages.graalvm-ce # Nix strictly requires this namespace now
              pkgs.zlib
            ] ++ darwinFrameworks;

            # Set environment variables so GraalVM knows where to find itself
            shellHook = ''
              export JAVA_HOME=${pkgs.graalvmPackages.graalvm-ce}
              export GRAALVM_HOME=${pkgs.graalvmPackages.graalvm-ce}

              echo "🚀 Clojure & GraalVM (Local Stable) Native Image environment loaded!"
              echo "Java version: $(java -version 2>&1 | awk 'NR==1{print $0}')"
              echo "Native-image: $(which native-image)"
            '';
          };
        }
      );
    };
}
