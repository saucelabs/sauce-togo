TAG := $(or $(TAG),$(TAG),4.0.0-beta-1-prerelease-20201130)
VIDEO_TAG := $(or $(VIDEO_TAG),$(VIDEO_TAG),ffmpeg-4.3.1-20201130)

all: video \
	standalone_firefox \
	standalone_chrome \
	node_firefox \
	node_chrome \
	event_bus \
	session_queuer \
	sessions \
	router \
	distributor \
	hub

video:
	docker pull selenium/video:$(VIDEO_TAG) && docker tag selenium/video:$(VIDEO_TAG) saucelabs/video:$(VIDEO_TAG)

standalone_firefox:
	docker build --no-cache -t saucelabs/standalone-firefox:$(TAG) -f dockerfile_standalone_firefox .

standalone_chrome:
	docker build --no-cache  -t saucelabs/standalone-chrome:$(TAG) -f dockerfile_standalone_chrome .

node_firefox:
	docker build --no-cache  -t saucelabs/node-firefox:$(TAG) -f dockerfile_node_firefox .

node_chrome:
	docker build --no-cache  -t saucelabs/node-chrome:$(TAG) -f dockerfile_node_chrome .

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
