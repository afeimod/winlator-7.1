cmake_minimum_required(VERSION 3.22.1)

Project(Winlator)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

#add_subdirectory(proot)
add_subdirectory(virglrenderer)
add_subdirectory(OpenXR-SDK)
#add_subdirectory(fluidsynth fluidsynth/build)

add_library(winlator SHARED
            xr/engine.c
            xr/framebuffer.c
            xr/input.c
            xr/main.c
            xr/math.c
            xr/renderer.c
            winlator/drawable.c
            winlator/gpu_image.c
            winlator/sysvshared_memory.c
            winlator/xconnector_epoll.c
            winlator/alsa_client.c)

target_link_libraries(winlator
                      log
                      android
                      jnigraphics
                      openxr_loader
                      aaudio
                      EGL
                      GLESv2
                      GLESv3
)


#include_directories(fluidsynth/include)
#add_subdirectory(fluidsynth fluidsynth/build)
#add_executable(midi_server fluidsynth/midi_server.cpp)
#target_link_libraries(midi_server fluidsynth log)

set(CWD ${CMAKE_CURRENT_SOURCE_DIR}/fluidsynth)
set(DEP_LIBS
        libFLAC
        libfluidsynth
        libfluidsynth-assetloader
        libgio-2.0
        libglib-2.0
        libgmodule-2.0
        libgobject-2.0
        libgthread-2.0
        libinstpatch-1.0
        liboboe
        libogg
        libopus
        libpcre
        libpcreposix
        libsndfile-1.0 # 这个winlator原本就有，重命名加个-1.0区分一下吧
        libvorbis
        libvorbisenc
        libvorbisfile)

# 定义各种依赖库名称
foreach (it_lib IN LISTS DEP_LIBS)
    add_library(${it_lib} SHARED IMPORTED)
    set_target_properties(${it_lib} PROPERTIES IMPORTED_LOCATION ${CWD}/lib/${ANDROID_ABI}/${it_lib}.so)
endforeach (it_lib)

find_package(OpenMP REQUIRED)

# 定义一个fluidsynth，依赖于这些库
add_library(fluidsynth INTERFACE)

foreach (it_lib IN LISTS DEP_LIBS)
    target_link_libraries(fluidsynth INTERFACE ${it_lib})
endforeach (it_lib)
target_link_libraries(fluidsynth INTERFACE OpenMP::OpenMP_CXX)

## 自己的native代码，依赖于fluidsynth即可。
#add_library(midi_server SHARED midi_server.cpp)
#target_include_directories(midi_server PRIVATE ${CWD}/include)
#target_link_libraries(midi_server PRIVATE fluidsynth log)

add_executable(midi_server_exe ${CWD}/midi_server.cpp)
target_include_directories(midi_server_exe PRIVATE ${CWD}/include)
target_link_libraries(midi_server_exe PRIVATE fluidsynth)