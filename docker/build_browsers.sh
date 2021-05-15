#!/usr/bin/env bash

SELENIUM_TAG=$1
BROWSER=$2

function short_version() {
    local __long_version=$1
    local __version_split=( ${__long_version//./ } )
    echo "${__version_split[0]}.${__version_split[1]}"
}

echo "Tagging images for browser ${BROWSER}..."

case "${BROWSER}" in
chrome)
  docker pull selenium/standalone-chrome:${SELENIUM_TAG}
  CHROME_VERSION=$(docker run --rm selenium/standalone-chrome:${SELENIUM_TAG} google-chrome --version | awk '{print $3}')
  echo "Chrome version -> "${CHROME_VERSION}
  CHROME_SHORT_VERSION="$(short_version ${CHROME_VERSION})"
  echo "Short Chrome version -> "${CHROME_SHORT_VERSION}

  CHROME_TAGS=(
      ${CHROME_VERSION}
      ${CHROME_SHORT_VERSION}
  )

  for chrome_tag in "${CHROME_TAGS[@]}"
  do
    docker build -t saucelabs/stg-chrome:${chrome_tag} --build-arg SELENIUM_TAG=${SELENIUM_TAG} -f dockerfile_stg_chrome .
  done
  ;;
edge)
  docker pull selenium/standalone-edge:${SELENIUM_TAG}
  EDGE_VERSION=$(docker run --rm selenium/standalone-edge:${SELENIUM_TAG} microsoft-edge --version | awk '{print $3}')
  echo "Edge version -> "${EDGE_VERSION}
  EDGE_SHORT_VERSION="$(short_version ${EDGE_VERSION})"
  echo "Short Edge version -> "${EDGE_SHORT_VERSION}

  EDGE_TAGS=(
      ${EDGE_VERSION}
      ${EDGE_SHORT_VERSION}
  )

  for edge_tag in "${EDGE_TAGS[@]}"
  do
    docker build -t saucelabs/stg-edge:${edge_tag} --build-arg SELENIUM_TAG=${SELENIUM_TAG} -f dockerfile_stg_edge .
  done
  ;;
firefox)
  docker pull selenium/standalone-firefox:${SELENIUM_TAG}
  FIREFOX_VERSION=$(docker run --rm selenium/standalone-firefox:${SELENIUM_TAG} firefox --version | awk '{print $3}')
  echo "Firefox version -> "${FIREFOX_VERSION}
  FIREFOX_SHORT_VERSION="$(short_version ${FIREFOX_VERSION})"
  echo "Short Firefox version -> "${FIREFOX_SHORT_VERSION}

  FIREFOX_TAGS=(
      ${FIREFOX_VERSION}
      ${FIREFOX_SHORT_VERSION}
  )

  for firefox_tag in "${FIREFOX_TAGS[@]}"
  do
    docker build -t saucelabs/stg-firefox:${firefox_tag} --build-arg SELENIUM_TAG=${SELENIUM_TAG} -f dockerfile_stg_firefox .
  done
  ;;
*)
  echo "Unknown browser!"
  ;;
esac
