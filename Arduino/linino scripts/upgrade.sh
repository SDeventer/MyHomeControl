echo "updating Linino -- DO NOT INTERRUPT OR REMOVE POWER UNTIL FINISHED"
echo ""
echo "--"
wget http://download.linino.org/linino_distro/master/latest/openwrt-ar71xx-generic-linino-yun-16M-250k-squashfs-sysupgrade.bin
echo ""
echo "--"
sysupgrade -v -n openwrt-ar71xx-generic-linino-yun-16M-250k-squashfs-sysupgrade.bin
echo ""
echo "--"
echo "Before installing any additional software run YunDiskSpaceExpander script on the Arduino Yun!!!"
echo ""
echo "--"
echo "Before installing any additional software run YunDiskSpaceExpander script on the Arduino Yun!!!"
echo ""
echo "--"
