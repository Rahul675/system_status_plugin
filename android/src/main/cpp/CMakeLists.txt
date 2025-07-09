cmake_minimum_required(VERSION 3.4.1)

add_library(cpu_info SHARED cpu_info.cpp)

find_library(log-lib log)

target_link_libraries(cpu_info ${log-lib})