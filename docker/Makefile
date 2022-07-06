SELENIUM_TAG := $(or $(SELENIUM_TAG),$(SELENIUM_TAG), 4.3.0-20220706)
CURRENT_DATE := $(shell date '+%Y%m%d')
SAUCE_TAG := $(or $(SAUCE_TAG),$(SAUCE_TAG),$(CURRENT_DATE))
VIDEO_TAG := $(or $(VIDEO_TAG),$(VIDEO_TAG),ffmpeg-4.3.1-20220706)

all: stg_video \
	stg_video_latest \
	stg_node \
	stg_standalone \
	stg_firefox \
	stg_chrome \
	stg_edge \
	stg_hub \
	stg_assets_uploader \
	stg_assets_uploader_latest

stg_video:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/stg-video:$(SAUCE_TAG)

stg_video_latest:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/stg-video:latest

stg_node:
	docker build --no-cache -t saucelabs/stg-node:$(SAUCE_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_stg_node .

stg_standalone:
	docker build --no-cache -t saucelabs/stg-standalone:$(SAUCE_TAG) --build-arg SELENIUM_TAG=$(SELENIUM_TAG) -f dockerfile_stg_standalone .

stg_firefox:
	./build_browsers.sh $(SELENIUM_TAG) firefox

stg_chrome:
	./build_browsers.sh $(SELENIUM_TAG) chrome

stg_edge:
	./build_browsers.sh $(SELENIUM_TAG) edge

stg_hub:
	docker pull selenium/hub:$(SELENIUM_TAG) && docker tag selenium/hub:$(SELENIUM_TAG) saucelabs/stg-hub:$(SAUCE_TAG)

stg_assets_uploader:
	docker build --no-cache -t saucelabs/stg-assets-uploader:$(SAUCE_TAG) -f dockerfile_stg_assets_uploader .

stg_assets_uploader_latest:
	docker build --no-cache -t saucelabs/stg-assets-uploader:latest -f dockerfile_stg_assets_uploader .

push_images:
	./push_images.sh

release_notes:
	./release_notes.sh $(SELENIUM_TAG)
