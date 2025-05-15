[![CeCILL-B License](https://img.shields.io/badge/Licence-CeCILL--B-brightgreen)](https://github.com/fbuloup/DOCoMETRe/blob/master/LICENCE)
[![SWH](https://archive.softwareheritage.org/badge/origin/https://github.com/TeamICSTECHNOS/Docometre/)](https://archive.softwareheritage.org/browse/origin/?origin_url=https://github.com/TeamICSTECHNOS/Docometre)

Welcome to DOCoMETRe software !

This software can be useful if you want to acquire signals based on ADwin or Arduino Uno devices. It can also be helpful to visualise and analyse acquired signals (e.g. filtering, points marking or features computation).

If you need more informations on DOCoMETRe, please check this [overview](http://www.ism.univ-amu.fr/buloup/documents/Overview.pdf) !

Need more details on installation procedure ? Follow this [link](http://www.ism.univ-amu.fr/buloup/documents/Installation.pdf).

Videos tutorials ? It's [here](https://www.youtube.com/watch?v=BV_56Ztva1I&list=PLToTNXU9fy6WKUKD1BgPlpn-ONyaHg7vn).

To download DOCoMETRe, please follow the link related to your platform :

* [Linux64](http://www.ism.univ-amu.fr/buloup/documents/DOCoMETRe/DocometreLinux.zip) (tested with Ubuntu 18.04.2 LTS)
* [MacOSX - DMG file](http://www.ism.univ-amu.fr/buloup/documents/DOCoMETRe/DOCoMETRe.dmg)
* [MacOSX](http://www.ism.univ-amu.fr/buloup/documents/DOCoMETRe/DocometreMacOS.zip)
* [Win64](http://www.ism.univ-amu.fr/buloup/documents/DOCoMETRe/DocometreWindows.zip)

<ins>**You are using OSX ?**</ins> You could unfortunately get an error the first time you launch DOCoMETRe.
Please read this page to get a solution : [note for OSX users](https://github.com/TeamICSTECHNOS/DOCoMETRe/wiki/Note-for-OSX-users).

<ins>**Your OS is Linux GTK**</ins>, 3D charts won't work because of this [bug](https://jogamp.org/bugzilla//show_bug.cgi?id=1362). It should be resolved once Jogamp release 2.4.0 will be available.

There is no installer : simply unzip downloaded file and copy/paste resulting folder where you want, but in a folder where you have all rights.

DOCoMETRe stands for "Dispositif d'Observation et de COntrôle du Mouvement En Temps Réel". It's obviously a very bad acronym, but has the advantage to evoke a meter device ! It is composed of a multiplatform software developed in Java using [Eclipse RCP](https://wiki.eclipse.org/Rich_Client_Platform) framework and for now is able to handle these hardwares : 

* [ADwin Gold](https://www.adwin.de/us/produkte/gold.html) and [ADwin Gold II](https://www.adwin.de/us/produkte/goldII.html) 
* [ADwin Pro](https://www.adwin.de/us/produkte/pro.html) and [ADwin Pro II](https://www.adwin.de/us/produkte/proII.html)
* [Arduino UNO R3 and R4](https://store.arduino.cc/arduino-uno-rev3) (with support for [ADS1115](https://www.pjrc.com/store/teensy40.html), ADC 16bits - I2C)

Enhanced Arduino Uno with support for [MCP4725](https://www.microchip.com/en-us/product/MCP4725) (DAC 12 bits - I2C), [Portenta Machine Control](https://store.arduino.cc/products/arduino-portenta-machine-control?selectedStore=eu) or [Teensy 4.0](https://www.pjrc.com/store/teensy40.html) support are planned.

If you want to use DOCoMETRe, you must first download Java 21 or later from [AdoptOpenJDK](https://adoptopenjdk.net), [Azul](https://www.azul.com/downloads/?package=jre), [Amazon](https://docs.aws.amazon.com/corretto/index.html), [Liberica](https://bell-sw.com), [OpenJDK](https://openjdk.java.net) or [Oracle](https://www.oracle.com/fr/java/technologies/downloads/#java17).

You can find direct Oracle JDK 21 links below :

* [Windows 64](http://www.ism.univ-amu.fr/buloup/documents/JAVA/jdk-21_windows-x64_bin.exe)
* [Linux Debian 64](http://www.ism.univ-amu.fr/buloup/documents/JAVA/jdk-21_linux-x64_bin.deb)
* [Linux RPM 64](http://www.ism.univ-amu.fr/buloup/documents/JAVA/jdk-21_linux-x64_bin.rpm)
* [Mac OS](http://www.ism.univ-amu.fr/buloup/documents/JAVA/jdk-21_macos-x64_bin.dmg)

Hope you will appreciate this software :-)
