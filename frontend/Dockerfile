FROM nginx:alpine

COPY dist /usr/share/nginx/html

COPY leafboss.conf /etc/nginx/conf.d/

RUN rm -f /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]