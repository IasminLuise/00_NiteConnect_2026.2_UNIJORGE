FROM debian:bookworm-slim@sha256:a4672c0cb26fbdde88e38fa2dfb6c681942306680e41e4378b28770b6e79ee91
RUN apt-get update && apt-get install -y --no-install-recommends \
    libreoffice-writer=4:7.4.7-1+deb12u14 libreoffice-math=4:7.4.7-1+deb12u14 \
    fonts-dejavu-core=2.37-6 fonts-liberation2=2.1.5-1 fontconfig \
    && dpkg-query -W > /package-versions.txt \
    && rm -rf /var/lib/apt/lists/*
ENV HOME=/tmp LANG=C.UTF-8
USER 10001:10001
ENTRYPOINT ["libreoffice", "-env:UserInstallation=file:///tmp/lo-profile", "--headless", "--convert-to", "pdf:writer_pdf_Export", "--outdir", "/output"]
