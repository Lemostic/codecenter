export function createMockPlugin(options) {
    const { handlers, defaultDelay = 100 } = options;
    return {
        name: 'mdm-mock',
        configureServer(server) {
            // eslint-disable-next-line no-console
            console.log('[mdm-mock] plugin initialized, handlers count =', handlers.length);
            server.middlewares.use(async (req, res, next) => {
                if (!req.url?.startsWith('/api/')) {
                    return next();
                }
                // 剥离 query 字符串后再匹配 pattern（handler 仍可读取 req.url 完整 URL）
                const path = req.url.split('?')[0];
                // eslint-disable-next-line no-console
                console.log('[mdm-mock]', req.method, path, 'handlers:', handlers.filter(h => h.method === req.method).map(h => h.pattern.source).join(' | '));
                for (const h of handlers) {
                    if (h.method !== req.method)
                        continue;
                    if (!h.pattern.test(path))
                        continue;
                    await new Promise((r) => setTimeout(r, defaultDelay));
                    await h.handler(req, res);
                    return;
                }
                next();
            });
        },
    };
}
export function createResponse(res, data, status = 200) {
    res.statusCode = status;
    res.setHeader('Content-Type', 'application/json');
    res.end(JSON.stringify({ success: true, data }));
}
