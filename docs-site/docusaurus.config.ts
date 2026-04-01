import {themes as prismThemes} from 'prism-react-renderer';
import type {Config} from '@docusaurus/types';
import type * as Preset from '@docusaurus/preset-classic';

const config: Config = {
  title: 'BestPartner',
  tagline: 'AI 應用大平台 — 動態建立 AI Agent，支援多種 AI 模型',
  favicon: 'img/favicon.ico',

  future: {
    v4: true,
  },

  url: 'https://github.com',
  baseUrl: '/',

  organizationName: 'a09090443',
  projectName: 'bestpartner',

  onBrokenLinks: 'throw',
  markdown: {
    hooks: {
      onBrokenMarkdownLinks: 'warn',
      onBrokenMarkdownImages: 'warn',
    },
  },

  i18n: {
    defaultLocale: 'zh-Hant',
    locales: ['zh-Hant'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: '/',
        },
        blog: false,
        theme: {
          customCss: './src/css/custom.css',
        },
      } satisfies Preset.Options,
    ],
  ],

  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'BestPartner',
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: '文件',
        },
        {
          href: 'https://github.com/a09090443/bestpartner',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: '文件',
          items: [
            { label: '快速開始', to: '/getting-started/prerequisites' },
            { label: '架構說明', to: '/architecture/overview' },
            { label: 'API 文件', to: '/api/authentication' },
          ],
        },
        {
          title: '資源',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/a09090443/bestpartner',
            },
            {
              label: 'Langchain4j 官網',
              href: 'https://docs.langchain4j.dev/',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} BestPartner. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['kotlin', 'java', 'bash', 'yaml', 'sql'],
    },
  } satisfies Preset.ThemeConfig,
};

export default config;
