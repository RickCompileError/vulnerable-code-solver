#!/bin/bash

filename="diff.txt"
counter=0
#While loop to read line by line
while IFS= read -r line; do
    if [[ $counter -lt 2 ]] ; then
        printf "\e[36m$line\e[0m\n"
        counter=$((counter+1))
        continue
    fi
    if [[ $counter -ge 2  &&  $line == \*\*\*\** ]] ; then
        printf "\e[30m$line\e[0m\n"
        continue
    fi
    if [[ $counter -ge 2  &&  $line == \*\*\*\ * ]] ; then
        printf "\e[35m$line\e[0m\n"
        color="red"
        continue
    fi
    if [[ $counter -ge 2  &&  $line == ---\ * ]] ; then
        printf "\e[35m$line\e[0m\n"
        color="green"
        continue
    fi
    if [[ $counter -ge 2  &&  $color == "red" ]] ; then
        printf "\e[91m$line\e[0m\n"
    fi
    if [[ $counter -ge 2  &&  $color == "green" ]] ; then
        printf "\e[32m$line\e[0m\n"
    fi
done < "$filename"