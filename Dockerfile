FROM nginx
EXPOSE 80
RUN apt-get update -y && \
	apt-get upgrade -y && \
    apt-get install -y curl && \
    cd /tmp && \
    apt-get install awscli -y && \
    rm -rf /tmp/* && \
    rm -rf /var/lib/apt/lists/* && \
    cd /tmp \
CMD nginx -g "daemon off;"