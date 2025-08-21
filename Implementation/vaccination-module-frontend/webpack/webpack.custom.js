const webpack = require('webpack');
const { merge } = require('webpack-merge');
const environment = require('./environment');
const proxyConfig = require('./proxy.conf');
const MergeJsonWebpackPlugin = require('merge-jsons-webpack-plugin');
const { hashElement } = require('folder-hash');

const ESLintPlugin = require('eslint-webpack-plugin');
const WebpackNotifierPlugin = require('webpack-notifier');
const BrowserSyncPlugin = require('browser-sync-v3-webpack-plugin');
const path = require('path');

// TEMPORARY FIX: Suppressing DEP0060 deprecation warning caused by browser-sync's dependency
// on outdated http-proxy library that uses deprecated util._extend API.
// This warning appears when using Angular 20 with browser-sync.
// TODO: Remove this suppression once browser-sync updates its http-proxy dependency
// or when we migrate away from browser-sync to a more modern alternative.
const originalEmit = process.emit;
process.emit = function (name, data, ...args) {
  if (name === 'warning' && data && data.name === 'DeprecationWarning' && data.code === 'DEP0060') {
    return false;
  }
  return originalEmit.apply(process, arguments);
};

module.exports = async (config, options, targetOptions) => {
  const languagesHash = await hashElement(path.resolve(__dirname, '../src/assets/i18n'), {
    algo: 'md5',
    encoding: 'hex',
    files: { include: ['*.json'] },
  });
  if (config.mode === 'development') {
    config.plugins.push(
      new ESLintPlugin({
        extensions: ['js', 'ts'],
      }),
      new WebpackNotifierPlugin({
        title: 'vaccination-module-frontend',
        excludeWarnings: true,
        emoji: true,
      })
    );
  }

  const tls = Boolean(config.devServer && config.devServer.https);
  if (config.devServer) {
    config.devServer.proxy = proxyConfig({ tls });
  }
  if (targetOptions.target === 'serve' || config.watch) {
    config.plugins.push(
      new BrowserSyncPlugin(
        {
          host: 'localhost',
          port: 9000,
          https: tls,
          proxy: {
            target: `http${tls ? 's' : ''}://localhost:${targetOptions.target === 'serve' ? '4200' : '8080'}`,
            ws: true,
            proxyOptions: {
              changeOrigin: false, //pass the Host header to the backend unchanged  https://github.com/Browsersync/browser-sync/issues/430
            },
          },
          socket: {
            clients: {
              heartbeatTimeout: 60000,
            },
          },
        },
        {
          reload: targetOptions.target === 'build', // enabled for build --watch
        }
      )
    );
  }
  config.plugins.push(
    new webpack.DefinePlugin({
      I18N_HASH: JSON.stringify(languagesHash.hash),
      __VERSION__: JSON.stringify(environment.__VERSION__),
      __DEBUG_INFO_ENABLED__: environment.__DEBUG_INFO_ENABLED__ || config.mode === 'development',
      SERVER_API_URL: JSON.stringify(environment.SERVER_API_URL),
    }),
    new MergeJsonWebpackPlugin({
      output: {
        groupBy: [
          { pattern: './src/assets/i18n/de/*/*.json', fileName: './i18n/de.json' },
          { pattern: './src/assets/i18n/fr/*/*.json', fileName: './i18n/fr.json' },
          { pattern: './src/assets/i18n/it/*/*.json', fileName: './i18n/it.json' },
          { pattern: './src/assets/i18n/en/*/*.json', fileName: './i18n/en.json' },
        ],
      },
    })
  );

  config = merge(config);
  return config;
};
