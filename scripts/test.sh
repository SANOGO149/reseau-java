#!/usr/bin/env bash
#set -euo pipefail

# Test end-to-end minimal: démarre le serveur, envoie un ton via le client, vérifie la réception.

OUT=out
LOG=server_test.log
# choisir un port aléatoire élevé pour éviter les conflits
PORT=$((20000 + RANDOM % 30000))
HOST=localhost

mkdir -p "$OUT"


javac -d "$OUT" $(find src -name '*.java')

# Lancer le serveur en arrière-plan et rediriger la sortie
java -cp "$OUT" com.example.udp.UdpAudioServer $PORT > "$LOG" 2>&1 &
PID=$!

# attendre que le serveur se prépare
sleep 1

# envoyer un ton test
java -cp "$OUT" com.example.udp.UdpAudioClient $HOST $PORT

# attendre un peu pour la fin de la transmission
sleep 2

# vérifier le log pour la fin
if grep -q "Réception: END" "$LOG"; then
  echo "Test OK: fin de transmission reçue"
  rc=0
else
  echo "Test KO: fin de transmission non trouvée dans $LOG"
  tail -n 200 "$LOG"
  rc=2
fi

# nettoyage
kill $PID 2>/dev/null || true
rm -f "$LOG"
exit $rc
