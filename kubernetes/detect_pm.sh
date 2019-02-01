#!/usr/bin/env bash

declare -A osInfo;
osInfo[/etc/redhat-release]="yum install "
osInfo[/etc/arch-release]="pacman -Sy "
osInfo[/etc/gentoo-release]="emerge -s "
#osInfo[/etc/SuSE-release]=zypp
osInfo[/etc/debian_version]="apt-get install "

for f in ${!osInfo[@]}
do
    if [[ -f $f ]];then
        echo ${osInfo[$f]}
    fi
done
