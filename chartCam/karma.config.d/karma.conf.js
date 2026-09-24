const fs = require('fs');
const path = require('path');

config.browsers = ['ChromeHeadlessNoSandbox'];
config.customLaunchers = {
    ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-setuid-sandbox']
    }
};

function createChartCamResourceMiddleware() {
    return function (req, res, next) {
        const url = req.url.split('?')[0];
        if (url === '/sqljs.worker.js' || url.startsWith('/composeResources/')) {
            const relPath = decodeURIComponent(url.replace(/^\//, ''));
            let projectRoot = config.basePath || __dirname;
            while (projectRoot && !fs.existsSync(path.join(projectRoot, 'settings.gradle.kts'))) {
                const parent = path.dirname(projectRoot);
                if (parent === projectRoot) break;
                projectRoot = parent;
            }

            const candidates = [
                path.join(projectRoot, 'chartCam/build/processedResources/wasmJs/main', relPath),
                path.join(projectRoot, 'chartCam/build/processedResources/js/main', relPath),
                path.join(projectRoot, 'chartCam/src/wasmJsMain/resources', relPath),
                path.join(projectRoot, 'chartCam/src/jsMain/resources', relPath),
                path.join(projectRoot, 'chartCam/src/commonMain/composeResources', relPath.replace(/^composeResources\/[^\/]+\//, ''))
            ];

            for (const filePath of candidates) {
                try {
                    if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
                        const contentType = filePath.endsWith('.js')
                            ? 'application/javascript'
                            : (filePath.endsWith('.json') ? 'application/json' : 'application/octet-stream');
                        res.writeHead(200, {
                            'Content-Type': contentType,
                            'Access-Control-Allow-Origin': '*'
                        });
                        fs.createReadStream(filePath).pipe(res);
                        return;
                    }
                } catch (e) {}
            }
        }
        return next();
    };
}

config.beforeMiddleware = config.beforeMiddleware || [];
config.beforeMiddleware.push('chartcamResources');

config.plugins = config.plugins || [];
config.plugins.push({
    'middleware:chartcamResources': ['factory', createChartCamResourceMiddleware]
});
