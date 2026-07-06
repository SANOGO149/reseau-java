# Projet de fin de module Java

## Description

Petite application Java créée dans le cadre d'un projet de fin de module. Ce dépôt contient le code source, la configuration et les instructions minimales pour ouvrir, compiler et exécuter le projet.

## Objectifs

- Implémenter les fonctionnalités demandées par le module.
- Fournir un code propre, lisible et réutilisable.

## Prérequis

- Java JDK 11 ou supérieur installé
- Un IDE Java (IntelliJ IDEA, Eclipse) ou les outils en ligne de commande (`javac`, `java`)

## Structure du dépôt

- `Projet de fin de module java.iml` — fichier de projet IntelliJ (si utilisé)
- `README.md` — ce fichier

> Remarque : adaptez le chemin des sources si votre arborescence diffère (ex. `src/`).

## Compiler et exécuter (exemples)

Si le projet utilise une arborescence `src/` classique :

```bash
# compiler tous les .java dans src/ vers le dossier out/
javac -d out $(find src -name '*.java')

# lancer la classe principale (remplacez `com.example.Main` par la vôtre)
java -cp out com.example.Main
```

Ou, ouvrez simplement le projet dans IntelliJ et utilisez les actions "Build" / "Run".

## Tests

Si des tests sont présents, exécutez-les depuis votre IDE ou via l'outil de build utilisé (Maven/Gradle).

## Exemple: Client/Serveur UDP audio

Ce dépôt inclut un petit serveur et client UDP pour transmettre un flux audio (PCM 16 bits) et l'écouter.

1) Compiler les classes:

```bash
javac -d out $(find src -name '*.java')
```

2) Lancer le serveur (écoute sur le port 55555):

```bash
java -cp out com.example.udp.UdpAudioServer 55555
```

3) Lancer le client pour envoyer un ton de test au serveur:

```bash
java -cp out com.example.udp.UdpAudioClient localhost 55555
```

4) Ou envoyer un fichier WAV (PCM) existant:

```bash
java -cp out com.example.udp.UdpAudioClient localhost 55555 chemin/vers/fichier.wav
```

Remarque: le client tente de convertir le WAV en PCM signé 16 bits si nécessaire. Le serveur attend un paquet `FMT:` décrivant le format, puis des paquets de données, puis un paquet `END`.

### Multicast et plusieurs clients

Vous pouvez envoyer vers un groupe multicast en mettant l'adresse multicast comme `host` du client, et en démarrant le serveur avec l'adresse de groupe en second argument :

```bash
# lancer un serveur qui rejoint le groupe multicast 239.0.0.1 port 55555
java -cp out com.example.udp.UdpAudioServer 55555 239.0.0.1

# envoyer vers le groupe
java -cp out com.example.udp.UdpAudioClient 239.0.0.1 55555 chemin/vers/fichier.wav
```

Le serveur joue les paquets reçus depuis n'importe quel client (ou depuis le groupe multicast). Il peut donc recevoir simultanément de plusieurs clients, mais la lecture est séquentielle selon l'arrivée des paquets.

### Scripts helper

- `build.sh` : compile et lance le test automatique.
- `scripts/test.sh` : test end‑to‑end minimal (démarre serveur, envoie un ton test, vérifie le journal).


## Contribution

- Ouvrez une issue pour signaler un bug ou proposer une amélioration.
- Envoyez une pull request claire, avec un descriptif des changements et tests si possible.

## Auteur

- Auteur: (à renseigner)

## Licence

Pas de licence spécifiée — ajoutez un fichier `LICENSE` si vous souhaitez en choisir une.
