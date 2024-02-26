# pd-with-flutter
This repo contains examples on how to use pure data as audio engine in flutter.

## How to use this repo

This repo contains branches with different examples. Every branch departures
from scratch (empty folder), creates an empty flutter project and adds the
minimum code to make it work. You should follow the commit history to see the
changes in the code.

## Steps until this version

0. Empty folder
1. Create an empty flutter project with `flutter create --org com.domain_name app_name`
2. Resolve dependencies with `flutter pub get`
3. Minimal code to send a "on/off" message to a pure data patch, that plays a 440Hz sine wave.
4. Ask for microphone permission at the app start, which becomes available inside the pure data patch as the "adc~" object. (not used yet)