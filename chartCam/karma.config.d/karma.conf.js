config.browsers = ['ChromeHeadlessNoSandbox']; config.customLaunchers = { ChromeHeadlessNoSandbox: { base: 'ChromeHeadless', flags: ['--no-sandbox', '--disable-setuid-sandbox'] } };
