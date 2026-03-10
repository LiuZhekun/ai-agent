#!/usr/bin/env bash
set -e
docker build -t ai-agent-demo:latest -f Dockerfile ../..
docker compose up -d
