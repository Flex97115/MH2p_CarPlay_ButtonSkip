# MH2p CarPlay Button Skip
Next / previous track in Apple CarPlay (and Android Auto) with the steering-wheel **programmable button**:
 - **1 press** = next track
 - **2 quick presses** (within 0.6 s) = previous track

Radio, USB and other sources keep their stock behaviour.

## Why
On the Porsche Macan 95B.2 the programmable steering-wheel button can be set to "Next track" in the
instrument cluster menu, but it only works with the PCM's own sources (radio, USB...): it does nothing
in CarPlay. This mod makes it work in CarPlay and adds "previous track" on a double press of the same button.

## Tested on
 - Porsche Macan 95B.2 (phase 2 / facelift), model year 2019
 - PCM 5.0 (MH2p), software `MH2p_ER_PO416_P2491`
 - Apple CarPlay (wired)

Other Porsche models/software versions using the same cluster button logic may work but have not been tested.
The install script only runs on Porsche units (`OEM=PO`).

## License
 - This file is part of MH2p_CarPlay_ButtonSkip, licensed under CC BY-NC-SA 4.0.
 - https://creativecommons.org/licenses/by-nc-sa/4.0/
 - See the LICENSE file in the project root for full license text.
 - NOT FOR COMMERCIAL USE

## Installation
This mod is for use with [MH2p SD ModKit](https://github.com/LawPaul/MH2p_SD_ModKit) by LawPaul.

1. Build your ModKit SD card / USB stick by following the guide on the
   [MH2p SD ModKit site](https://lawpaul.github.io/MH2p_SD_ModKit_Site/)
   (FAT32 media, `Data`, `Meta` and `Mods` folders at the root).
2. Download this repository and copy it to the media as `Mods/MH2p_CarPlay_ButtonSkip/`, so you get:
   ```
   Mods/MH2p_CarPlay_ButtonSkip/Update/install.sh
   Mods/MH2p_CarPlay_ButtonSkip/Update/uninstall.sh
   Mods/MH2p_CarPlay_ButtonSkip/Update/CarPlayButtonSkip.jar
   ```
3. Start the car, insert the media and wait for the update to finish. Do not use the PCM during the update
   and remove the media when prompted.
4. In the **instrument cluster** settings, set the programmable steering-wheel button to **Next track**.
5. Play music in CarPlay: 1 press = next track, 2 quick presses = previous track.

Compatible with the other ModKit mods, including [Phone Full Screen](https://github.com/LawPaul/MH2p_CarPlay_FullScreen)
(this mod uses its own jar and does not touch `fc.jar`).

## Uninstall
Create an empty `uninstall.txt` file in `Mods/MH2p_CarPlay_ButtonSkip/` on the ModKit media and insert it
again (see the [MH2p SD ModKit site](https://lawpaul.github.io/MH2p_SD_ModKit_Site/)).
The script removes `CarPlayButtonSkip.jar` from the PCM.

## How it works
 - `CarPlayButtonSkip.jar` is copied to MH2p at `/mnt/app/eso/hmi/lsd/jars`
 - jars in that directory are loaded onto the classpath before `lsd.jxe` which runs the UI
 - classes in `CarPlayButtonSkip.jar` are prioritized over those in `lsd.jxe` and can modify functionality
 - on the Macan, the cluster does not send the button as a "joker key" event: when the button is set to
   "Next track", the cluster sends a BAP audio command (*DedicatedAudioControl* skip) to the audio source in focus.
   Radio and media have a listener for it, CarPlay/Android Auto (terminal mode) have none, so the command is dropped
 - the mod replaces `de.audi.app.terminalmode.hmi.TMSkipOverJokerkey` with a version that registers the missing
   terminal-mode listener on the cluster audio connector and turns 1 press into *next* and 2 presses into *previous*
   (sent to the phone through the stock CarPlay/Android Auto skip buttons)
 - source code: [`src/`](src/de/audi/app/terminalmode/hmi/TMSkipOverJokerkey.java)
   (Java 1.4 syntax, compiled for class file version 48 against the unit's `lsd.jxe`)

## Credits
 - [LawPaul](https://github.com/LawPaul) for the [MH2p SD ModKit](https://github.com/LawPaul/MH2p_SD_ModKit)
   and its mods, which this mod is based on
