ARG IMAGE
FROM ${IMAGE}

EXPOSE 8200
COPY run.sh /usr/local/bin/run.sh
RUN apk --no-cache add curl
RUN apk --no-cache add jq

CMD ["run.sh"]
