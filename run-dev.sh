#!/usr/bin/env bash

clj -A:databases:run ping --jdbci-uri "jdbc:foobar"
