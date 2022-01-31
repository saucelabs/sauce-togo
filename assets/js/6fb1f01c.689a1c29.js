"use strict";(self.webpackChunk=self.webpackChunk||[]).push([[499],{2785:(e,t,a)=>{a.r(t),a.d(t,{frontMatter:()=>r,metadata:()=>d,toc:()=>l,default:()=>c});var n=a(7462),o=a(3366),i=(a(7294),a(3905)),s=["components"],r={id:"getting-started",title:"Getting Started",sidebar_label:"Getting Started"},d={unversionedId:"getting-started",id:"getting-started",isDocsHomePage:!1,title:"Getting Started",description:"Prerequisites",source:"@site/docs/GETTING_STARTED.md",sourceDirName:".",slug:"/getting-started",permalink:"/sauce-togo/getting-started",editUrl:"https://github.com/saucelabs/sauce-togo/edit/main/website/docs/GETTING_STARTED.md",version:"current",lastUpdatedBy:"Diego Molina",lastUpdatedAt:1643621561,formattedLastUpdatedAt:"1/31/2022",sidebar_label:"Getting Started",frontMatter:{id:"getting-started",title:"Getting Started",sidebar_label:"Getting Started"},sidebar:"docs",previous:{title:"Overview",permalink:"/sauce-togo/"},next:{title:"Become a Contributor",permalink:"/sauce-togo/contributing"}},l=[{value:"Prerequisites",id:"prerequisites",children:[]},{value:"Get Sauce To Go up and running",id:"get-sauce-to-go-up-and-running",children:[{value:"Two-minute demo",id:"two-minute-demo",children:[]},{value:"1. Create a directory and copy the configuration example",id:"1-create-a-directory-and-copy-the-configuration-example",children:[]},{value:"2. Start Sauce To Go",id:"2-start-sauce-to-go",children:[]},{value:"3. Run your tests",id:"3-run-your-tests",children:[]},{value:"4. Head to Sauce Labs and check your tests!",id:"4-head-to-sauce-labs-and-check-your-tests",children:[]}]}],m={toc:l};function c(e){var t=e.components,a=(0,o.default)(e,s);return(0,i.mdx)("wrapper",(0,n.default)({},m,a,{components:t,mdxType:"MDXLayout"}),(0,i.mdx)("h2",{id:"prerequisites"},"Prerequisites"),(0,i.mdx)("p",null,"Please make sure you have the following:"),(0,i.mdx)("ul",null,(0,i.mdx)("li",{parentName:"ul"},(0,i.mdx)("a",{parentName:"li",href:"https://docs.docker.com/engine/install/"},"Docker installed")),(0,i.mdx)("li",{parentName:"ul"},(0,i.mdx)("inlineCode",{parentName:"li"},"docker run hello-world")," runs without any errors"),(0,i.mdx)("li",{parentName:"ul"},"An active Sauce Labs account, if you don't have one yet, please ",(0,i.mdx)("a",{parentName:"li",href:"https://saucelabs.com/sign-up?utm_source=referral&utm_medium=ospo&utm_campaign=saucetogo&utm_term="},"sign-up")),(0,i.mdx)("li",{parentName:"ul"},"Recommended - Your Sauce Labs ",(0,i.mdx)("inlineCode",{parentName:"li"},"SAUCE_USERNAME")," and ",(0,i.mdx)("inlineCode",{parentName:"li"},"SAUCE_ACCESS_KEY")," as environment variables")),(0,i.mdx)("pre",null,(0,i.mdx)("code",{parentName:"pre",className:"language-bash"},"SAUCE_USERNAME='valid.username'\nSAUCE_ACCESS_KEY='valid.key'\n")),(0,i.mdx)("p",null,"Here are instructions for setting environment variables on each Operating System:"),(0,i.mdx)("ul",null,(0,i.mdx)("li",{parentName:"ul"},(0,i.mdx)("a",{parentName:"li",href:"https://www.architectryan.com/2018/08/31/how-to-change-environment-variables-on-windows-10/"},"Windows 10")),(0,i.mdx)("li",{parentName:"ul"},(0,i.mdx)("a",{parentName:"li",href:"https://apple.stackexchange.com/questions/106778/how-do-i-set-environment-variables-on-os-x"},"MacOS")),(0,i.mdx)("li",{parentName:"ul"},(0,i.mdx)("a",{parentName:"li",href:"https://askubuntu.com/questions/58814/how-do-i-add-environment-variables"},"Linux"))),(0,i.mdx)("h2",{id:"get-sauce-to-go-up-and-running"},"Get Sauce To Go up and running"),(0,i.mdx)("h3",{id:"two-minute-demo"},"Two-minute demo"),(0,i.mdx)("video",{style:{maxWidth:"100%"},controls:!0},(0,i.mdx)("source",{src:"https://user-images.githubusercontent.com/5992658/135048022-58e73843-69d7-4f04-8e9e-ae8f6a83c89d.mp4",type:"video/mp4"}),"Your browser does not support the video tag."),(0,i.mdx)("h3",{id:"1-create-a-directory-and-copy-the-configuration-example"},"1. Create a directory and copy the configuration example"),(0,i.mdx)("p",null,"Save the file as ",(0,i.mdx)("inlineCode",{parentName:"p"},"config.toml")),(0,i.mdx)("p",null,"Check the comments in the configuration example for specific adjustments on each operating system."),(0,i.mdx)("pre",null,(0,i.mdx)("code",{parentName:"pre",className:"language-toml"},'[docker]\n# Configs have a mapping between a Docker image and the capabilities that need to be matched to\n# start a container with the given image.\nconfigs = [\n    "saucelabs/stg-firefox:96.0", \'{"browserName": "firefox", "browserVersion": "95.0", "platformName": "linux"}\',\n    "saucelabs/stg-edge:97.0", \'{"browserName": "MicrosoftEdge", "browserVersion": "96.0", "platformName": "linux"}\',\n    "saucelabs/stg-chrome:97.0", \'{"browserName": "chrome", "browserVersion": "96.0", "platformName": "linux"}\'\n]\n\n# URL for connecting to the docker daemon\n# Linux: 172.17.0.1 (make sure the Docker deamon is listening to this url first)\n# Docker Desktop on macOS and Windows: host.docker.internal\n# To have Docker listening through tcp on macOS, install socat and run the following command\n# socat -4 TCP-LISTEN:2375,fork UNIX-CONNECT:/var/run/docker.sock\nurl = "http://host.docker.internal:2375"\n# Docker image used for video recording\nvideo-image = "saucelabs/stg-video:20220131"\n# Docker image used to upload test assets\nassets-uploader-image = "saucelabs/stg-assets-uploader:20220131"\n\n[node]\nimplementation = "com.saucelabs.grid.SauceNodeFactory"\n')),(0,i.mdx)("p",null,(0,i.mdx)("em",{parentName:"p"},"Make sure the directory path can be accessed by Docker.")),(0,i.mdx)("p",null,(0,i.mdx)("strong",{parentName:"p"},"Tip:")," To improve loading time, pull the Docker images before moving to step two\n(only needed once)."),(0,i.mdx)("pre",null,(0,i.mdx)("code",{parentName:"pre",className:"language-bash"},"docker pull saucelabs/stg-firefox:96.0\ndocker pull saucelabs/stg-edge:97.0\ndocker pull saucelabs/stg-chrome:97.0\ndocker pull saucelabs/stg-video:20220131\ndocker pull saucelabs/stg-assets-uploader:20220131\n")),(0,i.mdx)("h3",{id:"2-start-sauce-to-go"},"2. Start Sauce To Go"),(0,i.mdx)("p",null,"You'll need to mount two volumes. The first one is the path where the config file from\nstep 1 is, and the second one is the path where the test assets will be temporarily stored."),(0,i.mdx)("p",null,(0,i.mdx)("em",{parentName:"p"},"Be sure to be in the same directory you created on step 1.")),(0,i.mdx)("pre",null,(0,i.mdx)("code",{parentName:"pre",className:"language-bash"},"docker run --rm --name sauce-togo -p 4444:4444 \\\n    -v ${PWD}/config.toml:/opt/bin/config.toml \\\n    -v ${PWD}/assets/directory:/opt/selenium/assets \\\n    saucelabs/stg-standalone:20220131\n")),(0,i.mdx)("h3",{id:"3-run-your-tests"},"3. Run your tests"),(0,i.mdx)("p",null,"Point them to either ",(0,i.mdx)("inlineCode",{parentName:"p"},"http://localhost:4444")," or to ",(0,i.mdx)("inlineCode",{parentName:"p"},"http://localhost:4444/wd/hub"),"."),(0,i.mdx)("p",null,"Your test capabilities need to include the ",(0,i.mdx)("inlineCode",{parentName:"p"},"sauce:options")," section, check the example below."),(0,i.mdx)("details",null,(0,i.mdx)("summary",null,"Click to see the test example"),(0,i.mdx)("pre",null,(0,i.mdx)("code",{parentName:"pre",className:"language-java"},'import org.junit.jupiter.api.Test;\nimport org.openqa.selenium.By;\nimport org.openqa.selenium.MutableCapabilities;\nimport org.openqa.selenium.firefox.FirefoxOptions;\nimport org.openqa.selenium.remote.RemoteWebDriver;\n\nimport java.net.MalformedURLException;\nimport java.net.URL;\n\nimport static org.junit.jupiter.api.Assertions.assertEquals;\n\npublic class DemoTest {\n  @Test\n  public void demoTest() throws MalformedURLException {\n    MutableCapabilities sauceOptions = new MutableCapabilities();\n    // Depending where your Sauce Labs account is created, use \'EU\' or \'US\'\n    sauceOptions.setCapability("dataCenter", "US");\n    sauceOptions.setCapability("timeZone", "US/Pacific");\n    sauceOptions.setCapability("screenResolution", "1920x1080");\n    sauceOptions.setCapability("username", System.getenv("SAUCE_USERNAME"));\n    sauceOptions.setCapability("accessKey", System.getenv("SAUCE_ACCESS_KEY"));\n    sauceOptions.setCapability("name", "demoTest");\n\n    URL gridUrl = new URL("http://localhost:4444");\n    FirefoxOptions firefoxOptions = new FirefoxOptions();\n    firefoxOptions.setCapability("platformName", "linux");\n    firefoxOptions.setCapability("browserVersion", "96.0");\n    firefoxOptions.setCapability("sauce:options", sauceOptions);\n    RemoteWebDriver driver = new RemoteWebDriver(gridUrl, firefoxOptions);\n    driver.manage().window().maximize();\n\n    try {\n      // Log in to www.saucedemo.com\n      driver.get("https://www.saucedemo.com");\n      driver.findElement(By.id("user-name")).sendKeys("standard_user");\n      driver.findElement(By.id("password")).sendKeys("secret_sauce");\n      driver.findElement(By.className("btn_action")).click();\n\n      // Add two items to the shopping cart\n      driver.get("https://www.saucedemo.com/inventory.html");\n      driver.findElement(By.className("btn_primary")).click();\n      driver.findElement(By.className("btn_primary")).click();\n      assertEquals("2", driver.findElement(By.className("shopping_cart_badge")).getText());\n\n      // Assert we have two items in the shopping cart\n      driver.get("https://www.saucedemo.com/cart.html");\n      assertEquals(2, driver.findElements(By.className("inventory_item_name")).size());\n    } finally {\n      driver.quit();\n    }\n  }\n}\n'))),(0,i.mdx)("h3",{id:"4-head-to-sauce-labs-and-check-your-tests"},"4. Head to ",(0,i.mdx)("a",{parentName:"h3",href:"https://app.saucelabs.com/"},"Sauce Labs")," and check your tests!"))}c.isMDXComponent=!0}}]);