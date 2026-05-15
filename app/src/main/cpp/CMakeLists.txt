cmake_minimum_required(VERSION 3.22.1)
project("soundamplifier")

set(CMAKE_CXX_STANDARD 17)

# Find Oboe via prefab (from com.google.oboe:oboe Gradle dependency)
find_package(oboe REQUIRED CONFIG)

add_library(native-lib SHARED
    native-lib.cpp
    oboe_audio_engine.cpp
    audio_processor.cpp
)

target_include_directories(native-lib PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(native-lib
    oboe::oboe
    log
)
