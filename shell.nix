{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = [
    pkgs.leiningen
    pkgs.nodejs_18
  ];
}