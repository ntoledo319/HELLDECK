cmake_minimum_required(VERSION 3.22.1)
project(helldeck_llama)

set(CMAKE_CXX_STANDARD 17)

# Add llama.cpp as subdirectory (assumes it will be placed in third_party)
# If llama.cpp is not present, this will fail - document this in README
if(EXISTS ${CMAKE_SOURCE_DIR}/../third_party/llama.cpp)
    add_subdirectory(${CMAKE_SOURCE_DIR}/../third_party/llama.cpp ${CMAKE_BINARY_DIR}/llama_build)
    
    add_library(helldeck_llama SHARED
        src/main/cpp/llama_jni.cpp
    )
    
    target_include_directories(helldeck_llama PRIVATE
        ${CMAKE_SOURCE_DIR}/../third_party/llama.cpp
    )
    
    target_link_libraries(helldeck_llama
        llama
        log
        android
    )
else()
    # Stub library when llama.cpp is not available
    add_library(helldeck_llama SHARED
        src/main/cpp/llama_jni.cpp
    )
    
    target_link_libraries(helldeck_llama
        log
        android
    )
endif()