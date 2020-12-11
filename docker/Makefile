TAG := $(or $(TAG),$(TAG),4.0.0-beta-1-prerelease-20201208)
VIDEO_TAG := $(or $(VIDEO_TAG),$(VIDEO_TAG),ffmpeg-4.3.1-20201208)
ASSETS_UPLOADER_TAG := $(or $(ASSETS_UPLOADER_TAG),$(ASSETS_UPLOADER_TAG),20201208)

all: video \
	video_latest \
	node_docker \
	standalone_docker \
	standalone_firefox \
	standalone_chrome \
	event_bus \
	session_queuer \
	sessions \
	router \
	distributor \
	hub \
	assets_uploader

video:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/video:$(VIDEO_TAG)

video_latest:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/video:latest

node_docker:
	docker build --no-cache -t saucelabs/node-docker:$(TAG) -f dockerfile_node_docker .

standalone_docker:
	docker build --no-cache -t saucelabs/standalone-docker:$(TAG) -f dockerfile_standalone_docker .

standalone_firefox:
	docker build --no-cache -t saucelabs/standalone-firefox:$(TAG) -f dockerfile_standalone_firefox .

standalone_chrome:
	docker build --no-cache  -t saucelabs/standalone-chrome:$(TAG) -f dockerfile_standalone_chrome .

event_bus:
	docker pull selenium/event-bus:$(TAG) && docker tag selenium/event-bus:$(TAG) saucelabs/event-bus:$(TAG)

session_queuer:
	docker pull selenium/session-queuer:$(TAG) && docker tag selenium/session-queuer:$(TAG) saucelabs/session-queuer:$(TAG)

sessions:
	docker pull selenium/sessions:$(TAG) && docker tag selenium/sessions:$(TAG) saucelabs/sessions:$(TAG)

router:
	docker pull selenium/router:$(TAG) && docker tag selenium/router:$(TAG) saucelabs/router:$(TAG)

distributor:
	docker pull selenium/distributor:$(TAG) && docker tag selenium/distributor:$(TAG) saucelabs/distributor:$(TAG)

hub:
	docker pull selenium/hub:$(TAG) && docker tag selenium/hub:$(TAG) saucelabs/hub:$(TAG)

assets_uploader:
	docker build --no-cache  -t saucelabs/assets-uploader:$(ASSETS_UPLOADER_TAG) -f dockerfile_assets_uploader .
