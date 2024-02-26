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
3. Create 'looper' example: plays 2 sounds in loop, controlling their volumes and receiving the instant loudness as a feedback message from pura data. Also asks for microphone permission on startup. (This version contains all the necessary features to make no matter what audio app with flutter and pure data.)