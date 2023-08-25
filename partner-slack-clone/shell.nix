{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.leiningen
    pkgs.nodejs_18
    pkgs.git
  ];

  PROJECT_NAME="partner-slack-clone";
}
