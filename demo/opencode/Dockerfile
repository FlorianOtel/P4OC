FROM golang:1.22-bookworm AS go-toolchain

RUN GOBIN=/go-tools go install golang.org/x/tools/gopls@v0.16.2

FROM ghcr.io/astral-sh/uv:0.12.4 AS uv-toolchain

FROM node:22-bookworm-slim

COPY --from=go-toolchain /usr/local/go /usr/local/go
COPY --from=go-toolchain /go-tools/gopls /usr/local/bin/gopls
COPY --from=uv-toolchain /uv /uvx /usr/local/bin/

ENV PATH="/usr/local/go/bin:${PATH}"

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install --yes --no-install-recommends \
        bash \
        build-essential \
        ca-certificates \
        clang \
        cmake \
        curl \
        dnsutils \
        fd-find \
        file \
        gdb \
        gh \
        git \
        git-lfs \
        gnupg \
        iproute2 \
        iputils-ping \
        jq \
        less \
        lldb \
        lsof \
        nano \
        netcat-openbsd \
        openjdk-17-jdk-headless \
        openssh-client \
        pkg-config \
        procps \
        python3 \
        python3-dev \
        python3-pip \
        python3-venv \
        ripgrep \
        rsync \
        shellcheck \
        shfmt \
        sqlite3 \
        strace \
        sudo \
        tmux \
        tree \
        unzip \
        vim-tiny \
        zip \
    && rm -rf /var/lib/apt/lists/* \
    && ln -s /usr/bin/fdfind /usr/local/bin/fd \
    && npm install --global \
        opencode-ai@1.18.18 \
        pnpm@11.21.0 \
        prettier@3.9.6 \
        pyright@1.1.413 \
        typescript@7.0.2 \
        typescript-language-server@5.3.0 \
    && npm cache clean --force \
    && git lfs install --system \
    && groupadd opencode-demo \
    && groupadd --gid 10001 ada \
    && useradd --uid 10001 --gid 10001 --groups opencode-demo --create-home --shell /bin/bash ada \
    && groupadd --gid 10002 linus \
    && useradd --uid 10002 --gid 10002 --groups opencode-demo --create-home --shell /bin/bash linus \
    && groupadd --gid 10003 grace \
    && useradd --uid 10003 --gid 10003 --groups opencode-demo --create-home --shell /bin/bash grace \
    && printf '%%opencode-demo ALL=(ALL:ALL) NOPASSWD: ALL\n' >/etc/sudoers.d/opencode-demo \
    && chmod 0440 /etc/sudoers.d/opencode-demo \
    && install -d -o ada -g ada /home/ada/workspaces /home/ada/.local/share/opencode \
    && install -d -o linus -g linus /home/linus/workspaces /home/linus/.local/share/opencode \
    && install -d -o grace -g grace /home/grace/workspaces /home/grace/.local/share/opencode \
    && install -d /opt/opencode/config /opt/opencode/mcp

RUN ln --symbolic --no-target-directory /usr/local/go/bin/go /usr/local/bin/go \
    && ln --symbolic --no-target-directory /usr/local/go/bin/gofmt /usr/local/bin/gofmt

COPY demo/opencode/config/ /opt/opencode/config/
COPY demo/opencode/mcp/ /opt/opencode/mcp/
COPY --chown=10001:10001 demo/opencode/seeds/ada/ /home/ada/workspaces/
COPY --chown=10002:10002 demo/opencode/seeds/linus/ /home/linus/workspaces/
COPY --chown=10003:10003 demo/opencode/seeds/grace/ /home/grace/workspaces/
COPY --chmod=0755 demo/opencode/entrypoint.sh /usr/local/bin/opencode-demo-entrypoint

RUN npm ci --omit=dev --prefix /opt/opencode/mcp \
    && npm ci --no-audit --no-fund --prefix /home/ada/workspaces/incident-api \
    && npm ci --no-audit --no-fund --prefix /home/ada/workspaces/status-dashboard \
    && npm cache clean --force \
    && chmod -R a+rX /opt/opencode/config /opt/opencode/mcp \
    && chown -R ada:ada /home/ada \
    && chown -R linus:linus /home/linus \
    && chown -R grace:grace /home/grace

ENV OPENCODE_CONFIG_DIR=/opt/opencode/config

EXPOSE 4096
USER 10001:10001
ENTRYPOINT ["/usr/local/bin/opencode-demo-entrypoint"]
