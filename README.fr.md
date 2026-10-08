# MultiSpaceUI

**Des fenêtres spatiales flottantes pour Minecraft — les écrans de conteneur deviennent des panneaux 3D dans le monde.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)](#prérequis)
[![Loader](https://img.shields.io/badge/Fabric-Loader%20%3E%3D%200.19.3-orange)](#prérequis)
[![Java](https://img.shields.io/badge/Java-25-blue)](#prérequis)
[![Licence](https://img.shields.io/badge/licence-MIT-blue)](LICENSE)

MultiSpaceUI fusionne deux idées en un seul mod : les **GUI en fenêtres** (plusieurs écrans
ouverts en même temps, déplaçables et redimensionnables) et les **GUI spatiales** (interfaces
rendues comme des panneaux flottant dans le monde au lieu de masquer l'écran).

Le panneau que vous regardez reste grand et entièrement lisible ; les autres rétrissent,
deviennent transparents puis disparaissent avec la distance. Un seul modèle de priorité
pilote tout :

> **focus** (réticule) > **distance** > **orientation** → `targetScale`, `targetOpacity`,
> `interactionEnabled`, le tout lissé dans le temps.

> 🇬🇧 **English version:** [README.md](README.md)

---

## Fonctionnalités

- **Multi-fenêtres** — un coffre, votre inventaire et une table de craft peuvent être ouverts
  en même temps ; chacun reçoit son propre panneau. Une seule fenêtre est « active » (celle à
  laquelle le jeu envoie les entrées), les autres restent vivantes en arrière-plan.
- **Priorité du focus** — viser un panneau le focusse : il repasse à l'échelle et à l'opacité
  pleines, et devient la seule fenêtre à recevoir les clics. La fenêtre ciblée n'est jamais
  masquée par la règle de distance.
- **Fondu distance** — les panneaux au-delà de `maxDistance` deviennent invisibles et inertes ;
  entre `minScale`/`minOpacity` et les valeurs focalisées, tout s'interpole en douceur (aucun
  saut).
- **Routage de clics sûr** — un clic qui n'atterrit pas sur le panneau de la fenêtre active
  est **jeté** : un écran de conteneur ne peut donc jamais l'interpréter comme « clic en
  dehors » et se fermer. Un clic n'est jamais routed vers une autre fenêtre que celle que
  vous avez cliquée.
- **Écrans vanilla et mods** — liste blanche / liste noire par nom de classe complètement
  qualifié, plus des interrupteurs globaux pour conteneurs, inventaire et « autres » écrans.
- **Deux modes caméra** — première et troisième personne, choisis dans la config.
- **Animations, désactivables** — ouverture/fermeture et transitions d'échelle peuvent être
  coupées intégralement (ami des mal des transports).
- **Config persistante** — un simple fichier JSON dans `.minecraft/config/`, éditable à la
  main hors jeu et rechargé au lancement.
- **Pensé pour les performances** — pas de reconstruction d'interface par frame, pas d'OpenGL
  brut, tampons hors écran uniquement pour les fenêtres ouvertes ; conçu pour rester compatible
  Sodium/Iris.

---

## Prérequis

| Composant       | Version                          |
| --------------- | -------------------------------- |
| Minecraft       | **26.3** (versions suivantes : voir [Compatibilité avec les futures versions](#compatibilité-avec-les-futures-versions)) |
| Java            | **25** (bytecode cible 21)       |
| Fabric Loader   | >= 0.19.3                        |
| Fabric API      | n'importe quel build pour 26.3   |

MultiSpaceUI est un mod **côté client** : il n'a pas besoin d'être installé sur le serveur.

## Installation

1. Installer [Fabric Loader](https://fabricmc.net/use/) pour Minecraft 26.3.
2. Installer **Fabric API** dans le dossier `mods`.
3. Déposer `multispaceui-mc26.3-v<version>.jar` dans `mods`.
4. Lancer le jeu. Le mod est activé par défaut.

Le nom du JAR contient toujours la version de Minecraft : plusieurs builds peuvent coexister
dans le même dossier.

## Commandes et raccourcis

Toutes les touches démarrent **non assignées** — à configurer dans
`Options → Contrôles → MultiSpaceUI` :

| Action                | Par défaut | Description |
| --------------------- | ---------- | ----------- |
| Basculer MultiSpaceUI | *non assignée* | Active/désactive les fenêtres spatiales ; une fois désactivé, toutes les fenêtres se replient et l'on retrouve les GUI vanilla plein écran. |
| Fenêtre suivante      | *non assignée* | Passe à la fenêtre active suivante. |
| Fermer la fenêtre active | *non assignée* | Ne ferme que la fenêtre active ; son menu est fermé côté serveur, les autres fenêtres restent ouvertes. |

Souris :

- **Regarder un panneau et cliquer** → cette fenêtre est focalisée (et le clic lui est appliqué).
- **Glisser un panneau** → le déplacer (`dragSensitivity`).
- **Glisser le bord d'un panneau** → le redimensionner (désactivable avec `resizeEnabled: false`).
- **Cliquer en dehors de tous les panneaux** → rien ne se passe par défaut
  (`closeOnClickOutside: false`).

## Configuration

Fichier : **`.minecraft/config/multispaceui.json`** — créé au premier lancement, rechargé au
démarrage. Chaque valeur est bornée à une plage raisonnable au chargement : un fichier édité
à la main ne peut pas casser le rendu. Tous les seuils sont des *valeurs de départ* : le
contrôleur de visibilité lisse chaque changement, aucune valeur ne provoque de saut à l'écran.

### Global

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `enabled` | `true` | Interrupteur général (équivalent de la touche de bascule). |
| `perspective` | `"FIRST_PERSON"` | `"FIRST_PERSON"` ou `"THIRD_PERSON"`. |
| `multiWindowEnabled` | `true` | Autorise plusieurs écrans ouverts à la fois. |
| `animationsEnabled` | `true` | Animations d'ouverture/fermeture et de transition. |

### Placement des panneaux (unités monde / degrés)

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `panelDistance` | `1.6` | Distance par rapport à la caméra. |
| `panelWidth` | `1.25` | Largeur du panneau en blocs à pleine échelle ; la hauteur suit le ratio de la fenêtre. |
| `panelSideOffset`, `panelHeightOffset` | `0.0` | Translation de tout le layout. |
| `panelYawOffset`, `panelPitchOffset` | `0.0` | Rotation de tout le layout. |
| `panelsFollowPlayer` | `true` | `false` = les panneaux restent ancrés dans le monde là où ils se sont ouverts. |
| `windowSpacingDegrees` | `18` | Espacement angulaire entre deux fenêtres de l'éventail. |
| `backgroundPushBack` | `0.35` | Profondeur supplémentaire pour les fenêtres non focalisées. |
| `resizeEnabled` | `true` | Glisser les bords pour redimensionner. |

### Taille et opacité

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `baseScale` | `1.0` | Échelle de la fenêtre focalisée. |
| `minScale` / `maxScale` | `0.35` / `1.15` | Bornes de l'interpolation d'échelle. |
| `minOpacity` / `maxOpacity` | `0.12` / `1.0` | Bornes de l'interpolation d'opacité. |

### Système de priorité

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `maxDistance` | `6.0` | Au-delà, une fenêtre est invisible et inerte. |
| `focusScaleIntensity` | `0.75` | À quel point une fenêtre non focalisée rétrécit (`0` = pas du tout, `1` = jusqu'à `minScale`). |
| `focusFadeIntensity` | `0.85` | À quel point elle devient transparente. |
| `crosshairBoost` | `1.0` | Priorité supplémentaire à la fenêtre visée par le réticule. |
| `transitionSpeed` | `9.0` | Vitesse d'interpolation ; plus haut = plus réactif. |

### Interaction

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `crosshairTargeting` | `true` | Le focus suit le réticule. |
| `strictClickRouting` | `true` | Seule la fenêtre focalisée réagit aux clics. |
| `closeOnClickOutside` | `false` | À garder désactivé pour garantir qu'un clic hors panneau ne ferme jamais un conteneur. |
| `dragSensitivity` | `1.0` | Vitesse de glissement des panneaux. |

### Rendu

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `showHud`, `showHotbar`, `showCrosshair` | `true` | Ce qui reste visible pendant qu'une fenêtre est ouverte. |
| `renderScalePercent` | `100` | Résolution du tampon hors écran en % de la taille de la fenêtre (plus bas = plus rapide). |
| `linearFiltering` | `true` | Filtrage de texture des panneaux. |
| `blurBackground`, `worldBlurStrength` | `false` / `0.6` | Flou du monde derrière l'écran. |

### Compatibilité des écrans

| Clé | Défaut | Rôle |
| --- | ------ | ---- |
| `spatialContainers` | `true` | Fourneaux, coffres, craft… deviennent des panneaux. |
| `spatialInventory` | `true` | L'inventaire joueur devient un panneau. |
| `spatialOtherScreens` | `false` | Tout le reste (écrans de mods) seulement s'il est listé en blanc. |
| `includedScreens` | `{}` | Force ces écrans en fenêtre spatiale même si leur catégorie est désactivée. |
| `excludedScreens` | `{}` | Ne transforme jamais ces écrans en fenêtre spatiale. |

Les écrans sont identifiés par leur **nom de classe complètement qualifié**, par ex.
`net.minecraft.client.gui.screens.inventory.ChestScreen`. `excludedScreens` l'emporte toujours.

## Architecture (comment ça marche)

Une frame, trois points de capture — aucun écran n'est extrait deux fois :

1. `LevelRenderer.render` (TAIL) — capture la projection, la rotation de vue et la position
   caméra de la frame.
2. `Gui.extractRenderState` — l'écran windowed est **sauté** : il est extrait par sa propre
   capture hors écran, afin que le panneau et le HUD ne dessinent jamais deux fois les mêmes
   widgets.
3. `GameRenderer.render` (TAIL) — `SpatialWindowRenderer` extrait chaque fenêtre ouverte dans
   son propre `TextureTarget`, puis dessine les panneaux en quad dans l'espace monde avec une
   projection perspective, et restaure la projection précédente dans un bloc `finally`.

| Classe | Rôle |
| ------ | ---- |
| `WindowManager` | Cycle de vie des fenêtres : ouverture, remplacement, activation, fermeture, persistance en arrière-plan. |
| `WindowCapture` | Un `GuiRenderState` + `GuiRenderer` + `TextureTarget` par fenêtre. |
| `WindowTargeting` | Réticule / curseur → test de collision avec un panneau. |
| `WindowVisibilityController` | `focusWeight`, `distanceWeight`, `orientationWeight`, `targetScale`, `targetOpacity`, `interactionEnabled` + lissage temporel. |
| `SpatialInput` | Place le curseur en coordonnées de panneau, focusse la fenêtre cliquée, jette les clics hors panneau. |
| `MultiSpaceConfig` | Config client persistante. |
| `ScreenCompatibility` | Évaluation liste blanche / liste noire. |

## Compilation

**JDK 25** requis (`JAVA_HOME` doit y pointer).

```bash
./gradlew build        # compile puis copie le JAR dans copy-jar-here/
./gradlew runClient    # lance un client de développement
```

Le JAR livré est `copy-jar-here/multispaceui-mc<version-mc>-v<version-mod>.jar`.
Recompiler la même version écrase son propre JAR et n'en touche jamais un autre.

## Matrice de tests

« Le build passe » ne suffit pas : le mod doit être **jouable**, avec des interfaces 3D
interactives.

| # | Test | Statut |
| - | ---- | ------ |
| 1 | `gradlew build` réussit seul | ✅ OK |
| 2 | JAR livré dans `copy-jar-here/` avec version MC + mod dans le nom | ✅ OK |
| 3 | Le client démarre avec le mod, aucune erreur d'application Mixin | ✅ OK |
| 4 | Le HUD s'affiche normalement avec le mod activé | ✅ OK |
| 5 | L'écran de conteneur apparaît en panneau 3D, sans fond assombri résiduel | ✅ OK |
| 6 | Contenu du panneau non miré / orientation UV correcte | ✅ OK (visuel) |
| 7 | La hotbar vanilla reste dessinée quand une fenêtre est ouverte | ✅ OK |
| 8 | Un clic dans un panneau atteint le bon slot (glisser-déposer d'objet) | ⏳ en attente |
| 9 | Un clic hors panneau ne ferme jamais le conteneur | ⏳ en attente |
| 10 | Défilement (molette) dans un panneau | ⏳ en attente |
| 11 | Regarder une 2e fenêtre la focalise et la met à l'échelle | ⏳ en attente |
| 12 | Deux fenêtres ouvertes simultanément, fenêtre de fond persistante | ⏳ en attente |
| 13 | Le panneau rétrécit et s'estompe avec la distance, disparaît au-delà de `maxDistance` | ⏳ en attente |
| 14 | La fenêtre focalisée n'est jamais masquée par la règle de distance | ⏳ en attente |
| 15 | La touche de bascule désactive le mod et replie les fenêtres | ⏳ en attente |
| 16 | Les modifications de config sont appliquées et persistantes au redémarrage | ⏳ en attente |

## Compatibilité avec les futures versions

Minecraft 26.3 est livré **sans obfuscation** : le build n'utilise donc aucun mapping. Chaque
cible vanilla est référencée par une signature explicite (`@At(target = ...)`) plutôt que par
un nom obfusqué, et les points d'appel spécifiques à la version sont isolés dans le package
`mixin`. Porter vers une version ultérieure consiste donc à revérifier ces signatures (avec
`javap` ou un décompilateur) plutôt qu'à re-mapper tout le mod.

## Limites connues

- La **barre des fenêtres** (liste des fenêtres ouvertes) et l'**écran de réglages en jeu**
  sont prévus (les clés de config existent) mais **pas encore implémentés** — à éditer dans
  `multispaceui.json` pour l'instant.
- Les **positions des fenêtres ne sont pas persistantes** d'une session à l'autre.
- Les touches sont non assignées par défaut et doivent être configurées une fois dans
  `Options → Contrôles`.
- Le multi-fenêtres suppose que les menus de conteneur restent ouverts côté serveur ; fermer
  une fenêtre envoie son propre `ServerboundContainerClosePacket`, aussi un serveur vanilla
  peut fermer le menu partagé quand deux écrans de conteneur sont ouverts — à retester sur un
  serveur 26.3.
- Testé sur une seule machine (OpenGL, NVIDIA). La compatibilité Sodium/Iris est prévue mais
  pas encore vérifiée avec une installation Sodium.

## Crédits et licences

- **MultiSpaceUI** — implémentation originale de ce dépôt, [MIT](LICENSE).
- [**Spatial-GUI**](https://github.com/tastytrash/Spatial-GUI) par tastytrash — **MIT**.
  Étudié comme *carte de référence d'API* pour Minecraft 26.3 (câblage de `GuiRenderer`,
  extraction d'écran, cibles hors écran). **Aucun code copié** : MultiSpaceUI possède sa
  propre architecture, son propre modèle de priorité et son propre routage d'entrées.
- [**Windowed GUIs**](https://github.com/Antikythera-Studios/Windowed-GUIs) par guivnf —
  **propriétaire, tous droits réservés** (copie, modification et redistribution interdites).
  Consulté pour une **analyse fonctionnelle uniquement** (quels comportements doit fournir un
  mod de GUI en fenêtres). **Aucun code, aucun asset, aucune texture n'a été copié**, et ni
  les sources ni les binaires ne sont redistribués dans ce dépôt.

Minecraft est une marque déposée de Mojang AB. Ce mod n'est ni affilié à Mojang ni à Microsoft.
