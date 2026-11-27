# La imagen copia la configuracion en lugar de montarla como volumen.
# En Docker Desktop, un .conf bind-mount falla al iniciar el contenedor.
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
