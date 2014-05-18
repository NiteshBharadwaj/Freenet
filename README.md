Darknet App Connector
=======

This project is an android application project, written as a part of Google Summer of Code 2013.
The android application can be associated with a home freenet node (with darknet app server added i.e. the other part of the code is merged)  
Once associated with a home node, it can be used to exchange nodereferences with a peer (running the app) by various means like Wifi-Direct or Bluetooth
So, two friends using freenet can become darknet peers when they meet, say at a party or a bar, and exchange references. 
It offers good security because an attack would require specific hardware (that can break bluetooth/ Wi-Fi) to be present at that particular location and at that point of time
The peer data is synchronized with home network when the mobile gets connected to home network

To build:

Pre-Requisites:

A freenet node built with the modified MDNSReceiverPlugin and modified fred-staging
(or) latest freenet build after the modified versions are merged

Latest Bouncy Castle renamed to Spongy Castle. This can be used: https://github.com/NiteshBharadwaj/NameChanger/ to rename the classes. 
The reason being to avoid classname conflicts resulting form the pre-shipped bouncy castle on android

CaptureActivity library project - For bundling QRScanner of zxing with our app. 
https://github.com/NiteshBharadwaj/CaptureActivity/
It is simply the open source zxing application (barcode scanner) modified slightly to bundle it with our app rather than it being another standalone app

Android SDK,
jmdns >= 3.4.1,
commons-io.jar,
android-support-v4.jar.

Eclipse with ADT plugin installed

Steps:
1) Import this project into eclipse
2) Rename the bouncy castle and add it as a dependency
3) Download the zxing source from zxing website
4) Build the zxing project using ant and get a snapshot of core.jar
5) Import CaptureActivity project into eclipse and add this core.jar as dependency. 
Mark the capture activity project as library project --  Right Click on CaptureActivity project -> Properties -> Android -> isLibrary box -> Select it
6) Add other dependencies mentioned above into the freenet project i.e. jmdns, commons-io.    
(android-support-v4 is generated automatically by eclipse)
7) Reference CaptureActivity as referenced library -- Right click on Freenet project -> Properties -> Android -> Library -> Add CaptureActivity
8) Run 

It can also built using ant scripts but a little more complicated.
Contact me at ntesh93@gmail.com for any kind of queries

Nitesh Bharadwaj

