function setupProxy({ tls }) {
  const conf = [
    {
      context: ['/'],
      target: `http${tls ? 's' : ''}://localhost:8080`,
      secure: false,
      changeOrigin: tls,
    },
  ];
}

module.exports = setupProxy;
