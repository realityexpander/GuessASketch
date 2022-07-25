# Guess-A-Sketch
Multi-player real-time game for Android using the Ktor-guessasketch-server backend

Backend repo: https://github.com/realityexpander/ktor-guessasketch-server

[<img src="https://user-images.githubusercontent.com/5157474/180709312-f6cd2742-6eac-4f64-9e17-af682c82f428.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180709312-f6cd2742-6eac-4f64-9e17-af682c82f428.png)

Game features:
- Players compete against each other in real-time to guess a word based on a one player's sketch
- Unlimited number of rooms
- As many as 8 players at a time per room
- Score board
- Real-time chat
- Speech-to-text functionality to guess word or chat

# Technologies employed:

* Kotlin 
* MVVM
* Custom Views for sketching
* Coroutines
* Custom REST API built with Ktor
* Responds to custom HTTP & HTTPS endpoints with JSON
* Websockets for real-time communication
* Allows users to reconnect if accidentally disconnected
* Retrofit to communicate with Ktor server for basic game utilities
* Scarlet library for real-time websocket communications
* Efficiently handles network errors
* Dependency injection with Dagger-Hilt
* Encrypted HTTPS traffic
* Deployable Ktor server accessable from anywhere
* Deployable to Heroku or Ubuntu servers

Note : this repository only contains the android app part, Ktor API is in this repo: https://github.com/realityexpander/ktor-guessasketch-server

To install the Apk:

1. Open this link on your Android device:
   https://github.com/realityexpander/GuessASketch/blob/master/guessasketch_1.0.apk
2. Tap the "skewer" menu and tap the "download"

   [![](https://user-images.githubusercontent.com/5157474/147434050-57102a30-af32-46ed-a90b-d94e0c4a4f35.jpg)]()
3. Allow the file to download (DO NOT click "show details")
4. After the file is downloaded, click "OK" to install
5. Click "OK" to install
6. Click "OK" to launch

If you have developer options turned on, you may need to turn off "USB Debugging" if the "Waiting for debugger" dialog is displayed.
