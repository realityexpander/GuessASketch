# Guess-A-Sketch
Multi-player real-time game for Android using the Ktor-guessasketch-server backend

Backend repo: https://github.com/realityexpander/ktor-guessasketch-server

[<img src="https://user-images.githubusercontent.com/5157474/180716112-91dd7a11-98e3-452d-a858-a3c4b11970f4.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716112-91dd7a11-98e3-452d-a858-a3c4b11970f4.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716332-061e5fc0-7dab-47fd-b7e0-fb9894daaffc.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716332-061e5fc0-7dab-47fd-b7e0-fb9894daaffc.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716243-eb1366d4-144a-4d05-85eb-4408ea3da380.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716243-eb1366d4-144a-4d05-85eb-4408ea3da380.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716466-15572b64-7caf-4ca5-a6d7-86b07bd22f84.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716466-15572b64-7caf-4ca5-a6d7-86b07bd22f84.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716599-deffb0db-1f08-44c6-8220-84c851649d13.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716599-deffb0db-1f08-44c6-8220-84c851649d13.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716734-d4100285-9132-4f14-8a85-754305ab8819.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716734-d4100285-9132-4f14-8a85-754305ab8819.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716817-fe0ec67d-f8d9-4cb7-80f3-32fb42430a3b.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716817-fe0ec67d-f8d9-4cb7-80f3-32fb42430a3b.png)
[<img src="https://user-images.githubusercontent.com/5157474/180716909-ddb47210-695d-4d8a-b316-68742e8e583d.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180716909-ddb47210-695d-4d8a-b316-68742e8e583d.png)
[<img src="https://user-images.githubusercontent.com/5157474/180717058-acfa8dc8-7683-440b-9b54-366b12d164f9.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180717058-acfa8dc8-7683-440b-9b54-366b12d164f9.png)
[<img src="https://user-images.githubusercontent.com/5157474/180717159-f8081d4a-2507-4460-83fa-d38797f84af3.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180717159-f8081d4a-2507-4460-83fa-d38797f84af3.png)
[<img src="https://user-images.githubusercontent.com/5157474/180717307-f9d8f72e-a289-4196-b4bc-520011a13f96.png" width="200"/>](https://user-images.githubusercontent.com/5157474/180717307-f9d8f72e-a289-4196-b4bc-520011a13f96.png)

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
