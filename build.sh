#!/usr/bin/env bash
set -euo pipefail

echo "Compilation du projet..."
mkdir -p out
javac -d out $(find src -name '*.java')
echo "Compilation terminée."

echo "Lancement du test automatique..."
./scripts/test.sh

echo "Succès."
