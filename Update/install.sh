#!/bin/ksh
# Copyright (c) 2026 Flex97115 (https://github.com/Flex97115)
# This file is part of MH2p_CarPlay_ButtonSkip, licensed under CC BY-NC-SA 4.0.
# https://creativecommons.org/licenses/by-nc-sa/4.0/
# See the LICENSE file in the project root for full license text.
# NOT FOR COMMERCIAL USE

[[ ! -e "/mnt/app" ]] && mount -t qnx6 /dev/mnanda0t177.1 /mnt/app
mount -uw /mnt/app

if [ "$OEM" = "PO" ]; then
    if [[ -e "/mnt/app/eso/hmi/lsd/jars" ]]; then
        if [[ -f "$MOD_PATH/CarPlayButtonSkip.jar" ]]; then
            echo "Modding CarPlay button skip for Porsche $TYPE $RELEASE_TYPE$SOFTWARE_VERSION..."
            cp -Vf "$MOD_PATH/CarPlayButtonSkip.jar" "/mnt/app/eso/hmi/lsd/jars/CarPlayButtonSkip.jar"
            chmod 644 "/mnt/app/eso/hmi/lsd/jars/CarPlayButtonSkip.jar"
            ls -l /mnt/app/eso/hmi/lsd/jars
        else
            echo "error: cannot find $MOD_PATH/CarPlayButtonSkip.jar"
        fi
    else
        echo "error: /mnt/app/eso/hmi/lsd/jars does not exist"
    fi
else
    echo "error: $OEM unsupported (only tested on Porsche)"
fi
