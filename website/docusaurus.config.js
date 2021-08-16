module.exports={
  title: 'Sauce To Go',
  tagline: 'Run tests on your infrastructure and see the results in Sauce Labs',
  url: 'https://opensource.saucelabs.com',
  baseUrl: '/sauce-togo/',
  organizationName: 'saucelabs',
  projectName: 'sauce-togo',
  scripts: [
    'https://buttons.github.io/buttons.js'
  ],
  stylesheets: [
    'https://use.typekit.net/zmt8tam.css'
  ],
  favicon: 'img/favicon.png',
  customFields: {
    disableHeaderTitle: true,
    users: [
      {
        caption: 'Sauce Labs',
        image: '/img/sauce-badge.png',
        infoLink: 'https://saucelabs.com',
        pinned: true
      }
    ],
    fonts: {
      saucelabsFont: [
        'museo-sans',
        'HelveticaNeue',
        'Helvetica Neue',
        'Serif'
      ]
    },
    repoUrl: 'https://github.com/saucelabs/sauce-togo'
  },
  onBrokenLinks: 'log',
  onBrokenMarkdownLinks: 'log',
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: 'sidebars.json',
          routeBasePath: 'docs/',
          editUrl:
              'https://github.com/saucelabs/sauce-togo/edit/main/website/',
          showLastUpdateAuthor: true,
          showLastUpdateTime: true,
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css')
        },
      }
    ]
  ],
  themes: [
    '@saucelabs/theme-github-codeblock'
  ],
  plugins: [],
  themeConfig: {
    googleAnalytics: {
      trackingID: 'UA-6735579-21',
    },
    hideableSidebar: true,
    prism: {
      additionalLanguages: ['java', 'ruby', 'csharp', 'bash', 'powershell', 'python', 'toml'],
    },
    navbar: {
      title: null,
      hideOnScroll: false,
      logo: {
        alt: 'Sauce Labs logo',
        src: '/img/logo-saucelabs.png',
      },
      items: [
        {
          label: 'Try it Free',
          position: 'right',
          href: 'https://saucelabs.com/sign-up?utm_source=referral&utm_medium=ospo&utm_campaign=saucetogo&utm_term=',
        },
        {
          label: 'Sign In',
          position: 'right',
          href: 'https://accounts.saucelabs.com/',
        },
      ]
    },
    footer: {
      logo: {
        alt: 'Sauce Logo',
        src: '/img/logo-saucelabs-inverted.png',
        href: 'https://saucelabs.com',
      },
      style: 'light',
      copyright: `Copyright © ${new Date().getFullYear()} Sauce Labs, Inc. Built with Docusaurus.`,
    },
  }
}
