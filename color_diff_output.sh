#!/bin/bash

filename="diff.txt"
counter=0
#While loop to read line by line
while IFS= read -r line; do
    if [[ $counter -lt 2 ]] ; then
        echo "\033[36m$line\033[0m\n"
        counter=$((counter+1))
        continue
    fi
    if [[ $counter -ge 2  &&  $line == \*\*\*\** ]] ; then
        echo "\033[30m$line\033[0m\n"
        continue
    fi
    if [[ $counter -ge 2  &&  $line == \*\*\*\ * ]] ; then
        echo "\033[35m$line\033[0m\n"
        color="red"
        continue
    fi
    if [[ $counter -ge 2  &&  $line == ---\ * ]] ; then
        echo "\033[35m$line\033[0m\n"
        color="green"
        continue
    fi
    if [[ $counter -ge 2  &&  $color == "red" ]] ; then
        if [[ $line == !\ * ]] ; then
            echo "\033[106m$line\033[0m\n"
        elif [[ $line == -\ * ]] ; then
            echo "\033[101m$line\033[0m\n"
        else
            echo "\033[91m$line\033[0m\n"
        fi
    fi
    if [[ $counter -ge 2  &&  $color == "green" ]] ; then
        if [[ $line == !\ * ]] ; then
            echo "\033[106m$line\033[0m\n"
        elif [[ $line == +\ * ]] ; then
            echo "\033[102m$line\033[0m\n"
        else
            echo "\033[32m$line\033[0m\n"
        fi
    fi

done < "$filename"
