#!/usr/bin/env bash


if [ "${CI:-false}" = "false" ]; then
  echo "The push_images.sh script is meant to be run only in GitHub actions!"
  echo "This prevents pushing Docker images by accident."
  echo "If you really want to push the images from your laptop, please modify the script and proceed."
  exit 1
fi

IMAGES=($(docker images --filter=reference='saucelabs/*:*' --format "{{.Repository}}:{{.Tag}}"))

for image in "${IMAGES[@]}"
do
  docker push ${image}
done


