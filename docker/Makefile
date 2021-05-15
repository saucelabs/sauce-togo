SELENIUM_TAG := $(or $(SELENIUM_TAG),$(SELENIUM_TAG),4.0.0-beta-4-prerelease-20210513)
VIDEO_TAG := $(or $(VIDEO_TAG),$(VIDEO_TAG),ffmpeg-4.3.1-20210513)
ASSETS_UPLOADER_TAG := $(or $(ASSETS_UPLOADER_TAG),$(ASSETS_UPLOADER_TAG),20210513)

all: video \
	node_docker \
	standalone_docker \
	standalone_firefox \
	standalone_chrome \
	standalone_edge \
	hub \
	assets_uploader

video:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/video:$(VIDEO_TAG)

node_docker:
	docker build --no-cache -t saucelabs/node-docker:$(SELENIUM_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_node_docker .

standalone_docker:
	docker build --no-cache -t saucelabs/standalone-docker:$(SELENIUM_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_standalone_docker .

standalone_firefox:
	docker build --no-cache -t saucelabs/standalone-firefox:$(SELENIUM_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_standalone_firefox .

standalone_chrome:
	docker build --no-cache -t saucelabs/standalone-chrome:$(SELENIUM_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_standalone_chrome .

standalone_edge:
	docker build --no-cache -t saucelabs/standalone-edge:$(SELENIUM_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_standalone_edge .

hub:
	docker pull selenium/hub:$(SELENIUM_TAG) && docker tag selenium/hub:$(SELENIUM_TAG) saucelabs/hub:$(SELENIUM_TAG)

assets_uploader:
	docker build --no-cache  -t saucelabs/assets-uploader:$(ASSETS_UPLOADER_TAG) -f dockerfile_assets_uploader .
