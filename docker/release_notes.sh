#!/usr/bin/env bash

SELENIUM_TAG=$1

#LATEST_TAG=$(git describe --tags --abbrev=0)
HEAD_BRANCH="origin/main"

echo "" >> release_notes.md
echo "### Changelog" > release_notes.md
#git --no-pager log "${LATEST_TAG}...${HEAD_BRANCH}" --pretty=format:"* [\`%h\`](http://github.com/saucelabs/sauce-togo/commit/%H) - %s :: %an" --reverse >> release_notes.md
git --no-pager log --pretty=format:"* [\`%h\`](http://github.com/saucelabs/sauce-togo/commit/%H) - %s :: %an" --reverse >> release_notes.md

CHROME_VERSION=$(docker run --rm selenium/node-chrome:${SELENIUM_TAG} google-chrome --version | awk '{print $3}')
EDGE_VERSION=$(docker run --rm selenium/node-edge:${SELENIUM_TAG} microsoft-edge --version | awk '{print $3}')
FIREFOX_VERSION=$(docker run --rm selenium/node-firefox:${SELENIUM_TAG} firefox --version | awk '{print $3}')


echo "" >> release_notes.md
echo "### Released versions" >> release_notes.md
echo "* Chrome: ${CHROME_VERSION}" >> release_notes.md
echo "* Edge: ${EDGE_VERSION}" >> release_notes.md
echo "* Firefox: ${FIREFOX_VERSION}" >> release_notes.md

echo "" >> release_notes.md
echo "### Published Docker images" >> release_notes.md
echo '```' >> release_notes.md
docker images --filter=reference='saucelabs/*:*' --format "table {{.ID}}\t{{.Repository}}\t{{.Tag}}\t{{.Size}}" >> release_notes.md
echo '```' >> release_notes.md

