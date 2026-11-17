#!/bin/ksh
# Copyright (c) 2026 Flex97115 (https://github.com/Flex97115)
# This file is part of MH2p_CarPlay_ButtonSkip, licensed under CC BY-NC-SA 4.0.
# https://creativecommons.org/licenses/by-nc-sa/4.0/
# See the LICENSE file in the project root for full license text.
# NOT FOR COMMERCIAL USE

[[ ! -e "/mnt/app" ]] && mount -t qnx6 /dev/mnanda0t177.1 /mnt/app
echo "Mounting /mnt/app in r/w mode..."
mount -uw /mnt/app
if [[ -e "/mnt/app/eso/hmi/lsd/jars" ]]; then
    echo "Unmodding CarPlay button skip..."
    rm -f "/mnt/app/eso/hmi/lsd/jars/CarPlayButtonSkip.jar"
else
    echo "error /mnt/app/eso/hmi/lsd/jars does not exist"
fi
