if(NOT TARGET oboe::oboe)
add_library(oboe::oboe SHARED IMPORTED)
set_target_properties(oboe::oboe PROPERTIES
    IMPORTED_LOCATION "/home/onyx/.gradle/caches/transforms-3/b421bb3b7fa235f7d8d2d3d9a4522c89/transformed/jetified-oboe-1.7.0/prefab/modules/oboe/libs/android.armeabi-v7a/liboboe.so"
    INTERFACE_INCLUDE_DIRECTORIES "/home/onyx/.gradle/caches/transforms-3/b421bb3b7fa235f7d8d2d3d9a4522c89/transformed/jetified-oboe-1.7.0/prefab/modules/oboe/include"
    INTERFACE_LINK_LIBRARIES ""
)
endif()

