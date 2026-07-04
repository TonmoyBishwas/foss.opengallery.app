# Third-party notices

## MobileFaceNet TFLite model (`app/src/main/assets/mobile_face_net.tflite`)

A MobileFaceNet face-embedding model (192-dimensional output, 112×112 input)
obtained from the open-source project
https://github.com/estebanuri/face_recognition (MIT License), based on the
MobileFaceNet architecture (Chen et al., 2018). Used for fully on-device
People grouping; no image data ever leaves the device.

## Google ML Kit

On-device text recognition, image labeling, and face detection are provided
by Google ML Kit via Google Play services. ML Kit is proprietary Google
software; the app degrades gracefully (search still works on filenames,
dates, folders and tags) when Play services is unavailable.
