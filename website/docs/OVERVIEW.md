---
id: overview
title: Overview
sidebar_label: Overview
---
## Run tests on your infrastructure and see the results in Sauce Labs

Start a local dynamic Selenium Grid in seconds, run your tests, and visualize your test results
in the Sauce Labs dashboard.

Without any extra configuration needed, test results include screenshots, logs, and videos.

Sauce To Go leverages [docker-selenium](https://github.com/seleniumhq/docker-selenium/) to run
your tests in Firefox, (Chromium)Edge, and Chrome in a Docker container with Ubuntu. Sauce To Go
uploads all screenshots, logs and videos to Sauce Labs when tests complete. You will be able to see
tests results after logging into your Sauce Labs account.

## Why?

[docker-selenium](https://github.com/seleniumhq/docker-selenium/) is useful to get a local Selenium
Grid that records videos per test working out of the box. However, it is troublesome to manage,
store and visualize all the generated files when executing tests. Moreover, providing, analytics,
and integrations based on tests results is a complicated task that needs a expert development team
to get it done.

Sauce To Go has 3 main goals:

- Help on-premise and DIY Grids users to visualize test results in Sauce Labs by swapping their 
  own implementation with Sauce To Go
- Enable users get more value from their data through Sauce Labs Insights and Analytics
- Offer users a single view for all executed tests, either on their infrastructure or on Sauce Labs

## Help

Sauce To Go is an experiment created by the [Sauce Labs Open Source Program Office](https://opensource.saucelabs.com/).

We're here for you if you [have questions](mailto:opensource@saucelabs.com).
