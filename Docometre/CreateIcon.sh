# Remove icon folder
rm -R DocometreIcon.iconset
# Create icons folder
mkdir DocometreIcon.iconset
# Convert 256 pixels png to ...
sips -z 16 16     icons/Docometre256.png --out DocometreIcon.iconset/icon_16x16.png
sips -z 32 32     icons/Docometre256.png --out DocometreIcon.iconset/icon_16x16@2x.png
sips -z 32 32     icons/Docometre256.png --out DocometreIcon.iconset/icon_32x32.png
sips -z 48 48     icons/Docometre256.png --out DocometreIcon.iconset/icon_48x48.png
sips -z 64 64     icons/Docometre256.png --out DocometreIcon.iconset/icon_32x32@2x.png
sips -z 128 128   icons/Docometre256.png --out DocometreIcon.iconset/icon_128x128.png
sips -z 256 256   icons/Docometre256.png --out DocometreIcon.iconset/icon_128x128@2x.png
cp icons/Docometre256.png DocometreIcon.iconset/icon_256x256.png
# Creat icns for mac os
iconutil -c icns -o icons/docometre.icns DocometreIcon.iconset
# In order to create ico file : 
# http://andrius.velykis.lt/2012/10/creating-icons-for-eclipse-rcp-launcher/
# http://catalyst.net.nz/news/creating-multi-resolution-favicon-including-transparency-gimp