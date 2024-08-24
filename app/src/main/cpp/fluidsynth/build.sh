# export ANDROID_ABI=arm64-v8a
# export ANDROID_PLATFORM=android-28
export ANDROID_NDK=$HOME/Android/Sdk/ndk/22.1.7171670

rm -r build
mkdir build && cd build
cmake -DANDROID_ABI="arm64-v8a" -dANDROID_NDK=$ANDROID_NDK -dANDROID_PLATFORM=android-28 ..
make

read a