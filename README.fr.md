# MultiSpaceUI

**Plusieurs fenêtres spatiales à la fois — les écrans de conteneur deviennent des panneaux 3D flottant dans le monde, et plus d'une reste ouverte.**

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-brightgreen)](#prérequis)
[![Loader](https://img.shields.io/badge/Fabric-Loader%20%3E%3D%200.19.3-orange)](#prérequis)
[![Java](https://img.shields.io/badge/Java-25-blue)](#prérequis)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

MultiSpaceUI repose sur deux couches :

| Couche | Origine | Licence | Ce qu'elle possède |
| ------ | ------- | ------- | ------------------ |
| **Cœur Spatial GUI** | copie intégrale de [Spatial-GUI](https://github.com/tastytrash/Spatial-GUI) par tastytrash | **MIT**, voir [`LICENSE.spatial-gui.txt`](LICENSE.spatial-gui.txt) | la fenêtre **active** : placement du panneau, caméra, capture hors écran, correspondance souris, écran de configuration |
| **Couche fenêtres** | code original écrit pour ce dépôt | **MIT**, voir [`LICENSE`](LICENSE) | toutes les fenêtres *sauf* l'active : cycle de vie, panneaux d'arrière-plan, focus, visibilité, routage des entrées, configuration propre |

Le panneau actif est donc dessiné **et** mesuré par le cœur, et MultiSpaceUI n'ajoute que ce
qu'une mod de fenêtres apporte : plusieurs fenêtres, priorité de focus, atténuation avec la
distance et routage de clics sûr. Ce partage est ce qui garantit que le pointeur, le panneau
et le clic reposent sur la géométrie exactement identique — la couche ne recalcule jamais un
placement que le cœur possède déjà.

> 🇬🇧 **English version :** [README.md](README.md)

---

## Fonctionnalités

- **Multi-fenêtres** — votre inventaire, un coffre et une table de craft peuvent être ouverts
  en même temps ; chacun a son panneau. Une seule fenêtre est *active* (celle à laquelle le
  jeu envoie les entrées) ; les autres restent ouvertes en arrière-plan et gardent leur
  contenu à jour.
- **Priorité du focus** — viser un panneau le focalise : il retrouve sa taille et son opacité
  pleines, et devient la seule fenêtre qui reçoit les clics. La fenêtre focalisée n'est
  jamais masquée par la règle de distance.
- **Atténuation avec la distance** — les panneaux au-delà de `maxDistance` deviennent
  invisibles et inertes ; entre `minScale`/`minOpacity` et les valeurs focalisées, tout est
  interpolé en douceur (aucun saut).
- **Routage de clics sûr** — un clic qui n'atterrit pas sur le panneau de la fenêtre active
  est **jeté** : un écran de conteneur ne peut donc jamais l'interpréter comme « clic en
  dehors » et se fermer. Les clics ne vont jamais à une fenêtre autre que celle qui est
  focalisée.
- **Écrans vanilla et mods** — liste blanche / liste noire par nom de classe fully qualified,
  plus trois interrupteurs de catégorie (conteneurs, inventaire, autres écrans).
- **Deux modes caméra** — première et troisième personne, choisis dans la configuration.
- **Animations, optionnelles** — ouverture/fermeture et transitions de taille peuvent être
  désactivées (confort visuel).
- **Configuration persistante** — deux fichiers JSON dans `.minecraft/config/`, éditables à
  la main hors jeu et relus au lancement, chaque valeur bornée à un intervalle sain.
- **Pensé pour la performance** — pas de reconstruction de GUI par appel, pas d'OpenGL brut,
  un seul tampon hors écran par fenêtre ouverte.

## Prérequis

| Composant | Version |
| --------- | ------- |
| Minecraft | **26.3** (voir [Compatibilité avec les futures versions](#compatibilité-avec-les-futures-versions)) |
| Java | **25** (bytecode cible 21) |
| Fabric Loader | **>= 0.19.3** (validé avec 0.19.5) |
| Fabric API | un build pour 26.3 (validé avec **0.161.0+26.3**) |
| Cloth Config | **>= 26.3.158** — dépendance **obligatoire** (écran de config du cœur Spatial GUI) |
| Mod Menu | facultatif, seulement pour ouvrir la config du cœur depuis sa liste |

MultiSpaceUI est une mod **côté client** : elle n'a pas besoin d'être installée sur le
serveur.

## Installation

1. Installez [Fabric Loader](https://fabricmc.net/use/) pour Minecraft 26.3.
2. Placez **Fabric API** et **Cloth Config** dans votre dossier `mods` (Mod Menu est
   facultatif).
3. Déposez `multispaceui-mc26.3-v<version>.jar` dans `mods`.
4. Lancez le jeu. La mod est activée par défaut.

Le nom du JAR contient toujours la version de Minecraft **et** celle de la mod : plusieurs
builds peuvent coexister dans le même dossier sans s'écraser.

## Contrôles

Les touches de MultiSpaceUI sont **non assignées** par défaut — assignez-les dans
`Options → Contrôles → MultiSpaceUI` :

| Action | Touche par défaut | Description |
| ------ | ----------------- | ----------- |
| Basculer MultiSpaceUI | *non assignée* | Active/désactive les fenêtres spatiales ; une fois désactivées, toutes se replient et les écrans plein écran vanilla sont utilisés. Le choix est persisté. |
| Fenêtre suivante | *non assignée* | Change de fenêtre active. |
| Fermer la fenêtre active | *non assignée* | Ferme uniquement la fenêtre active ; son menu est fermé côté serveur, les autres restent ouvertes. |

Le cœur Spatial GUI enregistre une touche à lui, dans sa propre catégorie :

| Action | Touche par défaut | Description |
| ------ | ----------------- | ----------- |
| Ouvrir la config Spatial GUI | *non assignée* | Ouvre l'écran de configuration du cœur (aussi accessible via Mod Menu). |

Souris, avec une fenêtre ouverte :

- **Viser un panneau et cliquer** → cette fenêtre est focalisée et reçoit le clic.
- **Cliquer en dehors de tous les panneaux** → le clic est jeté, rien ne se passe
  (`closeOnClickOutside: false`).
- **Molette sur un panneau** → routée vers l'écran ; **molette en dehors** → jetée, comme les
  clics.
- **Le glisser-déposer d'un bord de panneau pour le redimensionner n'est pas encore
  implémenté** (voir [Limitations connues](#limitations-connues)).

## Configuration

Il y a **deux** fichiers de configuration, un par couche — tous deux créés au premier
lancement, relus au démarrage, et chaque valeur bornée à un intervalle sain pour qu'un
fichier édité à la main ne casse pas le rendu.

| Fichier | Couche | Ce qu'il change |
| ------- | ------ | --------------- |
| `.minecraft/config/multispaceui.json` | MultiSpaceUI | fenêtres, focus, distances, routage des clics, listes d'écrans |
| `.minecraft/config/spatial-gui.json` | Cœur Spatial GUI | **comment et où le panneau actif est dessiné** : mode caméra, distance, décalages, échelle, FOV, HUD, flou, catégories d'écrans du cœur |

Tous les seuils sont des valeurs *initiales* : le contrôleur de visibilité lisse chaque
modification, aucune valeur ne produit donc de saut à l'écran.

### `multispaceui.json` — global

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `enabled` | `true` | Interrupteur général (identique à la touche de bascule). |
| `perspective` | `"FIRST_PERSON"` | `"FIRST_PERSON"` ou `"THIRD_PERSON"` : inclinaison utilisée par les ancrages des fenêtres d'*arrière-plan*. Le panneau actif suit le mode caméra du cœur. |
| `multiWindowEnabled` | `true` | Autoriser plusieurs écrans ouverts à la fois. |
| `animationsEnabled` | `true` | Animations d'ouverture/fermeture et de transition. |

### Placement du panneau (unités monde / degrés)

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `panelDistance` | `1.6` | Distance de référence d'une fenêtre, en blocs : jusqu'où elle peut dériver du joueur avant de commencer à s'estomper. |
| `panelsFollowPlayer` | `true` | `false` = les fenêtres restent ancrées là où elles se sont ouvertes ; l'atténuation suit alors la distance parcourue par le joueur. |
| `windowSpacingDegrees` | `18` | Espacement angulaire entre deux fenêtres consécutives de l'éventail. |
| `backgroundPushBack` | `0.35` | Profondeur supplémentaire ajoutée à toute fenêtre qui n'est pas active. |

> La **taille, la distance et l'angle du panneau actif ne sont pas ici** : ils appartiennent
> au cœur (`spatial-gui.json`, clés `screenDistance`, `screenScale`, `screenYawOffset`, …).

### Taille et opacité

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `baseScale` | `1.0` | Échelle de la fenêtre focalisée. |
| `minScale` / `maxScale` | `0.35` / `1.15` | Bornes de l'interpolation d'échelle. |
| `minOpacity` / `maxOpacity` | `0.12` / `1.0` | Bornes de l'interpolation d'opacité. |

### Système de priorité

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `maxDistance` | `6.0` | Au-delà, une fenêtre est invisible et inerte — sauf celle que l'on regarde : le focus l'emporte sur la distance. |
| `focusScaleIntensity` | `0.75` | Combien une fenêtre non focalisée rétrécit (`0` = pas du tout, `1` = jusqu'à `minScale`). |
| `focusFadeIntensity` | `0.85` | Combien une fenêtre non focalisée s'estompe. |
| `transitionSpeed` | `9.0` | Vitesse d'interpolation ; plus c'est élevé, plus c'est réactif. |

### Interaction et rendu

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `crosshairTargeting` | `true` | Le focus suit le viseur plutôt que le pointeur. |
| `closeOnClickOutside` | `false` | Laisser à `false` garantit qu'un clic en dehors ne ferme jamais un conteneur. |
| `renderScalePercent` | `100` | Résolution du tampon hors écran d'une fenêtre d'**arrière-plan**, en % de sa taille (25–100 ; plus bas = plus rapide). |
| `linearFiltering` | `true` | Filtrage des textures des panneaux d'arrière-plan. |

### Compatibilité des écrans

| Clé | Défaut | Signification |
| --- | ------ | ------------- |
| `spatialContainers` | `true` | Coffres, fours, craft… deviennent des fenêtres. |
| `spatialInventory` | `true` | L'inventaire du joueur devient une fenêtre. |
| `spatialOtherScreens` | `false` | Tout le reste (écrans de mods) seulement s'il est listé. |
| `includedScreens` | `[]` | Force ces écrans dans une fenêtre même si leur catégorie est désactivée. |
| `excludedScreens` | `[]` | Ne transforme jamais ces écrans en fenêtre. |

Les écrans sont identifiés par leur **nom de classe fully qualified**, par ex.
`net.minecraft.client.gui.screens.inventory.ChestScreen`.
`excludedScreens` a toujours la priorité.

### Quel écran devient vraiment une fenêtre

Un écran ne devient une fenêtre spatiale que si les **deux** couches l'acceptent :

```
fenêtre spatiale = SpatialGUIClient.shouldHookScreen(screen)   // catégories + listes du cœur
                ET ScreenCompatibility.isWindowable(screen)    // le tableau ci-dessus
```

Un écran désactivé dans `spatial-gui.json` reste donc un écran plein écran normal, quelle que
soit la valeur dans `multispaceui.json`, et inversement. En cas de doute, activez l'écran
dans les **deux** fichiers.

### `spatial-gui.json` — les clés qui comptent ici

| Clé | Signification |
| --- | ------------- |
| `screenDistance`, `screenSideOffset`, `screenHeightOffset`, `screenYawOffset`, `screenPitchOffset`, `screenScale` | Placement du panneau actif en **troisième personne**. |
| `firstPersonScreen*` (mêmes suffixes) | Idem en **première personne**. |
| `guiScale`, `autoCalculateGuiScale`, `autoScaleByFov`, `renderScalePercent` | Résolution et taille du panneau actif. |
| `inventory`, `containers`, `pauseScreen`, `allScreens`, `enabledScreens`, `disabledScreens` | Catégories et listes d'écrans côté cœur (combinées aux nôtres, cf. ci-dessus). |
| `firstPersonModeInventory`, `firstPersonModeContainers`, `autoDetectCameraMode`, `useCrosshairForFirstPerson` | Quel mode caméra le cœur utilise ; `autoDetectCameraMode` le laisse choisir. |
| `hudOptions.hideHotbar`, `hudOptions.hideHud`, `blurWorldBackground`, `worldBlurStrength`, `filtering.*`, `screenAlpha` | HUD, flou et filtrage pendant l'ouverture d'un écran. |

## Architecture (comment ça marche)

Une frame, trois points de capture — aucun écran n'est extrait deux fois :

1. `LevelRenderer.render` (TAIL) — capture la projection, la rotation de vue et la position de
   caméra de la frame : panneaux et monde ne peuvent donc pas diverger.
2. `Gui.extractRenderState` — l'écran de la fenêtre active est **ignoré** : le cœur l'extrait
   dans son propre tampon hors écran, pour que panneau et HUD ne dessinent jamais les mêmes
   widgets deux fois.
3. `GameRenderer.render` (TAIL) — `SpatialWindowRenderer` capture chaque fenêtre
   **d'arrière-plan** dans son propre `TextureTarget`, puis dessine les panneaux en quad
   d'espace monde sous la projection de la frame, en restaurant la précédente dans un
   `finally`.

Répartition des responsabilités :

| Classe | Couche | Rôle |
| ------ | ------ | ---- |
| `InventoryRenderer`, `RenderUtil`, `AnimationUtil` | cœur | calcule et dessine le panneau actif, son animation et sa correspondance souris. |
| `SpatialGUIConfig`, `SpatialGUIKeybinds`, `SpatialGUIModMenu` | cœur | config du cœur (cloth-config), sa touche et son entrée Mod Menu. |
| `WindowManager` | MultiSpaceUI | Cycle de vie : ouvrir, remplacer, activer, fermer, persistance en arrière-plan. |
| `WindowCapture` | MultiSpaceUI | Un `GuiRenderState` + `GuiRenderer` + `TextureTarget` par fenêtre d'arrière-plan. |
| `SpatialWindowRenderer` | MultiSpaceUI | Capture de frame, placement des panneaux d'arrière-plan, dessin, et placement de la fenêtre active **emprunté au quad du cœur**. |
| `WindowTargeting` | MultiSpaceUI | Test rayon viseur/pointeur → panneau ; c'est le panneau le **plus proche** qui gagne, car c'est celui qui est dessiné au-dessus. |
| `WindowVisibilityController` | MultiSpaceUI | `focusWeight`, `distanceWeight`, `targetScale`, `targetOpacity`, `interactionEnabled` + lissage temporel. |
| `SpatialInput` | MultiSpaceUI | Projette le pointeur sur la fenêtre focalisée, focalise la fenêtre cliquée et **jette** les clics et molettes qui ratent tous les panneaux. |
| `MultiSpaceConfig` | MultiSpaceUI | Configuration client persistante (`multispaceui.json`). |
| `ScreenCompatibility` | MultiSpaceUI | Évaluation liste blanche / liste noire, combinée au filtre du cœur. |

Règle de placement :

- **Panneau actif** — son quad est lu directement dans le cœur (`getQuadBasis()`) puis tourné
  dans l'espace de vue de la frame. Rien n'est recalculé : centre, échelle animée et ancrage
  caméra sont, par construction, exactement ce qui est dessiné.
- **Panels d'arrière-plan** — même disposition en éventail, un panneau par fenêtre, repoussée
  de `backgroundPushBack`, mise à l'échelle avec l'échelle du quad du cœur pour qu'une fenêtre
  d'arrière-plan et la fenêtre active ne divergent jamais sur la taille.

## Compilation et tests

**JDK 25** requis (`JAVA_HOME` doit y pointer).

```bash
./gradlew build              # tests unitaires + JAR, puis copie dans copy-jar-here/
./gradlew runClient          # lance un client de développement
./gradlew test               # 24 tests JUnit : géométrie, visibilité, configuration
./gradlew runClientGameTest  # 47 contrôles de bout en bout dans un client réel
```

Le JAR livré est `copy-jar-here/multispaceui-mc<version-mc>-v<version-mod>.jar`
(`copy-jar-here/` est un dossier local, ignoré par git : les binaires sont publiés en GitHub
Releases). Rebuilder la même version met à jour son propre JAR et n'écrase jamais un autre.

Le test de jeu pilote de vraies entrées (pointeur, boutons, molette, touches) dans un monde
vivant : il ouvre l'inventaire, vise un emplacement, clique dessus, clique en dehors de tous
les panneaux, ouvre un coffre comme seconde fenêtre, s'éloigne de 14 blocs des deux panneaux
et finit par assigner la touche de bascule. Chaque contrôle est loggé
`[gametest] PASS ...` (ou `FAIL ...`), la session se termine par un seul `AssertionError`
listant les échecs, et les captures d'écran atterrissent dans
`build/run/clientGameTest/screenshots/`.

### Tests exécutés

| Commande | Résultat | Moment |
| -------- | -------- | ------ |
| `./gradlew test` | **24/24 tests unitaires, BUILD SUCCESSFUL** | après l'édition de configuration, run final |
| `./gradlew runClientGameTest` | **47/47 contrôles passés, BUILD SUCCESSFUL**, 5 captures d'écran | après l'édition de configuration, run final |
| `./gradlew build` | **BUILD SUCCESSFUL**, JAR écrit dans `copy-jar-here/` | après les deux précédents |

## Matrice des tests

« Le build passe » ne suffit pas : la mod doit être **jouable**, avec des interfaces 3D
interactives. **Dernier run : 47/47 contrôles de jeu et 24/24 tests unitaires, tous verts.**

| # | Test | Résultat | Vérifié par |
| - | ---- | -------- | ----------- |
| 1 | `gradlew build` réussit seul | ✅ OK | `BUILD SUCCESSFUL` |
| 2 | JAR livré dans `copy-jar-here/` avec les versions MC + mod dans le nom | ✅ OK | `multispaceui-mc26.3-v0.1.0.jar` |
| 3 | Le client démarre avec les deux ensembles de mixins, aucune erreur Mixin | ✅ OK | le test de jeu pilote un vrai client pendant toute la session |
| 4 | Le HUD s'affiche normalement avec la mod activée | ✅ OK | capture `0000_multispaceui-01-inventory-window.png` |
| 5 | L'écran de conteneur apparaît en panneau 3D, aucun fond sombre résiduel | ✅ OK | `the panel is placed in view space`, `the panel content is extracted into its own texture` |
| 6 | Contenu non miroir / orientation UV correcte | ✅ OK | `the pointer maps onto the slot inside the panel (… / slot 141,187)` + capture |
| 7 | La barre d'action vanilla reste dessinée avec une fenêtre ouverte | ✅ OK | capture `0000_…` (barre d'action, vie et XP toujours dessinés) |
| 8 | Un clic dans un panneau atteint le bon emplacement (drag d'objet) | ✅ OK | `a click inside the panel is routed to the screen`, `clicking the slot picks the stack up` |
| 9 | Un clic en dehors d'un panneau ne ferme jamais le conteneur | ✅ OK | `a click far outside every panel is dropped`, `the very same container screen survives a click outside the panel`, `the click outside neither picked up nor dropped anything` |
| 10 | Molette dans un panneau routée, molette en dehors jetée | ✅ OK | `scroll inside the panel is routed to the screen`, `scroll far outside every panel is dropped as well` |
| 11 | Regarder une seconde fenêtre la focalise et la met à l'échelle | ✅ OK | `the crosshair focuses the new window`, `the background window loses the focus`, `background window is smaller than the focused one (0.35 < 1.0)` |
| 12 | Deux fenêtres ouvertes, la fenêtre d'arrière-plan persiste | ✅ OK | `using the chest opens a second window`, `the background window is still visible`, `clicks are never routed to a window that is not focused`, capture `0003_multispaceui-04-two-windows.png` |
| 13 | Le panneau rétrécit et s'estompe avec la distance, disparaît au-delà de `maxDistance` | ✅ OK | `past maxDistance is gone / fades out / shrinks to its minimum` (joueur téléporté à 14 blocs) |
| 14 | La fenêtre focalisée n'est jamais masquée par la règle de distance | ✅ OK | `the window being looked at is never hidden by the distance rule`, `and it keeps a readable opacity` + test unitaire `focusedWindowIsNeverHiddenByTheDistanceRule` |
| 15 | La touche de bascule désactive la mod et replie toutes les fenêtres | ✅ OK | `the toggle keybind switches the mod off`, `switching the mod off folds every window`, `the switch is persisted in multispaceui.json and read back` |
| 16 | Une édition de configuration est appliquée et persistée | ✅ OK | `a configuration edit is written to disk and read back (2.5)` + 7 tests unitaires sur `MultiSpaceConfig` |

Note sur le test n°10 : `mouseScrolled` passe par le même prédicat de rejet que
`mouseClicked`, et le test de jeu explore les deux côtés (sur un panneau → l'écran reçoit le
défilement, en dehors de tous → jeté). Aucun écran de conteneur vanilla ne fait défiler son
propre contenu : seul le routage est observable.

Note sur les tests n°6 et n°8 : le cœur fait glisser sa caméra avec le pointeur (son effet de
parallaxe), le panneau continue donc de glisser un peu après chaque mouvement. Le test poursuit
l'emplacement jusqu'à ce qu'il arrête de bouger sous le pointeur — le même point fixe sur lequel
se pose la main d'un joueur — et lit la recherche et la correspondance du pointeur dans une
seule et même frame.

## Compatibilité avec les futures versions

Minecraft 26.3 est livré **non obfusqué** : le build n'utilise donc aucun mapping, le plugin
Fabric Loom ne remappe ni Minecraft ni les mods. Cible vanilla référencée par signature
explicite (`@At(target = ...)`) plutôt que par nom obfusqué, et les points d'appel propres à la
version vivent dans les paquets `mixin`. Porter vers une version ultérieure consiste donc à
revérifier ces signatures (avec `javap` ou un décompilateur) plutôt qu'à remapper toute la mod.

## Limitations connues

- La **barre des tâches** des fenêtres (liste des fenêtres ouvertes) et l'**écran de
  paramètres en jeu** ne sont **pas implémentés** — éditez `multispaceui.json` (ou la config
  du cœur) pour l'instant.
- **Le glisser-déposer d'un panneau pour le déplacer ou le redimensionner n'est pas
  implémenté** ; les clés `dragSensitivity` / `resizeEnabled` des anciennes versions ont été
  supprimées plutôt que laissées inopérantes.
- Les **positions des fenêtres ne sont pas persistées** d'une session à l'autre.
- Les touches sont non assignées par défaut et doivent l'être une fois dans
  `Options → Contrôles`.
- Le multi-fenêtres suppose que les menus de *conteneur* restent ouverts côté serveur ;
  fermer une fenêtre envoie son propre `ServerboundContainerClosePacket` : un serveur vanilla
  peut donc fermer le menu partagé quand deux écrans de conteneur sont ouverts — à tester sur
  les serveurs 26.3.
- Seul `en_us` est traduit pour les chaînes propres à MultiSpaceUI ; le cœur fournit `en_us`,
  `pt_br` et `ru_ru`.
- Le cœur Spatial GUI conserve sa propre liste d'anomalies amont dans
  `src/main/java/org/tastytrash/spatialGUI/TODO` (notifications dessinées sur la GUI,
  rebond de la main réinitialisé à l'entrée/sortie d'un écran, avertissements de compatibilité
  EMI). Elles sont héritées telles quelles.
- Testé sur une seule machine (OpenGL, NVIDIA). La compatibilité Sodium/Iris est prévue mais
  pas encore vérifiée sur une installation Sodium.

## Crédits et licences

- **MultiSpaceUI** — la couche fenêtres, écrite pour ce dépôt, [MIT](LICENSE).
- [**Spatial-GUI**](https://github.com/tastytrash/Spatial-GUI) par **tastytrash** — **MIT**.
  Son code source et ses ressources sont **inclus dans ce dépôt**
  (`src/main/java/org/tastytrash/`, `src/main/resources/assets/spatial-gui/`,
  `spatial-gui.mixins.json`) comme cœur de rendu de cette mod. Le texte de licence amont est
  conservé verbatim dans [`LICENSE.spatial-gui.txt`](LICENSE.spatial-gui.txt), et le JAR
  l'emporte aussi. Les modifications se limitent à ce dont MultiSpaceUI avait besoin
  (décoration 26.3, crochets pour la couche fenêtres).
- [**Windowed GUIs**](https://github.com/Antikythera-Studios/Windowed-GUIs) par guivnf —
  **propriétaire, tous droits réservés** (aucune copie, modification ni redistribution).
  Consultée pour **l'analyse fonctionnelle uniquement** (quels comportements une mod de
  fenêtres doit fournir). **Aucun code, aucun asset, aucune texture n'a été copié**, et ni les
  sources ni les binaires ne sont redistribués dans ce dépôt (la copie locale `exemple/` est
  ignorée par git).
- Minecraft est une marque de Mojang AB. Cette mod n'est pas affiliée à Mojang ou Microsoft.
