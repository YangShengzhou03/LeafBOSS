FROM nginx:alpine

RUN mkdir -p /data/nginx/certs/

COPY default.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
